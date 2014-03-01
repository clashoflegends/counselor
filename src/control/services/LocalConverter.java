/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import business.facade.LocalFacade;
import business.facades.ListFactory;
import java.io.Serializable;
import model.Artefato;
import model.Exercito;
import model.Local;
import model.Personagem;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;
import utils.StringRet;

/**
 *
 * @author jmoura
 */
public class LocalConverter implements Serializable {

    private static final Log log = LogFactory.getLog(LocalConverter.class);
    private static final LocalFacade localFacade = new LocalFacade();
    private static final ListFactory listFactory = new ListFactory();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public static String getInfo(Local local) {
        StringRet ret = new StringRet();
        //cidade
        ret.add(CidadeConverter.getInfo(local.getCidade()));
        //local
        //"Local : @ 3103 em Planície O Clima é Polar"
        ret.add(String.format(labels.getString("TERRENO.CLIMA"),
                local.getTerreno().getNome(),
                BaseMsgs.localClima[local.getClima()]));
        //personagens
        if (local.getPersonagens().values().size() > 0) {
            ret.add(labels.getString("PERSONAGENS.LOCAL"));
        }
        for (Personagem personagem : local.getPersonagens().values()) {
            ret.add(PersonagemConverter.getInfo(personagem));
        }
        //exercitos
        if (local.getExercitos().values().size() > 0) {
            ret.add(labels.getString("EXERCITOS"));
        }
        for (Exercito exercito : local.getExercitos().values()) {
            ret.add(ExercitoConverter.getInfo(exercito));
        }
        //artefatos
        if (local.getArtefatos().values().size() > 0) {
            ret.add(labels.getString("ARTEFATOS"));
        }
        for (Artefato artefato : local.getArtefatos().values()) {
            ret.add(ArtefatoConverter.getInfo(artefato));
        }
        return ret.getText();
    }
}
