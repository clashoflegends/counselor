package gui.subtabs;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoComboObject;
import business.MovimentoExercito;
import business.converter.ConverterFactory;
import business.facade.ExercitoFacade;
import control.DirecaoExercitoControler;
import control.MapaControler;
import control.facade.WorldFacadeCounselor;
import control.support.ActorInterface;
import gui.TabBase;
import gui.services.ArmyMovementParameters;
import gui.services.ArmyMovementSession;
import gui.services.SvgIcon;
import gui.services.Toast;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import model.Local;
import model.Ordem;
import model.Pelotao;
import model.TipoTropa;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/** Shared Swing adapter for the two designer-owned movement layouts. */
public abstract class ArmyMovementEditorBase extends TabBase implements Serializable, ArmyMovementParameters {
    private static final Log log = LogFactory.getLog(ArmyMovementEditorBase.class);
    protected static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private ArmyMovementSession movementSession;
    private DirecaoExercitoControler dirExControl;
    private Local origem;
    private Ordem ordem;
    private ActorInterface actor;
    private int moveType = 0;
    private boolean all = false;
    private int limiteMovimento = 0;
    private boolean destinationPickArmed;
    private Object previousEscape;
    private Object previousFocusedEscape;
    private final Consumer<Local> destinationPickCallback = this::onMapDestinationPick;

    protected record Controls(javax.swing.JButton nw, javax.swing.JButton ne,
            javax.swing.JButton hold, javax.swing.JButton east, javax.swing.JButton west,
            javax.swing.JButton se, javax.swing.JButton sw, javax.swing.JButton clear,
            javax.swing.JButton removeLast, javax.swing.JLabel display,
            javax.swing.JRadioButton normal, javax.swing.JRadioButton evasive,
            javax.swing.JLabel destination, javax.swing.JCheckBox noFood,
            javax.swing.JComboBox troop, javax.swing.JButton mapPick) {
    }

    protected Controls controls;

    protected final void initializeMovement(Controls controls, String initial,
            ActorInterface actor, MapaControler map, Ordem order, boolean all) {
        this.controls = controls;
        setAll(all);
        setMapaControler(map);
        this.ordem = order;
        this.actor = actor;
        setDefaultValues();
        initConfig(actor.getLocal(), initial);
    }

    protected final void initializeSimulation(Controls controls, Local origin,
            int allowance, boolean water, MapaControler map) {
        this.controls = controls;
        setAll(true);
        setMapaControler(map);
        setLimiteMovimento(allowance);
        setAgua(water);
        initConfig(origin, "");
    }

    protected final ArmyMovementSession movementSession() {
        return movementSession;
    }

    protected final ActorInterface movementActor() {
        return actor;
    }

    protected void movementChanged() {
        // Optional presentation hook; canonical state stays in the session.
    }

    private void initConfig(Local local, String vlInicial) {
        //setValorInicial(vlInicial);
        this.origem = local;
        dirExControl = new DirecaoExercitoControler(this);
        //set combo.model
        final GenericoComboBoxModel cbmTemp = getTropaModel();
        controls.troop().setModel(cbmTemp);
        controls.troop().setVisible(isAll());
        //set default item - comida
        final ExercitoFacade ef = new ExercitoFacade();
        if (WorldFacadeCounselor.getInstance().hasResourceManagement()) {
            if (actor != null) {
                setComidaSelected(!ef.isComida(actor.getExercito()));
            } else {
                setComidaSelected(false);
            }
            doFieldsDisplay();
        } else {
            setComidaSelected(false);
            doFieldsHide();
        }
        //set default item - slower troop
        int slow = -1, index = -1;
        for (int ii = 0; ii < cbmTemp.getSize(); ii++) {
            final GenericoComboObject temp = (GenericoComboObject) cbmTemp.getElementAt(ii);
            final TipoTropa tpTropa = (TipoTropa) temp.getObject();
            final List<TipoTropa> tropas = new ArrayList<>(2);
            tropas.add(tpTropa);
            final int current = ef.getCustoMovimentoBase(tropas, origem.getTerreno(), false, isAgua());
            if (current > slow) {
                index = ii;
                slow = current;
            }
        }
        controls.troop().setSelectedIndex(index);
        // Saved display is also the editor's input: restore mode before costing steps.
        final String[] initialTokens = vlInicial.split(";");
        if (initialTokens.length > 0 && "ev".equals(initialTokens[initialTokens.length - 1])) {
            setDirecaoTipo("ev");
        }
        movementSession = new ArmyMovementSession(WorldFacadeCounselor.getInstance().getLocais(),
                origem, createMovementContext());
        //set initial tag
        this.doMovementTagsPaint(vlInicial);
        setListeners();
        installBackspace();
        installDestinationPick();
        updateGui();
    }

