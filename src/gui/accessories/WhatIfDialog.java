package gui.accessories;

import business.combat.ArmySim;
import business.combat.CombatScenario;
import business.combat.WhatIfSearch;
import control.BattleSimControler;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import model.Pelotao;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 * "What do I need to win this combat, or to capture this city?" T-842.
 *
 * The forward simulator answers "given these forces, what happens". This asks it backwards, by
 * varying one platoon the player nominates and reporting the smallest quantity that achieves what
 * he named. The search itself is {@link WhatIfSearch}; this is the question, the answer as a
 * sentence, and the two honesty flags the answer can carry.
 *
 * <h3>Two goals, and one of them is the one with a crisp answer</h3>
 *
 * <b>Take the city</b> is a single comparison of summed attack against the wall value, so a
 * quantity threshold genuinely exists and "1,850 takes Riverrun" is a real sentence. It is the
 * default whenever the battle has a city in it.
 *
 * <b>Hold the field</b> often has no answer at all, and that is a property of the game rather than
 * a limitation here. Measured on the curve: against an enemy with a per-soldier edge, adding men
 * LENGTHENS the battle - 2 rounds at 100, 6 at 1,000, 46 at 10,000 - until it reaches the round cap
 * and stops undecided. So numbers do not beat quality, and the honest answer is "no quantity works,
 * and here is why": a player told that changes his tactic, his training or his commander instead of
 * recruiting into a stalemate. The wording distinguishes it from losing.
 *
 * <h3>The worst case is the other end of a bracket</h3>
 *
 * John, 2026-09-27. An unidentified enemy fights at 1, so any threshold computed against it is a
 * FLOOR. Ticking the box retypes those troops to the strongest their nation could field on this
 * ground and asks again, which gives the ceiling. Running it both ways brackets the real answer,
 * and the two sentences say which is which.
 *
 * <h3>Run on the event thread, deliberately</h3>
 *
 * A search is about twenty runs of a chain that resolves a whole battle in well under a
 * millisecond - the fourteen tests around it, most of which run several searches, finish in 0.3
 * seconds together. Measured rather than assumed, and at that size a worker thread would add a
 * class of bug (a result arriving after the dialog closed) to buy nothing. The wait cursor is there
 * for the pathological ceiling.
 */
