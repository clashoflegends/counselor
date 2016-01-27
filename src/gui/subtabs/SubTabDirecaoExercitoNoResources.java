/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.subtabs;

import control.MapaControler;
import control.support.ActorInterface;
import model.Local;
import model.Ordem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.BundleManager;
import persistence.SettingsManager;

/**
 *
 * @author jmoura
 */
public class SubTabDirecaoExercitoNoResources extends SubTabDirecaoExercito {

    private static final Log log = LogFactory.getLog(SubTabDirecaoExercitoNoResources.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public SubTabDirecaoExercitoNoResources(String vlInicial, ActorInterface actorAtivo, MapaControler mapaControl, Ordem ordemSelecionada, boolean all) {
        super(vlInicial, actorAtivo, mapaControl, ordemSelecionada, all);
        setComidaSelected(false);
        doFieldsHide();
    }

    public SubTabDirecaoExercitoNoResources(Local local, int limitMov, boolean waterMov, MapaControler mapaControl) {
        super(local, limitMov, waterMov, mapaControl);
        setComidaSelected(false);
    }
}
