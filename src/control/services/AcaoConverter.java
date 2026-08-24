/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import business.facade.AcaoFacade;
import persistence.local.ListFactory;
import business.services.ComparatorFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import model.Ordem;
import business.converter.TitleFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author Gurgel
 */
public class AcaoConverter implements Serializable {

    private static final Log log = LogFactory.getLog(AcaoConverter.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final AcaoFacade acaoFacade = new AcaoFacade();
    private static final ListFactory listFactory = new ListFactory();

    public static GenericoComboBoxModel getAcaoComboModel() {
        Collection lista = listFactory.listAcoes().values();
        Ordem[] items = (Ordem[]) lista.toArray(new Ordem[0]);
        GenericoComboBoxModel model = new GenericoComboBoxModel(items);
        return model;
    }

    public static GenericoTableModel getAcaoModel(List<Ordem> lista) {
        GenericoTableModel feiticoModel = new GenericoTableModel(
                getAcaoColNames(),
                getAcaosAsArray(lista),
                new Class[]{
                    java.lang.Integer.class, java.lang.String.class,
                    java.lang.String.class,
                    java.lang.String.class, java.lang.String.class,
                    java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.String.class, java.lang.String.class
                });
        return feiticoModel;
    }

    private static String[] getAcaoColNames() {
        String[] colNames = {labels.getString("SEQUENCIA"), labels.getString("ACAO"),
            labels.getString("PERSONAGEM"),
            labels.getString("TIPO"), labels.getString("DIFICULDADE"),
            labels.getString("CUSTO"), labels.getString("STARTUP.POINTS"),
            labels.getString("IMPROVE"), labels.getString("PARAMETRO")
        };
        return (colNames);
    }

    private static Object[][] getAcaosAsArray(List<Ordem> listaExibir) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getAcaoColNames().length];
            for (Ordem ordem : listaExibir) {
                int nn = 0;
                ret[ii][nn++] = ii + 1;
                ret[ii][nn++] = ordem.getDescricao();
                ret[ii][nn++] = TitleFactory.getTipoPersonagem(ordem);
                ret[ii][nn++] = TitleFactory.getTipoOrdem(ordem);
                ret[ii][nn++] = TitleFactory.getDificuldade(ordem);
                ret[ii][nn++] = acaoFacade.getCusto(ordem);
                ret[ii][nn++] = acaoFacade.getPointsSetup(ordem);
                ret[ii][nn++] = acaoFacade.getImproveRank(ordem);
                ret[ii][nn++] = ordem.getParametros();
                ii++;
            }
            return (ret);
        }
    }

    public static String getAjuda(Ordem ordem) {
        return TitleFactory.getOrdemDisplay(ordem);
    }

    public static List<Ordem> listaByFiltro(String filtro) {
        List<Ordem> ret = new ArrayList<>();
        for (Ordem ordem : listFactory.listAcoes().values()) {
            if (filtro.equalsIgnoreCase("All")
                    || TitleFactory.getTipoPersonagem(ordem).equals(filtro)) {
                ret.add(ordem);
            }
        }
        //ordena pelo numero
        ComparatorFactory.getComparatorOrdemSorter(ret);
        return ret;
    }
}
