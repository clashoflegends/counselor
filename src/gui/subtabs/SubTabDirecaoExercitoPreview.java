package gui.subtabs;

import control.MapaControler;
import control.facade.WorldFacadeCounselor;
import control.support.ActorInterface;
import business.converter.ConverterFactory;
import gui.services.ArmyMovementSession;
import gui.services.SvgIcon;
import java.awt.Color;
import java.text.MessageFormat;
import java.util.stream.Collectors;
import javax.swing.AbstractButton;
import javax.swing.UIManager;
import model.Local;
import model.Ordem;

/**
 * Designer-owned preview layout using the same session and Swing adapter as Classic.
 */
public class SubTabDirecaoExercitoPreview extends ArmyMovementEditorBase {

    private Local displayedDestination;
    private String routeFeedback;

    public SubTabDirecaoExercitoPreview() {
        initComponents();
    }

    public SubTabDirecaoExercitoPreview(String initial, ActorInterface actor, MapaControler map,
            Ordem order, boolean all) {
        initComponents();
        configurePreview();
        initializeMovement(movementControls(), initial, actor, map, order, all);
    }

    private Controls movementControls() {
        return new Controls(jbNo, jbNe, jbC, jbL, jbO, jbSe, jbSo, jbApaga, jbBackspace,
                jlDisplay, jrbNormal, jrbEvasivo, null, jcbComida, jcTipoTropa, jtbDestinoMapa);
    }

    private void configurePreview() {
        jTextField1.setName("armyDestination");
        jTextField1.setText("");
        jTextField1.setToolTipText(labels.getString("ARMY.EDITOR.DESTINATION.TT"));
        jTextField1.getAccessibleContext().setAccessibleName(labels.getString("ARMY.EDITOR.DESTINATION"));
        jTextField1.addActionListener(event -> {
            String input = jTextField1.getText().trim();
            var locals = WorldFacadeCounselor.getInstance().getLocais();
            Local target = locals.get(input);
            if (target == null) {
                target = locals.values().stream().filter(local -> input.equals(local.getCoordenadas()))
                        .findFirst().orElse(null);
            }
            doMovementRouteTo(target);
        });
        jLabel1.setText(labels.getString("ARMY.EDITOR.DESTINATION"));
        jLabel1.setLabelFor(jTextField1);
        jlOrigem.setName("armyOrigin");
        jlDisplay.setName("armyRoute");
        jlCusto.setName("armyCost");
        jlPremissas.setName("armyAssumptions");
        jLabel3.setName("armyFeedback");
        icon(jbBackspace, "backspace", "ARMY.EDITOR.REMOVE.LAST");
        icon(jbApaga, "eraser", "ARMY.EDITOR.CLEAR");
        icon(jbC, "player-pause", "ARMY.EDITOR.HOLD");
        jrbEvasivo.setText(labels.getString("ARMY.EDITOR.EVASIVE"));
    }

    private void icon(AbstractButton button, String icon, String tooltip) {
        button.setText("");
        button.setIcon(SvgIcon.themed(icon, 16));
        button.setToolTipText(labels.getString(tooltip));
        button.getAccessibleContext().setAccessibleName(button.getToolTipText());
    }

    @Override
    protected void movementChanged() {
        routeFeedback = null;
        renderPreview();
    }

