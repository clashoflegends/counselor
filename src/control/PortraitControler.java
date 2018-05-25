/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import business.ImageManager;
import control.facade.WorldFacadeCounselor;
import control.support.ControlBase;
import control.support.DispatchManager;
import static java.awt.Component.CENTER_ALIGNMENT;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import model.Personagem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author gurgel
 */
public class PortraitControler extends ControlBase implements Serializable {

    private static final Log log = LogFactory.getLog(PortraitControler.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final JPanel portraitPanel;

    public PortraitControler(JPanel aPortraitPanel) {
        this.portraitPanel = aPortraitPanel;
        registerDispatchManagerForMsg(DispatchManager.SWITCH_PORTRAIT_PANEL);
        ImageManager.getInstance().doLoadPortraits();
    }

    @Override
    public void receiveDispatch(int msgName, String txt) {
        if (msgName == DispatchManager.SWITCH_PORTRAIT_PANEL) {
            boolean showPortrait = txt.equals("1");
            portraitPanel.setVisible(showPortrait);
        }
    }

    public void showPortrait(Personagem personagem) {
        String portraitFileName = personagem.getPortraitFilename();

        javax.swing.JLabel portrait = new javax.swing.JLabel(ImageManager.getInstance().getPortrait(portraitFileName));
        portrait.setBorder(new javax.swing.border.EmptyBorder(0, 5, 0, 0));
        if (portraitPanel.getComponentCount() > 0) {
            portraitPanel.removeAll();
            portraitPanel.repaint();
        }
        portraitPanel.setAlignmentX(CENTER_ALIGNMENT);
        portraitPanel.setLayout(new BoxLayout(portraitPanel, BoxLayout.Y_AXIS));
        portraitPanel.add(portrait);

        javax.swing.JLabel habilidad; // = null;

        Map<Integer, javax.swing.JLabel> rankMap = new TreeMap<Integer, javax.swing.JLabel>(Collections.reverseOrder());

        if (personagem.getPericiaComandanteNatural() > 0) {
            habilidad = getRankLabel("COMANDANTE", personagem.getPericiaComandanteNatural(), personagem.getPericiaComandante());
            rankMap.put(personagem.getPericiaComandanteNatural(), habilidad);
        }

        if (WorldFacadeCounselor.getInstance().hasRogue() || !WorldFacadeCounselor.getInstance().getCenario().isLom() && personagem.getPericiaAgenteNatural() > 0) {
            habilidad = getRankLabel("AGENTE", personagem.getPericiaAgenteNatural(), personagem.getPericiaAgente());
            if (rankMap.containsKey(personagem.getPericiaAgenteNatural())) {
                rankMap.put(personagem.getPericiaAgenteNatural() - 1, habilidad);
            } else {
                rankMap.put(personagem.getPericiaAgenteNatural(), habilidad);
            }
        }

        if (WorldFacadeCounselor.getInstance().hasDiplomat() && personagem.getPericiaEmissarioNatural() > 0) {
            habilidad = getRankLabel("EMISSARIO", personagem.getPericiaEmissarioNatural(), personagem.getPericiaEmissario());
            if (rankMap.containsKey(personagem.getPericiaEmissarioNatural())) {
                rankMap.put(personagem.getPericiaEmissarioNatural() - 2, habilidad);
            } else {
                rankMap.put(personagem.getPericiaEmissarioNatural(), habilidad);
            }
        }

        if (WorldFacadeCounselor.getInstance().hasWizard() && personagem.getPericiaMagoNatural() > 0) {
            habilidad = getRankLabel("MAGO", personagem.getPericiaMagoNatural(), personagem.getPericiaMago());
            if (rankMap.containsKey(personagem.getPericiaMagoNatural())) {
                rankMap.put(personagem.getPericiaMagoNatural() - 3, habilidad);
            } else {
                rankMap.put(personagem.getPericiaMagoNatural(), habilidad);
            }
        }
        Iterator<Map.Entry<Integer, javax.swing.JLabel>> iterator = rankMap.entrySet().iterator();
        Map.Entry<Integer, javax.swing.JLabel> entrada;

        while (iterator.hasNext()) {
            entrada = iterator.next();
            portraitPanel.add(entrada.getValue());
        }

        if (personagem.getPericiaFurtividadeNatural() > 0) {
            habilidad = getRankLabel("FURTIVIDADE", personagem.getPericiaFurtividadeNatural(), personagem.getPericiaFurtividade());
            portraitPanel.add(habilidad);
        }

        final int dueloNat = personagem.getDuelo() - personagem.getDueloBonus();
        if (dueloNat > 0) {
            habilidad = getRankLabel("DUELO", dueloNat, personagem.getDuelo());
            portraitPanel.add(habilidad);
        }

        if (personagem.getVida() > 0) {
            habilidad = getRankLabel("VITALIDADE", personagem.getVida(), personagem.getVida());
            portraitPanel.add(habilidad);
        }

        portraitPanel.repaint();
        portraitPanel.revalidate();
    }

    private JLabel getRankLabel(String rankName, int periciaNatural, int pericia) {
        javax.swing.JLabel habilidadLabel = null;
        String labelText;
        if (periciaNatural > 0) {
            labelText = labels.getString(rankName) + ": " + periciaNatural;
            if (periciaNatural != pericia) {
                labelText = labelText.concat(" (" + pericia + ")");

            }
            habilidadLabel = new javax.swing.JLabel(labelText);
            habilidadLabel.setBorder(new javax.swing.border.EmptyBorder(5, 5, 0, 0)); //top,left,bottom,right
        }

        return habilidadLabel;
    }
}
