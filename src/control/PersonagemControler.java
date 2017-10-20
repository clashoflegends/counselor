/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import baseLib.GenericoComboObject;
import baseLib.GenericoTableModel;
import business.ImageManager;
import control.services.PersonagemConverter;
import control.support.ControlBase;
import control.support.DispatchManager;
import gui.tabs.TabPersonagensGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import model.Nacao;
import model.Personagem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class PersonagemControler extends ControlBase implements Serializable, ActionListener, ListSelectionListener {

    private static final Log log = LogFactory.getLog(PersonagemControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private GenericoTableModel mainTableModel;
    private final TabPersonagensGui tabGui;
    private List<Personagem> listaExibida;
    private Personagem personagem;
    private int modelRowIndex = 0;

    public PersonagemControler(TabPersonagensGui tabPersonagensGui) {
        this.tabGui = tabPersonagensGui;
        registerDispatchManagerForMsg(DispatchManager.ACTIONS_RELOAD);
        registerDispatchManagerForMsg(DispatchManager.SWITCH_PORTRAIT_PANEL);
    }

    @Override
    public void receiveDispatch(int msgName, String txt) {
        if (msgName == DispatchManager.ACTIONS_RELOAD) {
            getTabGui().doLoadChars();
        } else if (msgName == DispatchManager.SWITCH_PORTRAIT_PANEL) {
            boolean showPortrait = txt.equals("1");            
            tabGui.switchPortrait(showPortrait);
        }
    }

    public int getModelRowIndex() {
        return this.modelRowIndex;
    }

    public GenericoTableModel getMainTableModel(GenericoComboObject filtro) {
        Nacao nacao = null;
        try {
            nacao = (Nacao) filtro.getObject();
            listaExibida = PersonagemConverter.listaByNacao(nacao);
        } catch (ClassCastException e) {
            listaExibida = PersonagemConverter.listaByFiltro(filtro.getComboId());
        }
        this.mainTableModel = PersonagemConverter.getPersonagemModel(listaExibida);
        return this.mainTableModel;
    }

    public GenericoTableModel getArtefatoTableModel() {
        if (personagem == null) {
            return (null);
        } else {
            GenericoTableModel artefatoModel = PersonagemConverter.getArtefatoModel(personagem);
            return (artefatoModel);
        }
    }

    public GenericoTableModel getFeiticoTableModel() {
        if (personagem == null) {
            return (null);
        } else {
            GenericoTableModel feiticoModel = PersonagemConverter.getFeiticoModel(personagem);
            return (feiticoModel);
        }
    }

    public String getResultado() {
        return PersonagemConverter.getResultado(personagem);
    }

    public String getNome() {
        return personagem.getNome();
    }

    private TabPersonagensGui getTabGui() {
        return this.tabGui;
    }

    /**
     * Alguem acionou o botao filtro(JComboBox)
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() instanceof JTable) {
            log.info(labels.getString("OPS.JTABLE.EVENT"));
        } else if (actionEvent.getSource() instanceof JComboBox) {
            JComboBox cb = (JComboBox) actionEvent.getSource();
            if ("comboFiltro".equals(cb.getName())) {
                //refaz o modelo com a nova lista.
                getTabGui().setMainModel(getMainTableModel((GenericoComboObject) cb.getSelectedItem()));
            }
        } else {
            log.info(labels.getString("OPS.GENERAL.EVENT"));
        }
    }

    /**
     * listener para a tabela de personagens.
     *
     * @param event
     */
    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        try {
            ListSelectionModel lsm = (ListSelectionModel) event.getSource();
            if (lsm.isSelectionEmpty()) {
                this.personagem = null;
                getTabGui().doPersonagemClear();
            } else {
                int rowIndex = lsm.getAnchorSelectionIndex();
                JTable table = this.getTabGui().getMainLista();
                modelRowIndex = table.convertRowIndexToModel(rowIndex);
                this.personagem = (Personagem) listaExibida.get(modelRowIndex);
                getTabGui().doPersonagemMuda(this.personagem);
            }
        } catch (IndexOutOfBoundsException ex) {
            //lista vazia?
        }
    }
       
}
