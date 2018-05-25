/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import business.facade.FeiticoFacade;
import business.facade.PersonagemFacade;
import persistence.local.ListFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import model.Feitico;
import model.Personagem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author Gurgel
 */
public class FeiticoConverter implements Serializable {

    private static final Log log = LogFactory.getLog(FeiticoConverter.class);
    private static final FeiticoFacade feiticoFacade = new FeiticoFacade();
    private static final ListFactory listFactory = new ListFactory();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public static GenericoComboBoxModel getFeiticoComboModel() {
        Feitico[] items = (Feitico[]) listFactory.listFeiticos().toArray();
        GenericoComboBoxModel model = new GenericoComboBoxModel(items);
        return model;
    }

    /**
     * Feiticos que o personagem nao tem (ordem 700)
     * filtro = 0, todas as magias
     * filtro = 1, todas que ele nao tem
     * filtro = 2, todas que ele nao tem, mas pode ter pq tem o pre-requisito para aprender. incluindo NSP
     * @param filtro, personagem
     * @return
     */
    public static GenericoComboBoxModel getFeiticoComboModel(int filtro, Personagem personagem) {
        PersonagemFacade personagemFacade = new PersonagemFacade();
        List<Feitico> lista = listFactory.listFeiticos();
        List<Feitico> itens = new ArrayList<Feitico>(lista.size());
        if (filtro == 1) {
            //lista todas que ele nao tem
            for (Feitico feitico : lista) {
                if (!personagemFacade.isPersonagemHasFeitico(personagem, feitico)) {
                    itens.add(feitico);
                }
            }
        } else if (filtro == 2) {
            //lista todas que ele nao tem mas pode aprender + NSP
            for (Feitico feitico : lista) {
                //personagem nao conhece o feitico, se conhece, nao adiciona
                if (!personagemFacade.isPersonagemHasFeitico(personagem, feitico)) {
                    //lista os feiticos conhecidos do mesmo tomo:
                    final Feitico[] listFeiticosTomo = personagemFacade.listFeiticos(personagem, feitico.getLivroFeitico());
                    if (personagemFacade.hasFeiticoRequisito(personagem, feitico, listFeiticosTomo)) {
                        //se tiver pre-req, adiciona a lista.
                        itens.add(feitico);
                    }
                }
            }
        } else {
            //lista todas
            itens.addAll(listFactory.listFeiticos());
        }
        GenericoComboBoxModel model = new GenericoComboBoxModel(itens.toArray(new Feitico[0]));
        return model;
    }

    public static GenericoTableModel getFeiticoModel(List<Feitico> lista) {
        GenericoTableModel feiticoModel = new GenericoTableModel(
                getFeiticoColNames(),
                getFeiticosAsArray(lista),
                new Class[]{
                    java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
                });
        return feiticoModel;
    }

    private static String[] getFeiticoColNames() {
        String[] colNames = {labels.getString("NOME"), labels.getString("TOMO"),
            labels.getString("DIFICULDADE"),
            labels.getString("PROIBIDO"), labels.getString("ACAO")
        };
        return (colNames);
    }

    private static Object[][] getFeiticosAsArray(List<Feitico> listaExibir) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getFeiticoColNames().length];
            for (Feitico feitico : listaExibir) {
                // Converte um Feitico para um Array[] 
                int nn = 0;
                ret[ii][nn++] = feiticoFacade.getNome(feitico);
                ret[ii][nn++] = feiticoFacade.getLivroFeitico(feitico);
                ret[ii][nn++] = feiticoFacade.getDificuldadeDisplay(feitico);
                ret[ii][nn++] = feiticoFacade.getProibidoDisplay(feitico);
                ret[ii][nn++] = feiticoFacade.getOrdemNome(feitico);
                ii++;
            }
            return (ret);
        }
    }

    public static GenericoComboBoxModel getLivroComboModel() {
        String[][] itens = listLivro();
        return new GenericoComboBoxModel(itens);
    }

    public static List<Feitico> listaByFiltro(String filtro) {
        List ret = new ArrayList();
        for (Feitico feitico : listFactory.listFeiticos()) {
            if (filtro.equalsIgnoreCase("Todos")
                    || feitico.getLivroFeitico().equalsIgnoreCase(filtro)) {
                ret.add(feitico);
            }
        }
        return ret;
    }

    private static String[][] listLivro() {
        SortedMap<String, String> itens = new TreeMap();
        for (Feitico feitico : listFactory.listFeiticos()) {
            itens.put(feitico.getLivroFeitico(), feitico.getLivroFeitico());
        }
        String[][] ret = new String[itens.size() + 1][2];
        int ii = 0;
        ret[ii][0] = labels.getString("ALL");
        ret[ii++][1] = "Todos";
        for (String livro : itens.keySet()) {
            ret[ii][0] = livro;
            ret[ii++][1] = livro;
        }
        return ret;
    }
}
