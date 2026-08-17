/*
 * SubTabTropas.java
 *
 * Created on 30/Abr/2011, 13:15:51
 */
package gui.subtabs;

import baseLib.GenericoComboBoxModel;
import baseLib.IBaseModel;
import business.MovimentoPersonagem;
import business.facade.LocalFacade;
import control.MapaControler;
import gui.TabBase;
import gui.services.SvgIcon;
import gui.services.Toast;
import java.awt.Window;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import model.Local;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.WorldManager;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public class SubTabCoordenadas extends TabBase implements Serializable {

    private static final Log log = LogFactory.getLog(SubTabCoordenadas.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final LocalFacade localFacade = new LocalFacade();
    private JToggleButton mapBtn;
    // Optional auto-save (player request): set by ComponentFactory only when this coordinate is the SOLE
    // parameter of the order; a successful map-pick then saves+advances if AutoSaveOnMapPick is on. Null otherwise.
    private Runnable autoSaveOnPick;
    // What this picker may target, for the map's range border. Null origem = unbounded (ALL ticked), which
    // draws nothing; MapaControler also ignores the 9999 range the unbounded Coordenada* controls pass.
    private Local rangeOrigem;
    private int range;

    /** Wire the map-pick to also save the order + advance (single-parameter orders only). See ComponentFactory. */
    public void setAutoSaveOnPick(Runnable autoSaveOnPick) {
        this.autoSaveOnPick = autoSaveOnPick;
    }

    public SubTabCoordenadas(String vlInicial, Local origem, int range, boolean all, boolean water, int displayCities, MapaControler mapaControl) {
        //water=true, lista todos os hexes, water=false, lista apenas os hexes de terra (nao agua)
        initComponents();
        //define o controler
        this.setMapaControler(mapaControl);
        //remember what this picker may target, so the map can outline it (see addNotify)
        this.rangeOrigem = all ? null : origem;
        this.range = range;
        //liga actionlisteners para a combo
        jcLocais.addItemListener(mapaControl);
        jcLocais.setActionCommand("Coordenadas");
        //seta model no combo do panel
        GenericoComboBoxModel cbmTemp = getCombo(origem, range, water, displayCities, all);
        jcLocais.setModel(cbmTemp);
        //seleciona vlInicial como default
        if (vlInicial == null || vlInicial.equals("")) {
            vlInicial = localFacade.getCoordenadas(origem);
        }
        int index = cbmTemp.getIndexByDisplay(vlInicial);
        if (index == 0 && jcLocais.isEditable()) {
            jcLocais.insertItemAt(vlInicial, 0);
        }
        //desenha no mapa quando selecionado ou vlInicial
        if (index == 0) {
            //to force a change on first load
            jcLocais.setSelectedIndex(-1);
        }
        jcLocais.setSelectedIndex(index);
        installMapPick();
        installRangeOutline();
    }

    /**
     * Show the range border while this picker is actually on screen, and take it down when it is not.
     * <p>
     * Driven by {@code SHOWING_CHANGED} rather than {@code addNotify}/{@code removeNotify}, because the
     * data tabs all live in one {@code JTabbedPane} and are never removed from the hierarchy - measured,
     * a tab switch fires NEITHER notify, only the hierarchy event. Hanging this on the notifies left the
     * border on the map, anchored to an actor no longer selected anywhere, until the player returned to
     * the tab and changed order. The hierarchy event covers tab switches AND removal (the rebuild of the
     * parameter panel on actor/order change, and EGF load), so it is the whole protocol.
     * <p>
     * Fires more than once for a single switch, so both calls must be idempotent - they are: the show
     * path hits the cached shape, and the clear is guarded on ownership identity.
     * <p>
     * Deliberately NOT hooked to clearMovementTags - that fires on every hex selection, i.e. while the
     * player is still choosing, which is exactly when the border must stay up.
     */
    private void installRangeOutline() {
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) == 0
                    || getMapaControler() == null) {
                return;
            }
            if (isShowing()) {
                getMapaControler().showRangeOutline(this, rangeOrigem, range);
            } else {
                getMapaControler().clearRangeOutline(this); // by identity - see clearRangeOutline
            }
        });
    }

    /** Adds a "pick a hex on the map" button beside the coordinate combo (covers all Coordenada* controls). */
    private void installMapPick() {
        mapBtn = new JToggleButton(SvgIcon.themed("map", 16));
        mapBtn.setToolTipText(labels.getString("COORD.PICK.MAP.TT"));
        mapBtn.addActionListener(e -> {
            if (getMapaControler() == null) {
                return;
            }
            if (mapBtn.isSelected()) {
                Window w = SwingUtilities.getWindowAncestor(this);
                if (w != null) {
                    Toast.show(w, labels.getString("COORD.PICK.MAP.HINT"));
                }
                getMapaControler().armCityPick(this::onMapPick);
            } else {
                getMapaControler().cancelCityPick();
                Toast.dismissCurrent();
            }
        });
        this.add(mapBtn);
        javax.swing.GroupLayout gl = new javax.swing.GroupLayout(this);
        this.setLayout(gl);
        gl.setHorizontalGroup(gl.createSequentialGroup()
                .addComponent(jcLocais, 0, 208, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mapBtn));
        gl.setVerticalGroup(gl.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(jcLocais)
                .addComponent(mapBtn));
    }

    /**
     * One-shot map click (armed by the map button): select the clicked hex if it's a valid target for
     * THIS control (present in the combo - i.e. in range and matching the terrain/city filter); else a
     * brief error toast. Matches on the coordinate display, like the initial-value select in the ctor.
     */
    private void onMapPick(Local local) {
        mapBtn.setSelected(false);        // one-shot; MapaControler already cleared its callback
        Toast.dismissCurrent();
        if (!isShowing()) {
            return;                       // stale widget (param area was rebuilt) - ignore
        }
        final String coord = (local == null) ? null : localFacade.getCoordenadas(local);
        if (coord != null) {
            ComboBoxModel m = jcLocais.getModel();
            for (int i = 0; i < m.getSize(); i++) {
                Object o = m.getElementAt(i);
                if (o instanceof IBaseModel && coord.equals(((IBaseModel) o).getComboDisplay())) {
                    jcLocais.setSelectedIndex(i);
                    maybeAutoSave();
                    return;
                }
            }
        }
        Toast.showError(labels.getString("COORD.PICK.INVALID"));
    }

    /** If this coordinate is the order's only parameter and AutoSaveOnMapPick is on, save+advance after the
     *  pick (deferred so the combo selection / listeners settle first). */
    private void maybeAutoSave() {
        if (autoSaveOnPick != null && SettingsManager.getInstance().isAutoSaveOnMapPick()) {
            SwingUtilities.invokeLater(autoSaveOnPick);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jcLocais = new javax.swing.JComboBox();

        jcLocais.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jcLocais, 0, 208, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jcLocais)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jcLocais;
    // End of variables declaration//GEN-END:variables

    private GenericoComboBoxModel getCombo(Local origem, int range, boolean water, int displayCities, boolean all) {
        /* displayCities == 0, all
         *  displayCities == 1, cities only
         *  displayCities == 2, hide cities and landmarks
         */
        //prep list
        List<MovimentoPersonagem> lista = new ArrayList<>(WorldManager.getInstance().getLocais().size());
        //monta a lista de locais
        for (Local local : WorldManager.getInstance().getLocais().values()) {
            if (all) {
                //verifica se ALL
                lista.add(new MovimentoPersonagem(origem, local, range, water));
            } else if (!localFacade.isAgua(local) || water) {
                if (displayCities == 2 && (localFacade.isCidade(local) || localFacade.isTerrainLandmark(local))) {
                    //hide cities
                    continue;
                }
                if (displayCities == 1 && !localFacade.isCidade(local)) {
                    //cities only
                    continue;
                }
                //filtra os locais no range
                if (localFacade.getDistancia(origem, local) <= range) {
                    //verifica se Water
                    lista.add(new MovimentoPersonagem(origem, local, range, water));
                }
            }
        }
        GenericoComboBoxModel model = new GenericoComboBoxModel(lista.toArray(new MovimentoPersonagem[0]));
        return model;
    }

    public IBaseModel getCoordenadaSelected() {
        return (IBaseModel) jcLocais.getModel().getSelectedItem();
    }
}