public class WhatIfDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    /** Enough room for the longest answer plus its caveat, without a scrollbar in the normal case. */
    private static final int ANSWER_ROWS = 7;
    /** A ceiling nobody would type, and the spinner's own upper rail. */
    private static final int MAX_CEILING = 9999999;

    private final transient BattleSimControler controler;
    private final transient ArmySim army;
    private final transient Pelotao platoon;
    private final JRadioButton takeCity = new JRadioButton(
            labels.getString("BATTLESIM.WHATIF.GOAL.CITY"));
    private final JRadioButton holdField = new JRadioButton(
            labels.getString("BATTLESIM.WHATIF.GOAL.FIELD"));
    private final JCheckBox worstCase = new JCheckBox(
            labels.getString("BATTLESIM.WHATIF.WORSTCASE"));
    private final JSpinner ceiling;
    private final JTextArea answer = new JTextArea(ANSWER_ROWS, 44);
    private final JButton open = new JButton(labels.getString("BATTLESIM.WHATIF.OPEN"));
    /** The last search's threshold, which is what {@link #doOpen} builds a battle at. */
    private transient int found = -1;

    public WhatIfDialog(BattleSimWindow owner, BattleSimControler controler, ArmySim army,
            Pelotao platoon) {
        super(owner, labels.getString("BATTLESIM.WHATIF.TITLE"), true);
        this.controler = controler;
        this.army = army;
        this.platoon = platoon;
        // Ten times what is there now, floored so a tiny platoon still gets a useful range. The
        // player can raise it, and "not within this ceiling" says what it was - the two together
        // are why a default that is merely reasonable is enough.
        this.ceiling = new JSpinner(new SpinnerNumberModel(
                Math.max(platoon.getQtd() * 10, 1000), 1, MAX_CEILING, 100));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildQuestion(), BorderLayout.NORTH);
        add(buildAnswer(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildQuestion() {
        final JPanel ret = new JPanel(new GridLayout(0, 1, 0, 4));
        ret.add(new JLabel(String.format(labels.getString("BATTLESIM.WHATIF.SUBJECT"),
                platoon.getTipoTropa() == null ? "" : platoon.getTipoTropa().getNome(),
                controler.getScenario().getDisplayName(army))));
        final ButtonGroup goals = new ButtonGroup();
        goals.add(takeCity);
        goals.add(holdField);
        // A city that is not in this battle cannot be taken in it, so the option is not offered -
        // an enabled radio that can only ever answer "no assault" is a question with a fake answer.
        takeCity.setEnabled(controler.getScenario().isCityParticipates());
        takeCity.setSelected(takeCity.isEnabled());
        holdField.setSelected(!takeCity.isEnabled());
        ret.add(takeCity);
        ret.add(holdField);
        // Only offered when there IS something unidentified: otherwise the worst case and the
        // ordinary case are the same battle, and a tickbox that changes nothing invites the player
        // to believe it did.
        worstCase.setEnabled(WhatIfSearch.hasUnidentifiedTroops(controler.getScenario()));
        worstCase.setToolTipText(labels.getString(worstCase.isEnabled()
                ? "BATTLESIM.WHATIF.WORSTCASE.HINT" : "BATTLESIM.WHATIF.WORSTCASE.NONE"));
        ret.add(worstCase);
        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        row.add(new JLabel(labels.getString("BATTLESIM.WHATIF.CEILING")));
        row.add(ceiling);
        ret.add(row);
        return ret;
    }

    private JScrollPane buildAnswer() {
        answer.setEditable(false);
        answer.setLineWrap(true);
        answer.setWrapStyleWord(true);
        answer.setOpaque(false);
        answer.setText(labels.getString("BATTLESIM.WHATIF.PROMPT"));
        final JScrollPane ret = new JScrollPane(answer);
        ret.setBorder(BorderFactory.createTitledBorder(
                labels.getString("BATTLESIM.WHATIF.ANSWER")));
        ret.setPreferredSize(new Dimension(520, 150));
        return ret;
    }

    private JPanel buildButtons() {
        final JPanel ret = new JPanel(new FlowLayout(FlowLayout.TRAILING, 6, 0));
        final JButton run = new JButton(labels.getString("BATTLESIM.WHATIF.RUN"));
        run.setActionCommand("run");
        run.addActionListener(this);
        open.setActionCommand("open");
        open.addActionListener(this);
        open.setEnabled(false);
        open.setToolTipText(labels.getString("BATTLESIM.WHATIF.OPEN.HINT"));
        final JButton close = new JButton(labels.getString("BATTLESIM.DIPLOMACY.CLOSE"));
        close.setActionCommand("close");
        close.addActionListener(this);
        ret.add(open);
        ret.add(run);
        ret.add(close);
        getRootPane().setDefaultButton(run);
        return ret;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if ("run".equals(event.getActionCommand())) {
            doRun();
        } else if ("open".equals(event.getActionCommand())) {
            doOpen();
        } else {
            dispose();
        }
    }

    /** The scenario the question is really about: this battle, or its worst-case twin. */
    private CombatScenario subject() {
        final CombatScenario ret = controler.getScenario().copy();
        if (worstCase.isSelected() && worstCase.isEnabled()) {
            WhatIfSearch.toWorstCase(ret, controler.getScenario().getPartida() == null
                    ? null : controler.getScenario().getPartida().getCenario());
        }
        return ret;
    }

    private void doRun() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            final CombatScenario subject = subject();
            final WhatIfSearch.Answer result = WhatIfSearch.forPlatoonQuantity(subject,
                    subject.getPartida() == null ? null : subject.getPartida().getCenario(),
                    controler.getScenario().getArmies().indexOf(army), platoon.getCodigo(),
                    takeCity.isSelected()
                            ? WhatIfSearch.Goal.TAKE_THE_CITY
                            : WhatIfSearch.Goal.HOLD_THE_FIELD,
                    (Integer) ceiling.getValue());
            found = result.isFound() ? result.getThreshold() : -1;
            open.setEnabled(result.isFound());
            answer.setText(sentence(result));
            answer.setCaretPosition(0);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * The answer in words, including every reason it might be worth less than it looks.
     *
     * Each caveat is a separate sentence rather than a footnote, because they change what the
     * number MEANS: a floor is not an estimate, an unverified threshold is not the smallest value,
     * and a stalemate is not a defeat. A player who reads only the first line should still not be
     * misled by it, which is why the qualifier is inside that line and not below it.
     */
    private String sentence(WhatIfSearch.Answer result) {
        final StringBuilder ret = new StringBuilder();
        if (result.isWinsAtNothing()) {
            ret.append(labels.getString("BATTLESIM.WHATIF.ALREADY"));
        } else if (!result.isFound()) {
            ret.append(String.format(labels.getString(result.isCeilingStalemate()
                    ? "BATTLESIM.WHATIF.STALEMATE" : "BATTLESIM.WHATIF.NONE"),
                    result.getCeiling()));
        } else {
            // NO TROOP NAME HERE. Crash 327108: the troop was being passed as the second argument
            // to labels whose second specifier is %,d, and String.format throws
            // IllegalFormatConversionException rather than coercing - on the SUCCESS path, so the
            // headline feature crashed whenever it found an answer. The troop is already named in
            // the line above the answer box ("Varying the X of Y"), so nothing is lost by it.
            ret.append(String.format(labels.getString(result.isLowerBound()
                    ? "BATTLESIM.WHATIF.ATLEAST" : "BATTLESIM.WHATIF.EXACT"),
                    result.getThreshold(), platoon.getQtd()));
            if (!result.isVerified()) {
                ret.append("\n\n").append(labels.getString("BATTLESIM.WHATIF.UNVERIFIED"));
            }
        }
        if (result.isLowerBound()) {
            ret.append("\n\n").append(labels.getString("BATTLESIM.WHATIF.FLOOR"));
        } else if (worstCase.isSelected() && worstCase.isEnabled()) {
            ret.append("\n\n").append(labels.getString("BATTLESIM.WHATIF.CEILINGNOTE"));
        }
        ret.append("\n\n").append(String.format(labels.getString("BATTLESIM.WHATIF.TRIALS"),
                result.getTrials()));
        return ret.toString();
    }

    /**
     * Opens the answer as a battle rather than leaving it as a number.
     *
     * T-842's own note asked for this: a threshold is most useful as something the player can look
     * at and argue with. The new window carries the worst-case swap if he asked for one, so what he
     * opens is the battle the sentence was actually about - not this one with a number changed.
     */
    private void doOpen() {
        if (found < 0) {
            return;
        }
        final CombatScenario built = subject();
        final ArmySim copy = built.getArmies().get(
                controler.getScenario().getArmies().indexOf(army));
        final Pelotao target = copy.getPelotoes().get(platoon.getCodigo());
        if (target == null) {
            return;
        }
        target.setQtd(found);
        // The player's own number now: he asked for this quantity, so it must not keep reporting
        // itself as somebody's estimate in the window that opens.
        built.setEdited(target);
        BattleSimWindow.openBeside((BattleSimWindow) getOwner(), built);
        dispose();
    }
}
