/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoComboObject;
import baseLib.IBaseModel;
import business.ImageManager;
import business.combat.ArmySim;
import business.converter.ConverterFactory;
import business.facade.BattleSimFacade;
import business.facade.CidadeFacade;
import business.facade.ExercitoFacade;
import business.facade.LocalFacade;
import business.facade.NacaoFacade;
import control.facade.WorldFacadeCounselor;
import control.services.AcaoConverter;
import control.services.CenarioConverter;
import control.services.ExercitoConverter;
import control.services.FiltroConverter;
import control.support.DispatchManager;
import control.support.WindowPopupText;
import gui.accessories.BattleCasualtySimulatorNew;
import gui.accessories.TroopsCasualtiesList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import model.Cidade;
import model.Exercito;
import model.Nacao;
import model.Ordem;
import model.Pelotao;
import model.Terreno;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 *
 * @author gurgel
 */
public class BattleSimulatorControlerNew implements Serializable, ChangeListener, ListSelectionListener, ActionListener {

    private static final Log log = LogFactory.getLog(BattleSimulatorControlerNew.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final BattleCasualtySimulatorNew tabGui;
    private final List<ArmySim> armiesList = new ArrayList<>();
    private ArmySim armySelected;
    private Terreno terreno;
    private Cidade cityClone;
    private int rowIndex = 0;
    private BattleSimPlatoonCasualtyControlerNew casualtyControler;
    private final BattleSimFacade bsf = new BattleSimFacade();
    private final NacaoFacade nacaoFacade = new NacaoFacade();
    private final CidadeFacade cidadeFacade = new CidadeFacade();
    private final ExercitoFacade exercitoFacade = new ExercitoFacade();
    private final LocalFacade localFacade = new LocalFacade();

    public BattleSimulatorControlerNew(BattleCasualtySimulatorNew tabGui) {
        this.tabGui = tabGui;
    }

    public String getAjuda(Ordem ordem) {
        return AcaoConverter.getAjuda(ordem);
    }

    public BattleCasualtySimulatorNew getTabGui() {
        return tabGui;
    }

    public Terreno getTerrain() {
        return terreno;
    }

    private void setTerreno(Terreno terreno) {
        this.terreno = terreno;
    }

    public void setCity(Cidade city) {
        cityClone = city;
    }

    public Cidade getCity() {
        return cityClone;
    }

    public void setCasualtyControler(BattleSimPlatoonCasualtyControlerNew casualtyControler) {
        this.casualtyControler = casualtyControler;
    }

    public void doChangeTerrain(Terreno terrain) {
        setTerreno(terrain);
        this.getTabGui().setIconImage(ImageManager.getInstance().getTerrainImages(terrain.getCodigo()));
    }

    private void updateArmyCasualtyControler(ArmySim exercito, Terreno terreno) {
        if (this.casualtyControler == null) {
            //safe to ignore if not initialized
            return;
        }
        this.casualtyControler.updateArmy(exercito, terreno);
    }

    public ComboBoxModel listFiltroTactic() {
        return CenarioConverter.getInstance().getTaticaComboModel();
    }

    public GenericoComboBoxModel getNacaoComboModel() {
        List<IBaseModel> lista = new ArrayList<>(WorldFacadeCounselor.getInstance().getNacoes().values());
        return new GenericoComboBoxModel(lista.toArray(new IBaseModel[0]));
    }

    public TableModel getArmyListTableModel(Collection<Exercito> armies) {
        for (Exercito army : armies) {
            //FIXME: Needs deep clone for pelotao
            armiesList.add(bsf.clone(army));
        }
        return ExercitoConverter.getBattleModel(armiesList);
    }

    public GenericoComboBoxModel getTerrenoComboModel() {
        List<IBaseModel> lista = new ArrayList<>(WorldFacadeCounselor.getInstance().getCenario().getTerrenos().values());
        return new GenericoComboBoxModel(lista.toArray(new IBaseModel[0]));
    }

    private void doRemoveArmy(ArmySim army) {
        armiesList.remove(army);
        rowIndex--;
        doRefreshArmies();
    }

    private void doCloneArmy(ArmySim army) {
        armiesList.add(bsf.clone(army));
        rowIndex = armiesList.size() - 1;
        doRefreshArmies();
    }

    private void doNewArmy() {
        //FIXME: needs to receive Local for the Battle to be resolved. Deal with this later. Local could be stored in GUi or Control.
        armiesList.add(new ArmySim("Blank", getTerrain(), getTabGui().getNation()));
        rowIndex = armiesList.size() - 1;
        doRefreshArmies();
    }

    public void doRefreshArmies() {
        this.getTabGui().setArmyModel(ExercitoConverter.getBattleModel(armiesList), rowIndex);
    }

    public ComboBoxModel listFiltroTypes() {
        return new GenericoComboBoxModel(FiltroConverter.listFiltroLW());
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        if (event.getSource() instanceof JSlider) {
            JSlider source = (JSlider) event.getSource();
            if (!source.getValueIsAdjusting()) {
                if ("jsCityLoyalty".equals(source.getName()) || "jsCitySize".equals(source.getName()) || "jsCityFortification".equals(source.getName())) {
                    this.getTabGui().updateCityLabels();
                } else {
                    log.info(source.getName());
                }
            }
        } else if (event.getSource() instanceof JSpinner) {
            try {
                JSpinner source = (JSpinner) event.getSource();
                if (null != source.getName()) {
                    switch (source.getName()) {
                        case "jsArmyCommander":
                            armySelected.setComandante((Integer) source.getValue());
                            break;
                        case "jsArmyMorale":
                            armySelected.setMoral((Integer) source.getValue());
                            break;
                        case "jsArmyAbonus":
                            armySelected.setBonusAttack((Integer) source.getValue());
                            break;
                        case "jsArmyDbonus":
                            armySelected.setBonusDefense((Integer) source.getValue());
                            break;
                        default:
                            break;
                    }
                }
                doRefreshArmies();
            } catch (NullPointerException e) {
                //hex with no army
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        try {
            ListSelectionModel lsm = (ListSelectionModel) event.getSource();
            if (lsm.isSelectionEmpty()) {
                return;
            }
            JTable table = this.getTabGui().getListaExercitos();
            rowIndex = lsm.getAnchorSelectionIndex();
            int modelIndex = table.convertRowIndexToModel(rowIndex);
            armySelected = (ArmySim) armiesList.get(modelIndex);
            getTabGui().updateArmy(armySelected);
            //set short casualties list
            getTabGui().setCasualtyBorder(armySelected, getTerrain());
            updateArmyCasualtyControler(armySelected, getTerrain());
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() instanceof JTable) {
            log.info(labels.getString("OPS.JTABLE.EVENT"));
        } else if (event.getSource() instanceof JButton) {
            doActionButton(event);
        } else if (event.getSource() instanceof JComboBox) {
            actionOnTabGui(event);
        } else {
            log.info(labels.getString("OPS.GENERAL.EVENT"));
        }
    }

    protected void doActionButton(ActionEvent event) {
        JButton jbTemp = (JButton) event.getSource();
        if (null == jbTemp.getActionCommand()) {
            log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
        } else //monta csv com as ordens
        {
            switch (jbTemp.getActionCommand()) {
                case "jbAbout":
                    doAbout();
                    break;
                case "jbTacticHelp":
                    doTacticHelp();
                    break;
                case "jbCopyDetails":
                    doCopyTableArmy();
                    break;
                case "jbCasualtiesList":
                    doLocalCasualties();
                    break;
                case "jbSimulation":
                    doSimulation();
                    break;
                case "jbNewArmy":
                    doNewArmy();
                    break;
                case "jbCloneArmy":
                    doCloneArmy(armySelected);
                    break;
                case "jbRemArmy":
                    doRemoveArmy(armySelected);
                    break;
                default:
                    log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
                    break;
            }
        }
    }

    private void actionOnTabGui(ActionEvent event) {
        JComboBox jcbActive = (JComboBox) event.getSource();
        if (null == jcbActive.getActionCommand()) {
            log.info(String.format("actionOnTabGui %s %s", jcbActive.getActionCommand(), jcbActive.getName()));
        } else {
            switch (jcbActive.getActionCommand()) {
                case "jcbTerrain":
                    try {
                        final GenericoComboObject obj = (GenericoComboObject) jcbActive.getModel().getSelectedItem();
                        final Terreno terrain = (Terreno) obj.getObject();
                        for (ArmySim army : armiesList) {
                            army.setTerreno(terrain);
                        }
                        doRefreshArmies();
                        this.doChangeTerrain(terrain);
                        getTabGui().setCasualtyBorder(armySelected, terrain);
                        //set short casualties list
                        updateArmyCasualtyControler(armySelected, getTerrain());
                    } catch (NullPointerException ex) {
                    }
                    break;
                case "cbTactic":
                    final GenericoComboObject tactic = (GenericoComboObject) jcbActive.getModel().getSelectedItem();
                    if (armySelected != null) {
                        armySelected.setTatica(ConverterFactory.taticaToInt(tactic.getComboId()));
                    }
                    doRefreshArmies();
                    updateArmyCasualtyControler(armySelected, getTerrain());
                    break;
                case "cbNation":
                    final GenericoComboObject nation = (GenericoComboObject) jcbActive.getModel().getSelectedItem();
                    if (armySelected != null) {
                        armySelected.setNacao((Nacao) nation.getObject());
                    }
                    doRefreshArmies();
                    updateArmyCasualtyControler(armySelected, getTerrain());
                    break;
                case "comboFiltro":
                    updateArmyCasualtyControler(armySelected, getTerrain());
                    break;
                default:
                    log.info(String.format("actionOnTabGui %s %s", jcbActive.getActionCommand(), jcbActive.getName()));
                    break;
            }
        }
    }

    private void doAbout() {
        //Launch popup window with BatleSim disclaimer
        WindowPopupText.showWindowText(labels.getString("BATTLESIM.DISCLAIMER.TEXT"), labels.getString("BATTLESIM.DISCLAIMER.TITLE"), this.getTabGui());
    }

    private void doTacticHelp() {
        //launch tactics bonuses in a new window
        WindowPopupText.showWindowTable(CenarioConverter.getInstance().getTaticaTableModel(), labels.getString("BATTLESIM.TATICA.HINT"), this.getTabGui());

    }

    private void doLocalCasualties() {
        //TODO: implement this with Terreno, not Local
        TroopsCasualtiesList casualtiesSim = new TroopsCasualtiesList(getTerrain());
        casualtiesSim.setLocationRelativeTo(this.getTabGui());
        casualtiesSim.setVisible(true);
    }

    private void doSimulation() {
        //put results in a new window.
        //clone the army so that we can run multiple simulations without changing the BattleSim

        //FIXME: to start, all armies attack the city.
        doCombatCity();
    }

    private void doCombatCity() {
        //FIXME: add Cidade from GUI (as opposed to labels) and Controler

        final Cidade city = getCity();
        //calc defense
        long defesa = cidadeFacade.getDefesa(getCity());
        //calcula forca e constituicao das tropas, sem tatica
        int ataqueTotal = 0;
        long qtTrops = 0;
        for (ArmySim army : armiesList) {
            //add the total amount of troops to be used later
            qtTrops += exercitoFacade.getQtTropasTotal(army);
            //calcula os fatores da media ponderada.
            int forcaBasica = bsf.getArmyAttackBaseLand(army, army.getLocal());
            int forcaPlus = 0;
            int modRelacionamento = 100 - nacaoFacade.getBonusRelacionamento(city.getNacao(), army.getNacao());
            //aqui entram os bonus da nacao por terreno/tropa
            final int dano = forcaPlus + (forcaBasica * modRelacionamento / 100);
            ataqueTotal += dano;
            //City round %s: %s inflicted %s of damage to %s with a defense of %s.
//            final String msgDano = SysMsgs.CombateFezDanoCidadeAtaque + SysMsgs.Separador + rounds + SysMsgs.Separador
//                    + army.displayComandante() + SysMsgs.Separador + dano + SysMsgs.Separador
//                    + city.displayNomeHex() + SysMsgs.Separador + defesa;
        }
        //apply damage

        //distribui a defesa do cp como dano aos atacantes
        for (ArmySim army : armiesList) {
            long danoPer = defesa * exercitoFacade.getQtTropasTotal(army) / qtTrops;

            //City round %s: %s with an attack of %s inflicted %s of damage to %s with a defense of %s.
            //how to apply damage from PbmCommons?
//            army.sumCombateDano(danoPer);
//            List<String> msgDanoT = army.doCombateDano();
            List<String> msgDanoT = new ArrayList<>();
            try {
                boolean first = true;
                for (String item : msgDanoT) {
                    if (first) {
//                        msg += "\t\t" + SysMsgs.CombateCasualtiesLabel + SysMsgs.Separador + rounds + "\n";
                        first = false;
                    }
//                    msg += "\t\t" + item + "\n";
                }
            } catch (NullPointerException ex) {
                //just skip, no messages
            }
        }
        System.out.println(defesa + "/" + ataqueTotal);

        //check results
        if (ataqueTotal <= defesa) {
            //ataque falhou.
        } else {
            //capturou
        }
    }

    private void doCopyTableArmy() {
        //copy para o clipboard
        SysApoio.setClipboardContents(listArmies());
        //update status bar
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.STATUS_BAR_MSG, labels.getString("COPIAR.ARMY.DETAILS"));
    }

    private String listArmies() {
        String ret = "";
        if (SettingsManager.getInstance().isConfig("BattleSimArmyCopyLabels", "1", "1")) {
            for (String details : listArmyDetailLabels()) {
                ret += String.format("%s\t", details);
            }
            ret += "\n";
            ret += "\t";
            for (String details : listPlatoonDetailLabels()) {
                ret += String.format("%s\t", details);
            }
            ret += "\n";
            ret += "\n";
        }
        for (ArmySim objArmy : armiesList) {
            //get army info
            ArmySim army = (ArmySim) objArmy;
            ret += listArmyDetails(army);
            ret += listPlatoonDetails(army);
            ret += "\n";
            ret += "\n";
        }
        return ret;
    }

    private String listArmyDetails(ArmySim army) {
        String ret = "";
        for (String details : armyToArray(army)) {
            ret += String.format("%s\t", details);
        }
        ret += "\n";
        return ret;
    }

    private String listPlatoonDetails(ArmySim army) {
        String ret = "";
        for (Pelotao platoon : army.getPelotoes().values()) {
            //for each platoon, get list of information
            ret += "\t";
            for (String details : platoonToArray(platoon)) {
                ret += String.format("%s\t", details);
            }
            ret += "\n";
        }
        ret += "\n";
        return ret;
    }

    private List<String> platoonToArray(Pelotao platoon) {
        List<String> platoonDetail = new ArrayList<>();
        platoonDetail.add(platoon.getTipoTropa().getNome());
        platoonDetail.add(platoon.getQtd() + "");
        platoonDetail.add(platoon.getTreino() + "");
        platoonDetail.add(platoon.getModAtaque() + "");
        platoonDetail.add(platoon.getModDefesa() + "");
        return platoonDetail;
    }

    private List<String> listPlatoonDetailLabels() {
        List<String> armiesLabels = new ArrayList<>();
        armiesLabels.add("Troop Type");
        armiesLabels.add("# of Soldiers");
        armiesLabels.add("Training");
        armiesLabels.add("Weapon");
        armiesLabels.add("Armor");
        return armiesLabels;
    }

    private List<String> armyToArray(ArmySim army) {
        List<String> armyDetail = new ArrayList<>();
        armyDetail.add(exercitoFacade.getComandanteTitulo(army, WorldFacadeCounselor.getInstance().getCenario()));
        armyDetail.add(army.getComandantePericia() + "");
        armyDetail.add(exercitoFacade.getMoral(army) + "");
        armyDetail.add(exercitoFacade.getAtaqueExercito(army, true) + "");
        armyDetail.add(exercitoFacade.getDefesaExercito(army, true) + "");
        armyDetail.add(exercitoFacade.getAtaqueExercito(army, false) + "");
        armyDetail.add(exercitoFacade.getDefesaExercito(army, false) + "");
        armyDetail.add(exercitoFacade.getTerreno(army));
        return armyDetail;
    }

    private List<String> listArmyDetailLabels() {
        List<String> armiesLabels = new ArrayList<>();
        armiesLabels.add("Name");
        armiesLabels.add("Commander rank");
        armiesLabels.add("Moral");
        armiesLabels.add("Land attack");
        armiesLabels.add("Land defense");
        armiesLabels.add("Navy attack");
        armiesLabels.add("Navy defense");
        armiesLabels.add("Terrain");
        return armiesLabels;
    }
}