    private void installDestinationPick() {
        controls.mapPick().setText("");
        controls.mapPick().setIcon(SvgIcon.themed("map", 16));
        controls.mapPick().setMargin(new Insets(1, 1, 1, 1));
        final boolean shipsAvailable = hasRequiredShips();
        controls.mapPick().setToolTipText(labels.getString(shipsAvailable
                ? "ARMY.PATH.PICK.TT" : "ARMY.PATH.NO.SHIPS"));
        controls.mapPick().getAccessibleContext().setAccessibleName(controls.mapPick().getToolTipText());
        controls.mapPick().setEnabled(getMapaControler() != null && shipsAvailable);
        if (!shipsAvailable) {
            for (var button : List.of(controls.nw(), controls.ne(), controls.hold(), controls.east(), controls.west(), controls.se(), controls.sw())) {
                button.setEnabled(false);
                button.setToolTipText(labels.getString("ARMY.PATH.NO.SHIPS"));
            }
        }
        controls.mapPick().addActionListener(e -> {
            if (destinationPickArmed) {
                cancelDestinationPick();
            } else if (getMapaControler() != null) {
                destinationPickArmed = true;
                bindPickerEscape();
                controls.mapPick().setSelected(true);
                getMapaControler().armCityPick(destinationPickCallback);
                final Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    Toast.show(window, labels.getString("ARMY.PATH.PICK.HINT"));
                }
            }
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) {
                cancelDestinationPick();
            }
        });
    }

    private void cancelDestinationPick() {
        if (getMapaControler() != null) {
            if (destinationPickArmed && getMapaControler().isCityPickArmed(destinationPickCallback)) {
                Toast.dismissCurrent();
            }
            getMapaControler().cancelCityPick(destinationPickCallback);
        }
        if (destinationPickArmed) {
            restoreEscape(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, previousEscape);
            restoreEscape(JComponent.WHEN_FOCUSED, previousFocusedEscape);
        }
        destinationPickArmed = false;
        controls.mapPick().setSelected(false);
    }

    private void bindPickerEscape() {
        KeyStroke escape = KeyStroke.getKeyStroke("ESCAPE");
        previousEscape = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(escape);
        previousFocusedEscape = getInputMap(JComponent.WHEN_FOCUSED).get(escape);
        getActionMap().put("cancelArmyDestinationPick", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                cancelDestinationPick();
            }
        });
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escape, "cancelArmyDestinationPick");
        getInputMap(JComponent.WHEN_FOCUSED).put(escape, "cancelArmyDestinationPick");
    }

    private void restoreEscape(int condition, Object previous) {
        KeyStroke escape = KeyStroke.getKeyStroke("ESCAPE");
        if (previous == null) {
            getInputMap(condition).remove(escape);
        } else {
            getInputMap(condition).put(escape, previous);
        }
    }

    @Override
    public void removeNotify() {
        if (controls != null) {
            cancelDestinationPick();
        }
        super.removeNotify();
    }

    private void onMapDestinationPick(Local target) {
        if (!destinationPickArmed || !isShowing()) {
            return;
        }
        cancelDestinationPick();
        Toast.dismissCurrent();
        if (!doMovementRouteTo(target)) {
            showRouteFailure();
        }
    }

    protected void showRouteFailure() {
        Toast.showError(labels.getString("ARMY.PATH.PICK.INVALID"));
    }

    /** Plan before clearing: an invalid pick must not destroy the player's entered route. */
    public boolean doMovementRouteTo(Local target) {
        movementSession.setContext(createMovementContext());
        if (!movementSession.routeTo(target)) {
            return false;
        }
        paintSession();
        return true;
    }

    /** Shared inputs for automatic routing and manual step-by-step entry. */
    private ArmyMovementSession.Context createMovementContext() {
        final List<TipoTropa> troops = new ArrayList<>();
        if (!isAll()) {
            for (GenericoComboObject troop : getTropaModel().getElementAll()) {
                troops.add((TipoTropa) troop.getObject());
            }
        } else {
            final GenericoComboObject troop = (GenericoComboObject) controls.troop().getSelectedItem();
            if (troop != null) {
                troops.add((TipoTropa) troop.getObject());
            }
        }
        return new ArmyMovementSession.Context(troops, !controls.noFood().isSelected(),
                controls.evasive().isSelected(), getLimiteMovimento(), getMoveType(), hasRequiredShips());
    }

    private void installBackspace() {
        final AbstractAction backspace = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
                        instanceof JTextComponent)) {
                    doApagaUltimo();
                }
            }
        };
        controls.removeLast().addActionListener(backspace);
        controls.removeLast().getAccessibleContext().setAccessibleName(controls.removeLast().getToolTipText());

        getActionMap().put("movementBackspace", backspace);
        final KeyStroke key = KeyStroke.getKeyStroke("BACK_SPACE");
        getInputMap(JComponent.WHEN_FOCUSED).put(key, "movementBackspace");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, "movementBackspace");
    }



    public String getDirecoesIdTipo() {
        return movementSession.getDirecoesIdTipo();
    }

    public String getDirecaoDisplay() {
        return movementSession.getDirecaoDisplay();
    }

    /**
     * @param aDirecaoTipo the direcaoTipo to set
     */
    public void setDirecaoTipo(String aDirecaoTipo) {
        controls.evasive().setSelected("ev".equals(aDirecaoTipo));
        controls.normal().setSelected(!"ev".equals(aDirecaoTipo));
        if (movementSession != null) {
            movementSession.setContext(createMovementContext());
        }
    }

    public void doApaga() {
        movementSession.clear();
        paintSession();
    }

    public void doApagaUltimo() {
        movementSession.removeLast();
        doMovementTagsRepaint();
    }

    public void doMovementTagAdd(String direcao) {
        movementSession.setContext(createMovementContext());
        if (movementSession.append(direcao)) {
            paintSession();
        }
    }

    private void doMovementTagsPaint(String vlInicial) {
        final List<String> directions = new ArrayList<>();
        directions.addAll(ConverterFactory.armyPathToList(vlInicial));
        for (String elem : directions) {
            this.doMovementTagAdd(elem);
        }
    }

    /**
     * reinicia o movimento, calculando corretamente os valores.
     */
    public void doMovementTagsRepaint() {
        movementSession.setContext(createMovementContext());
        paintSession();
    }

    private void paintSession() {
        if (getMapaControler() != null) {
            getMapaControler().remMovementTag();
            for (ArmyMovementSession.Step step : movementSession.steps()) {
                getMapaControler().addMovementTag(step.movement());
            }
        }
        updateGui();
    }

    private void updateGui() {
        final boolean shipsAvailable = hasRequiredShips();
        controls.display().setText(shipsAvailable ? getDirecaoDisplay() : labels.getString("ARMY.PATH.NO.SHIPS"));
        controls.display().setToolTipText(shipsAvailable ? null : labels.getString("ARMY.PATH.NO.SHIPS"));
        if (shipsAvailable && movementSession.isLandingStopped()) {
            controls.display().setToolTipText(String.format(labels.getString("ARMY.PATH.LANDING.STOP"),
                    movementSession.destination().getCoordenadas()));
        }
        if (controls.destination() != null) {
            controls.destination().setText(movementSession.destination().getCoordenadas());
        }
        movementChanged();
    }

    /**
     * @return the agua
     */
    private boolean isAgua() {
        return getMoveType() == MovimentoExercito.BY_WATER;
    }

    private int getMoveType() {
        return moveType;
    }

    private void setMoveType(int moveType) {
        this.moveType = moveType;
    }

    /**
     * @param water the agua to set
     */
    private void setAgua(boolean water) {
        if (water) {
            //variable costs per terrain on water
            setMoveType(MovimentoExercito.BY_WATER);
        } else {
            //else, variable costs per terrain on land
            setMoveType(MovimentoExercito.BY_LAND);
        }
    }

    /**
     * @return the limiteMovimento
     */
    public int getLimiteMovimento() {
        return limiteMovimento;
    }

    /**
     * @param limiteMovimento the limiteMovimento to set
     */
    private void setLimiteMovimento(int limiteMovimento) {
        this.limiteMovimento = limiteMovimento;
    }

    /**
     * @return the all
     */
    public final boolean isAll() {
        return all;
    }

    /**
     * @param all the all to set
     */
    private void setAll(boolean all) {
        this.all = all;
    }

    protected final void doFieldsHide() {
        controls.noFood().setVisible(false);
        controls.evasive().setVisible(false);
        controls.normal().setVisible(false);
    }

    protected final void doFieldsDisplay() {
        controls.noFood().setVisible(true);
        controls.evasive().setVisible(true);
        controls.normal().setVisible(true);
    }

    protected final void setComidaSelected(boolean food) {
        controls.noFood().setSelected(food);
    }

    private void setListeners() {
        //add listeners
        controls.nw().addActionListener(dirExControl);
        controls.ne().addActionListener(dirExControl);
        controls.hold().addActionListener(dirExControl);
        controls.east().addActionListener(dirExControl);
        controls.west().addActionListener(dirExControl);
        controls.se().addActionListener(dirExControl);
        controls.sw().addActionListener(dirExControl);
        controls.clear().addActionListener(dirExControl);
        controls.evasive().addActionListener(dirExControl);
        controls.normal().addActionListener(dirExControl);
        controls.troop().addActionListener(dirExControl);
        controls.noFood().addActionListener(dirExControl);
    }

    private GenericoComboBoxModel getTropaModel() {
        if (isAll() || actor == null) {
            //assume all troops are available
            return dirExControl.getTropaTipoComboModel(isAgua());
//        } else if (actor.getExercito() == null) {
//            //LOM has PCs marching as armies, but at a cost of 1 point
//            //If the army has no troops, the cost of each move will default to 1.
//            Collection<TipoTropa> list = new ArrayList<TipoTropa>();
//            GenericoComboBoxModel model = new GenericoComboBoxModel(list.toArray(new TipoTropa[0]));
//            return model;
        } else if (isAgua()) {
            // Actual sailing needs actual ships; an empty roster must not become hypothetical ships.
            return new GenericoComboBoxModel(getActualShips().toArray(new TipoTropa[0]));
        } else if (actor.getExercito() == null) {
            //assume all troops are available
            return dirExControl.getTropaTipoComboModel(isAgua());
        } else {
            return dirExControl.getTropaTipoComboModel(actor.getExercito(), isAgua());
        }
    }

    private boolean hasRequiredShips() {
        return !isAgua() || isAll() || actor == null || !getActualShips().isEmpty();
    }

    private List<TipoTropa> getActualShips() {
        final List<TipoTropa> ships = new ArrayList<>();
        if (actor != null && actor.getExercito() != null) {
            for (Pelotao platoon : actor.getExercito().getPelotoes().values()) {
                if (platoon.getQtd() > 0 && platoon.getTipoTropa().isBarcos()) {
                    ships.add(platoon.getTipoTropa());
                }
            }
        }
        return ships;
    }

    private void setDefaultValues() {
        if (ordem.hasHabilidade(";AMF;")) {
            //if fixed cost of 1
            setMoveType(MovimentoExercito.BY_FIXED);
        } else if (ordem.hasHabilidade(";AMW;")) {
            //else, variable costs per terrain on water
            setMoveType(MovimentoExercito.BY_WATER);
        } else {
            //else, variable costs per terrain on land
            setMoveType(MovimentoExercito.BY_LAND);
        }
        if (ordem.hasHabilidade(";AM12;")) {
            setLimiteMovimento(12);
            return;
        }
        if (ordem.hasHabilidade(";AM14;")) {
            setLimiteMovimento(14);
            return;
        }
        if (ordem.hasHabilidade(";AM08;")) {
            setLimiteMovimento(8);
            return;
        }
//        //FIXME: clean up after a while (7/5/2016) once it's in the EGF.
//        if (ordem.getCodigo().equals("850") && WorldFacadeCounselor.getInstance().hasResourceManagement()) {
//            setLimiteMovimento(12);
//            setMoveType(MovimentoExercito.BY_LAND);
//        } else if (ordem.getCodigo().equals("850") && !WorldFacadeCounselor.getInstance().hasResourceManagement()) {
//            setLimiteMovimento(14);
//            setMoveType(MovimentoExercito.BY_LAND);
//        } else if (ordem.getCodigo().equals("851")) {
//            setLimiteMovimento(14);
//            setMoveType(MovimentoExercito.BY_LAND);
//        } else if (ordem.getCodigo().equals("852")) {
//            setLimiteMovimento(14);
//            setMoveType(MovimentoExercito.BY_LAND);
//        } else if (ordem.getCodigo().equals("860")) {
//            setLimiteMovimento(14);
//            setMoveType(MovimentoExercito.BY_LAND);
//        } else if (ordem.getCodigo().equals("830")) {
//            setLimiteMovimento(14);
//            setMoveType(MovimentoExercito.BY_WATER);
//        } else if (ordem.getCodigo().equals("831")) {
//            setLimiteMovimento(14);
//            setMoveType(MovimentoExercito.BY_WATER);
//        } else {
//            setLimiteMovimento(0);
//            setMoveType(MovimentoExercito.BY_LAND);
//        }
    }

}
