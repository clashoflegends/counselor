/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package radialMenu;

import business.converter.ConverterFactory;
import business.facade.LocalFacade;
import control.support.DispatchManager;
import java.util.SortedMap;
import model.Cidade;
import model.Local;
import model.Nacao;
import model.Terreno;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.local.WorldManager;

/**
 *
 * @author jmoura
 */
public class RadialEvents {

    private static final Log log = LogFactory.getLog(RadialEvents.class);
    private final LocalFacade lf = new LocalFacade();
    private SortedMap<String, Local> locais;

    private void sendChange(int msg, Local local) {
        local.setChanged(true);
        DispatchManager.getInstance().sendDispatchForMsg(msg, local);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW, local);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SAVE_WORLDBUILDER_FILE, local);
    }

    private void sendChange(Local local) {
        local.setChanged(true);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW, local);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SAVE_WORLDBUILDER_FILE, local);
    }

    private void sendChange(Local local, Local vizinho) {
        local.setChanged(true);
        vizinho.setChanged(true);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.SAVE_WORLDBUILDER_FILE, local);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW, local);
        DispatchManager.getInstance().sendDispatchForMsg(DispatchManager.LOCAL_MAP_REDRAW, vizinho);
    }

    /**
     * @return the locais
     */
    private SortedMap<String, Local> getLocais() {
        return locais;
    }

    /**
     * @param locais the locais to set
     */
    public void setLocais(SortedMap<String, Local> locais) {
        this.locais = locais;
    }

    private String removeDirection(String directions, int direction) {
        int posicao = directions.indexOf(direction + "");
        if (posicao != -1) {
            String newDirecoes = "";
            for (int ii = 0; ii < directions.length(); ii++) {
                if (ii != posicao) {
                    newDirecoes += directions.substring(ii, ii + 1);
                }
            }
            directions = newDirecoes;
        }
        return directions;
    }

    public void toogleRoad(Local local, String direction) {
        final int dir = ConverterFactory.direcaoToInt(direction);
        final int dirv = ConverterFactory.getDirecao(dir + 3);
        final Local localVizinho = lf.getLocalVizinho(local, direction, locais);
        if (lf.isEstrada(local, dir)) {
            //on then remove
            local.setEstrada(removeDirection(local.getEstrada(), dir));
            localVizinho.setEstrada(removeDirection(localVizinho.getEstrada(), dirv));
        } else {
            //off then build
            local.setEstrada(local.getEstrada().concat(dir + ""));
            localVizinho.setEstrada(localVizinho.getEstrada().concat(dirv + ""));
        }
        sendChange(local, localVizinho);
    }

    public void toogleRiver(Local local, String direction) {
        final int dir = ConverterFactory.direcaoToInt(direction);
        final int dirv = ConverterFactory.getDirecao(dir + 3);
        final Local localVizinho = lf.getLocalVizinho(local, direction, locais);
        if (lf.isRio(local, dir)) {
            //on then remove
            local.setRio(removeDirection(local.getRio(), dir));
            localVizinho.setRio(removeDirection(localVizinho.getRio(), dirv));
        } else {
            //off then build
            if (lf.isRiacho(local, dir)) {
                local.setRiacho(removeDirection(local.getRiacho(), dir));
                localVizinho.setRiacho(removeDirection(localVizinho.getRiacho(), dirv));
            }
            local.setRio(local.getRio().concat(dir + ""));
            localVizinho.setRio(localVizinho.getRio().concat(dirv + ""));
        }
        sendChange(local, localVizinho);
    }

    public void toogleStream(Local local, String direction) {
        final int dir = ConverterFactory.direcaoToInt(direction);
        final int dirv = ConverterFactory.getDirecao(dir + 3);
        final Local localVizinho = lf.getLocalVizinho(local, direction, locais);
        if (lf.isRiacho(local, dir)) {
            //on then remove
            local.setRiacho(removeDirection(local.getRiacho(), dir));
            localVizinho.setRiacho(removeDirection(localVizinho.getRiacho(), dirv));
        } else {
            //off then build
            if (lf.isRio(local, dir)) {
                local.setRio(removeDirection(local.getRio(), dir));
                localVizinho.setRio(removeDirection(localVizinho.getRio(), dirv));
            }
            local.setRiacho(local.getRiacho().concat(dir + ""));
            localVizinho.setRiacho(localVizinho.getRiacho().concat(dirv + ""));
        }
        sendChange(local, localVizinho);
    }

    public void toogleBridge(Local local, String direction) {
        final int dir = ConverterFactory.direcaoToInt(direction);
        final int dirv = ConverterFactory.getDirecao(dir + 3);
        final Local localVizinho = lf.getLocalVizinho(local, direction, locais);
        if (lf.isPonte(local, dir)) {
            //on then remove
            local.setPonte(removeDirection(local.getPonte(), dir));
            localVizinho.setPonte(removeDirection(localVizinho.getPonte(), dirv));
        } else {
            //off then build
            if (lf.isVau(local, dir)) {
                local.setVau(removeDirection(local.getVau(), dir));
                localVizinho.setVau(removeDirection(localVizinho.getVau(), dirv));
            }
            local.setPonte(local.getPonte().concat(dir + ""));
            localVizinho.setPonte(localVizinho.getPonte().concat(dirv + ""));
        }
        sendChange(local, localVizinho);
    }

    public void toogleLanding(Local local, String direction) {
        final int dir = ConverterFactory.direcaoToInt(direction);
//        final int dirv = ConverterFactory.getDirecao(dir + 3);
//        final Local localVizinho = lf.getLocalVizinho(local, direction, locais);
        if (lf.isLanding(local, dir)) {
            //on then remove
            local.setLanding(removeDirection(local.getLanding(), dir));
//            localVizinho.setLanding(removeDirection(localVizinho.getLanding(), dirv));
        } else {
            //off then build
            local.setLanding(local.getLanding().concat(dir + ""));
//            localVizinho.setLanding(localVizinho.getLanding().concat(dirv + ""));
        }
        sendChange(local);
    }

    public void toogleShalow(Local local, String direction) {
        final int dir = ConverterFactory.direcaoToInt(direction);
        final int dirv = ConverterFactory.getDirecao(dir + 3);
        final Local localVizinho = lf.getLocalVizinho(local, direction, locais);
        if (lf.isVau(local, dir)) {
            //on then remove
            local.setVau(removeDirection(local.getVau(), dir));
            localVizinho.setVau(removeDirection(localVizinho.getVau(), dirv));
        } else {
            //off then build
            if (lf.isPonte(local, dir)) {
                local.setPonte(removeDirection(local.getPonte(), dir));
                localVizinho.setPonte(removeDirection(localVizinho.getPonte(), dirv));
            }
            local.setVau(local.getVau().concat(dir + ""));
            localVizinho.setVau(localVizinho.getVau().concat(dirv + ""));
        }
        sendChange(local, localVizinho);
    }

    public void changeTerrain(Local local, Terreno terreno) {
        local.setTerreno(terreno);
        sendChange(local);
    }

    public void changeCityNation(Local local, Nacao nacao) {
        if (lf.isCidade(local)) {
            //change city's nation
            lf.getCidade(local).setNacao(nacao);
        } else {
            //create city
            Cidade cidade = new Cidade();
            cidade.setTamanho(2);
            cidade.setNacao(nacao);
            cidade.setLocal(local);
            cidade.setLealdade(50);
            cidade.setNome(nacao.getNome() + " at " + local.getCoordenadas());
            WorldManager.getInstance().addCidade(cidade);
        }
        sendChange(DispatchManager.LOCAL_CITY_REDRAW, local);
    }
}
