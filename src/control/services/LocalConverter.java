/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import business.converter.ConverterFactory;
import business.facade.LocalFacade;
import business.services.ComparatorFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import model.Artefato;
import model.Exercito;
import model.Habilidade;
import model.Local;
import model.Personagem;
import msgs.BaseMsgs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import utils.StringRet;

/**
 *
 * @author jmoura
 */
public class LocalConverter implements Serializable {

    private static final Log log = LogFactory.getLog(LocalConverter.class);
    private static final LocalFacade localFacade = new LocalFacade();
//    private static final ListFactory listFactory = new ListFactory();
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
        //old landmark print
//        if (localFacade.isTerrainLandmark(local)) {
//            ret.add(labels.getString("LANDMARK.LOCAL") + ";");
//            for (Habilidade feature : localFacade.getTerrainLandmark(local)) {
//                ret.addTab(ConverterFactory.getLandmarkName(feature.getCodigo()));
//            }
//        }
        //landmarks and others
        for (Habilidade hab : local.getHabilidades().values()) {
            if (hab.getCodigo().equals(";-;")) {
                continue;
            }
            //ret.add(labels.getString("LANDMARK.LOCAL") + " " + ConverterFactory.getLandmarkName(hab.getCodigo()));
            ret.add(hab.getNome());
        }
        //personagens
        if (local.getPersonagens().values().size() > 0) {
            ret.add(labels.getString("PERSONAGENS.LOCAL"));
            if (SettingsManager.getInstance().getConfig("HexInfoPcSorting", "N").equals("N")) {
                //sort by nation
                final List<Personagem> personagens = new ArrayList<Personagem>(local.getPersonagens().values());
                ComparatorFactory.getComparatorNationSorter(personagens);

                for (Personagem personagem : personagens) {
                    ret.add(PersonagemConverter.getInfo(personagem));
                }
            } else {
                //sort alphabetcaly
                for (Personagem personagem : local.getPersonagens().values()) {
                    ret.add(PersonagemConverter.getInfo(personagem));
                }
            }
        }
        //exercitos
        if (local.getExercitos().values().size() > 0) {
            ret.add(labels.getString("EXERCITOS"));
            for (Exercito exercito : local.getExercitos().values()) {
                ret.add(ExercitoConverter.getInfo(exercito));
            }
        }
        //artefatos
        if (local.getArtefatos().values().size() > 0) {
            ret.add(labels.getString("ARTEFATOS"));
            for (Artefato artefato : local.getArtefatos().values()) {
                ret.add(ArtefatoConverter.getInfo(artefato));
            }
        }
        return ret.getText();
    }
}
