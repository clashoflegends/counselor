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
import business.facade.NacaoFacade;
import control.facade.WorldFacadeCounselor;
import control.services.AcaoConverter;
import control.services.CenarioConverter;
import control.services.ExercitoConverter;
import control.services.FiltroConverter;
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
import model.Terreno;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class BattleSimulatorControlerNew implements Serializable, ChangeListener, ListSelectionListener, ActionListener {

    private static final Log log = LogFactory.getLog(BattleSimulatorControlerNew.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final BattleCasualtySimulatorNew tabGui;
    private final List<ArmySim> armiesList = new ArrayList<ArmySim>();
    private ArmySim armySelected;
    private Terreno terreno;
    private Cidade cityClone;
    private int rowIndex = 0;
    private BattleSimPlatoonCasualtyControlerNew casualtyControler;
    private final BattleSimFacade bsf = new BattleSimFacade();
    private final NacaoFacade nacaoFacade = new NacaoFacade();
    private final CidadeFacade cidadeFacade = new CidadeFacade();
    private final ExercitoFacade exercitoFacade = new ExercitoFacade();

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
        List<IBaseModel> lista = new ArrayList<IBaseModel>(WorldFacadeCounselor.getInstance().getNacoes().values());
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
        List<IBaseModel> lista = new ArrayList<IBaseModel>(WorldFacadeCounselor.getInstance().getCenario().getTerrenos().values());
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
                if ("jsArmyCommander".equals(source.getName())) {
                    armySelected.setComandante((Integer) source.getValue());
                } else if ("jsArmyMorale".equals(source.getName())) {
                    armySelected.setMoral((Integer) source.getValue());
                } else if ("jsArmyAbonus".equals(source.getName())) {
                    armySelected.setBonusAttack((Integer) source.getValue());
                } else if ("jsArmyDbonus".equals(source.getName())) {
                    armySelected.setBonusDefense((Integer) source.getValue());
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
        //monta csv com as ordens
        if ("jbAbout".equals(jbTemp.getActionCommand())) {
            doAbout();
        } else if ("jbTacticHelp".equals(jbTemp.getActionCommand())) {
            doTacticHelp();
        } else if ("jbCasualtiesList".equals(jbTemp.getActionCommand())) {
            doLocalCasualties();
        } else if ("jbSimulation".equals(jbTemp.getActionCommand())) {
            doSimulation();
        } else if ("jbNewArmy".equals(jbTemp.getActionCommand())) {
            doNewArmy();
        } else if ("jbCloneArmy".equals(jbTemp.getActionCommand())) {
            doCloneArmy(armySelected);
        } else if ("jbRemArmy".equals(jbTemp.getActionCommand())) {
            doRemoveArmy(armySelected);
        } else {
            log.info(labels.getString("NOT.IMPLEMENTED") + jbTemp.getActionCommand());
        }
    }

    private void actionOnTabGui(ActionEvent event) {
        JComboBox jcbActive = (JComboBox) event.getSource();
        if ("jcbTerrain".equals(jcbActive.getActionCommand())) {
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
        } else if ("cbTactic".equals(jcbActive.getActionCommand())) {
            final GenericoComboObject tactic = (GenericoComboObject) jcbActive.getModel().getSelectedItem();
            if (armySelected != null) {
                armySelected.setTatica(ConverterFactory.taticaToInt(tactic.getComboId()));
            }
            doRefreshArmies();
            updateArmyCasualtyControler(armySelected, getTerrain());
        } else if ("cbNation".equals(jcbActive.getActionCommand())) {
            final GenericoComboObject nation = (GenericoComboObject) jcbActive.getModel().getSelectedItem();
            if (armySelected != null) {
                armySelected.setNacao((Nacao) nation.getObject());
            }
            doRefreshArmies();
            updateArmyCasualtyControler(armySelected, getTerrain());
        } else if ("comboFiltro".equals(jcbActive.getActionCommand())) {
            updateArmyCasualtyControler(armySelected, getTerrain());
        } else {
            log.info(String.format("actionOnTabGui %s %s", jcbActive.getActionCommand(), jcbActive.getName()));
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
            List<String> msgDanoT = new ArrayList<String>();
            try {
                boolean first = true;
                for (String item : msgDanoT) {
                    if (first) {
//                        msg += "\t\t" + SysMsgs.CombateCasualitiesLabel + SysMsgs.Separador + rounds + "\n";
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

}