    private void renderPreview() {
        ArmyMovementSession session = movementSession();
        if (session == null) {
            return;
        }
        jlOrigem.setText(message("ARMY.EDITOR.ORIGIN", session.origin().getCoordenadas()));
        if (displayedDestination != session.destination()) {
            jTextField1.setText(session.destination().getCoordenadas());
            displayedDestination = session.destination();
        }
        jTextField1.setEnabled(session.context().enabled());
        String route = session.steps().stream().map(step -> switch (ConverterFactory.direcaoToInt(step.direction())) {
            case 1 -> labels.getString("NORTEOESTE.ABREVIADO");
            case 2 -> labels.getString("NORTELESTE.ABREVIADO");
            case 3 -> labels.getString("LESTE.ABREVIADO");
            case 4 -> labels.getString("SULLESTE.ABREVIADO");
            case 5 -> labels.getString("SULOESTE.ABREVIADO");
            case 6 -> labels.getString("OESTE.ABREVIADO");
            default -> labels.getString("ARMY.EDITOR.HOLD");
        }).collect(Collectors.joining("  "));
        jlDisplay.setText(route.isEmpty() ? "-" : route);
        jlDisplay.setToolTipText(session.isLandingStopped()
                ? String.format(labels.getString("ARMY.PATH.LANDING.STOP"), session.destination().getCoordenadas())
                : route);
        jlCusto.setText(message("ARMY.EDITOR.COST", session.cost(), session.context().allowance()));
        String troops = session.context().troops().stream().map(troop -> troop.getNome())
                .collect(Collectors.joining(", "));
        jlPremissas.setText(isAll() ? message("ARMY.EDITOR.SIMULATION", troops)
                : labels.getString("ARMY.EDITOR.ACTUAL"));
        jlPremissas.setToolTipText(troops);

        String feedback = routeFeedback;
        boolean warning = feedback != null;
        if (!session.context().enabled()) {
            feedback = labels.getString("ARMY.PATH.NO.SHIPS");
            warning = true;
        } else if (feedback == null && session.steps().stream().anyMatch(step -> step.cost() >= 9999)) {
            feedback = labels.getString("ARMY.EDITOR.IMPASSABLE");
            warning = true;
        } else if (feedback == null && session.cost() > session.context().allowance()) {
            feedback = message("ARMY.EDITOR.OVER", session.cost() - session.context().allowance());
            warning = true;
        } else if (feedback == null && session.isLandingStopped()) {
            feedback = String.format(labels.getString("ARMY.PATH.LANDING.STOP"), session.destination().getCoordenadas());
        }
        jLabel3.setText(feedback == null ? " " : feedback);
        jLabel3.setToolTipText(feedback);
        Color foreground = UIManager.getColor(warning ? "Component.warning.focusedBorderColor" : "Label.foreground");
        jLabel3.setForeground(foreground != null ? foreground : UIManager.getColor("Label.foreground"));
    }

    @Override
    public boolean doMovementRouteTo(Local target) {
        routeFeedback = null;
        boolean success = super.doMovementRouteTo(target);
        if (success) {
            jTextField1.setText(movementSession().destination().getCoordenadas());
        } else {
            routeFeedback = labels.getString("ARMY.PATH.PICK.INVALID");
            renderPreview();
        }
        return success;
    }

    @Override
    protected void showRouteFailure() {
        // Preview reports failure inline, keeping both the entered route and typed target.
    }

    private String message(String key, Object... arguments) {
        return MessageFormat.format(labels.getString(key).replace("'", "''"), arguments);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jbNo = new javax.swing.JButton();
        jbNe = new javax.swing.JButton();
        jbC = new javax.swing.JButton();
        jbL = new javax.swing.JButton();
        jbO = new javax.swing.JButton();
        jbSe = new javax.swing.JButton();
        jbSo = new javax.swing.JButton();
        jbApaga = new javax.swing.JButton();
        jlDisplay = new javax.swing.JLabel();
        jrbNormal = new javax.swing.JRadioButton();
        jrbEvasivo = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jcbComida = new javax.swing.JCheckBox();
        jcTipoTropa = new javax.swing.JComboBox();
        jbBackspace = new javax.swing.JButton();
        jtbDestinoMapa = new javax.swing.JButton();
        jlOrigem = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jlCusto = new javax.swing.JLabel();
        jlPremissas = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();

        jbNo.setText(labels.getString("NORTEOESTE.ABREVIADO")); // NOI18N
        jbNo.setActionCommand("nw");
        jbNo.setDefaultCapable(false);
        jbNo.setMaximumSize(new java.awt.Dimension(50, 20));
        jbNo.setMinimumSize(new java.awt.Dimension(50, 20));
        jbNo.setName("nw"); // NOI18N
        jbNo.setPreferredSize(new java.awt.Dimension(50, 20));

        jbNe.setText(labels.getString("NORTELESTE.ABREVIADO")); // NOI18N
        jbNe.setActionCommand("ne");
        jbNe.setDefaultCapable(false);
        jbNe.setMaximumSize(new java.awt.Dimension(50, 20));
        jbNe.setMinimumSize(new java.awt.Dimension(50, 20));
        jbNe.setName("ne"); // NOI18N
        jbNe.setPreferredSize(new java.awt.Dimension(50, 20));

        jbC.setText(labels.getString("CENTRO.ABREVIADO")); // NOI18N
        jbC.setActionCommand("h");
        jbC.setDefaultCapable(false);
        jbC.setMaximumSize(new java.awt.Dimension(50, 20));
        jbC.setMinimumSize(new java.awt.Dimension(50, 20));
        jbC.setName("h"); // NOI18N
        jbC.setPreferredSize(new java.awt.Dimension(50, 20));

        jbL.setText(labels.getString("LESTE.ABREVIADO")); // NOI18N
        jbL.setActionCommand("e");
        jbL.setDefaultCapable(false);
        jbL.setMaximumSize(new java.awt.Dimension(50, 20));
        jbL.setMinimumSize(new java.awt.Dimension(50, 20));
        jbL.setName("e"); // NOI18N
        jbL.setPreferredSize(new java.awt.Dimension(50, 20));

        jbO.setText(labels.getString("OESTE.ABREVIADO")); // NOI18N
        jbO.setActionCommand("w");
        jbO.setDefaultCapable(false);
        jbO.setMaximumSize(new java.awt.Dimension(50, 20));
        jbO.setMinimumSize(new java.awt.Dimension(50, 20));
        jbO.setName("w"); // NOI18N
        jbO.setPreferredSize(new java.awt.Dimension(50, 20));

        jbSe.setText(labels.getString("SULLESTE.ABREVIADO")); // NOI18N
        jbSe.setActionCommand("se");
        jbSe.setDefaultCapable(false);
        jbSe.setMaximumSize(new java.awt.Dimension(50, 20));
        jbSe.setMinimumSize(new java.awt.Dimension(50, 20));
        jbSe.setName("se"); // NOI18N
        jbSe.setPreferredSize(new java.awt.Dimension(50, 20));

        jbSo.setText(labels.getString("SULOESTE.ABREVIADO")); // NOI18N
        jbSo.setActionCommand("sw");
        jbSo.setDefaultCapable(false);
        jbSo.setMaximumSize(new java.awt.Dimension(50, 20));
        jbSo.setMinimumSize(new java.awt.Dimension(50, 20));
        jbSo.setName("sw"); // NOI18N
        jbSo.setPreferredSize(new java.awt.Dimension(50, 20));

        jbApaga.setText(labels.getString("APAGA"));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("labels"); // NOI18N
        jbApaga.setToolTipText(bundle.getString("MOVER.EXERCITO.ERASE.TOOLTIP")); // NOI18N
        jbApaga.setActionCommand("apaga");
        jbApaga.setDefaultCapable(false);
        jbApaga.setMaximumSize(new java.awt.Dimension(50, 20));
        jbApaga.setMinimumSize(new java.awt.Dimension(50, 20));
        jbApaga.setName("Erase"); // NOI18N
        jbApaga.setPreferredSize(new java.awt.Dimension(28, 24));

        jlDisplay.setText("-");
        jlDisplay.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        buttonGroup1.add(jrbNormal);
        jrbNormal.setSelected(true);
        jrbNormal.setText(labels.getString("NORMAL")); // NOI18N
        jrbNormal.setActionCommand("normal");

        buttonGroup1.add(jrbEvasivo);
        jrbEvasivo.setText(labels.getString("EVASIVO")); // NOI18N
        jrbEvasivo.setActionCommand("evasivo");

        jLabel1.setText(labels.getString("DESTINO: ")); // NOI18N
        jLabel1.setToolTipText(labels.getString("DESTINO.TOOLTIP")); // NOI18N

        jcbComida.setText(labels.getString("SEM.COMIDA")); // NOI18N
        jcbComida.setToolTipText(bundle.getString("MOVER.EXERCITO.FOOD.TOOLTIP")); // NOI18N
        jcbComida.setActionCommand("comida");

        jcTipoTropa.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jcTipoTropa.setToolTipText(labels.getString("MOVER.EXERCITO.TIPOTROPA.TOOLTIP")); // NOI18N
        jcTipoTropa.setActionCommand("tipoTropaChanged");

        jbBackspace.setText("⌫"); // NOI18N
        jbBackspace.setToolTipText(bundle.getString("MOVER.EXERCITO.BACKSPACE.TOOLTIP")); // NOI18N
        jbBackspace.setActionCommand("jbBackspace");
        jbBackspace.setDefaultCapable(false);
        jbBackspace.setMaximumSize(new java.awt.Dimension(28, 24));
        jbBackspace.setMinimumSize(new java.awt.Dimension(28, 24));
        jbBackspace.setName("jbBackspace"); // NOI18N
        jbBackspace.setPreferredSize(new java.awt.Dimension(28, 24));

        jtbDestinoMapa.setText("⌫"); // NOI18N
        jtbDestinoMapa.setToolTipText(bundle.getString("MOVER.EXERCITO.BACKSPACE.TOOLTIP")); // NOI18N
        jtbDestinoMapa.setActionCommand("armyDestinationPick");
        jtbDestinoMapa.setDefaultCapable(false);
        jtbDestinoMapa.setMaximumSize(new java.awt.Dimension(30, 16));
        jtbDestinoMapa.setMinimumSize(new java.awt.Dimension(30, 16));
        jtbDestinoMapa.setName("armyDestinationPick"); // NOI18N
        jtbDestinoMapa.setPreferredSize(new java.awt.Dimension(30, 16));

        jlOrigem.setText("jLabel2");

        jTextField1.setText("jTextField1");
        jTextField1.setBorder(null);

        jlCusto.setText("jLabel2");

        jlPremissas.setText("jLabel2");

        jLabel3.setText("jLabel3");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jlPremissas, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jlCusto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jcTipoTropa, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jlDisplay, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jcbComida)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jrbNormal))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jlOrigem)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabel1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jrbEvasivo)
                                .addGap(0, 21, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(29, 29, 29)
                                .addComponent(jbNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jbNe, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jbO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jbC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jbL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(30, 30, 30)
                                        .addComponent(jbSo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jbSe, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jbApaga, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jbBackspace, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jtbDestinoMapa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbNe, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbO, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbBackspace, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbSo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbSe, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbApaga, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jtbDestinoMapa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jlOrigem)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jcbComida)
                    .addComponent(jrbNormal, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jrbEvasivo, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jcTipoTropa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jlDisplay)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jlCusto)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jlPremissas)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JButton jbApaga;
    private javax.swing.JButton jbBackspace;
    private javax.swing.JButton jbC;
    private javax.swing.JButton jbL;
    private javax.swing.JButton jbNe;
    private javax.swing.JButton jbNo;
    private javax.swing.JButton jbO;
    private javax.swing.JButton jbSe;
    private javax.swing.JButton jbSo;
    private javax.swing.JComboBox jcTipoTropa;
    private javax.swing.JCheckBox jcbComida;
    private javax.swing.JLabel jlCusto;
    private javax.swing.JLabel jlDisplay;
    private javax.swing.JLabel jlOrigem;
    private javax.swing.JLabel jlPremissas;
    private javax.swing.JRadioButton jrbEvasivo;
    private javax.swing.JRadioButton jrbNormal;
    private javax.swing.JButton jtbDestinoMapa;
    // End of variables declaration//GEN-END:variables
}
