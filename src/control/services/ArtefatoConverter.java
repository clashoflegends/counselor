/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import business.facade.ArtefatoFacade;
import business.facade.PersonagemFacade;
import business.facades.ListFactory;
import business.facades.WorldFacadeCounselor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.ComboBoxModel;
import model.Artefato;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Personagem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import utils.StringRet;

/**
 *
 * @author Gurgel
 */
public class ArtefatoConverter implements Serializable {

    public static final int FILTRO_PROPRIOS = 1;
    public static final int FILTRO_TODOS = 0;
    private static final Log log = LogFactory.getLog(ArtefatoConverter.class);
    private static final ArtefatoFacade artefatoFacade = new ArtefatoFacade();
    private static final ListFactory listFactory = new ListFactory();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();

    public static ComboBoxModel getArtefatoComboModel(int tipo, Personagem personagem) {
        //cria lista completa
        List todos = new ArrayList(200);
        if (tipo == 0) {
            //adiciona um blank
            todos.add(getArtefatoBlank());
            //é um combo com TODOS os artefatos disponiveis
            todos.addAll(listFactory.listArtefatos());
        } else if (tipo == 1) {
            //adiciona um blank
            todos.add(getArtefatoBlank());
            //é um combo com os artefatos que o personagem carrega
            PersonagemFacade personagemFacade = new PersonagemFacade();
            Collection artefatosPersonagem = personagemFacade.getArtefatos(personagem);
            todos.addAll(artefatosPersonagem);
        } else if (tipo == 2) {
            //adiciona um blank
            todos.add(getArtefatoBlank());
            //é um combo com os artefatos que a nacao possui
            //lista os artefatos da nacao
            //nao basta percorrer os personagens, pois podem haver artefatos caidos.
            //TODO: colocar uma lsita de artefatos na nacao?
            PersonagemFacade personagemFacade = new PersonagemFacade();
            Collection<Artefato> artefatosNacao = artefatoFacade.getArtefatos(listFactory.listArtefatos(), personagemFacade.getNacao(personagem));
            todos.addAll(artefatosNacao);
        } else if (tipo == 3) {
            todos.add(getArtefatoBlank());
            todos.addAll(listFactory.listArtefatos());
            //remove os artefatos da nacao
            PersonagemFacade personagemFacade = new PersonagemFacade();
            Collection<Artefato> artefatosNacao = artefatoFacade.getArtefatos(listFactory.listArtefatos(), personagemFacade.getNacao(personagem));
            todos.removeAll(artefatosNacao);
        } else if (tipo == 4) {
            //é um combo com os artefatos de Scry que o personagem carrega
            PersonagemFacade personagemFacade = new PersonagemFacade();
            for (Artefato artefato : personagemFacade.getArtefatos(personagem)) {
                if (artefato.isExploracao()) {
                    todos.add(artefato);
                }
            }
        } else if (tipo == 5) {
            //é um combo com os artefatos de Summon que o personagem carrega
            PersonagemFacade personagemFacade = new PersonagemFacade();
            for (Artefato artefato : personagemFacade.getArtefatos(personagem)) {
                if (artefato.isSummon()) {
                    todos.add(artefato);
                }
            }
        } else if (tipo == 6) {
            //é um combo com os artefatos de DragonEgg que o personagem carrega
            PersonagemFacade personagemFacade = new PersonagemFacade();
            for (Artefato artefato : personagemFacade.getArtefatos(personagem)) {
                if (artefato.isDragonEgg()) {
                    todos.add(artefato);
                }
            }
        }
        //cria a combo e retorna a lista
        GenericoComboBoxModel model = new GenericoComboBoxModel((Artefato[]) todos.toArray(new Artefato[0]));
        return model;
    }

    private static Artefato getArtefatoBlank() {
        //adiciona um artefato dummy para servir como "any" nas combos.
        Artefato blank = new Artefato();
        blank.setNome("");
        blank.setCodigo("0");
        return blank;
    }

    public static GenericoTableModel getArtefatoModel(List lista) {
        GenericoTableModel artefatoModel
                = new GenericoTableModel(getArtefatoColNames(), getArtefatosAsArray(lista),
                        new Class[]{
                            java.lang.String.class, java.lang.String.class, java.lang.Integer.class,
                            java.lang.String.class, Local.class, java.lang.String.class,
                            java.lang.String.class, java.lang.String.class, java.lang.String.class,
                            java.lang.Integer.class
                        });
        return artefatoModel;
    }

    private static String[] getArtefatoColNames() {
        String[] colNames = {labels.getString("NOME"), labels.getString("ARTEFATO.PODER"), labels.getString("VALOR"),
            labels.getString("ALINHAMENTO"), labels.getString("LOCAL"), labels.getString("PERSONAGEM"),
            labels.getString("NACAO"), labels.getString("LATENTE"), labels.getString("TIPO"), labels.getString("PODER")
        };
        return (colNames);
    }

    private static Object[][] getArtefatosAsArray(List listaExibir) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", "", "", "", "", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getArtefatoColNames().length];
            Iterator lista = listaExibir.iterator();
            while (lista.hasNext()) {
                Artefato artefato = (Artefato) lista.next();
                // Converte um Artefato para um Array[] 
                ret[ii++] = ArtefatoConverter.toArray(artefato);
            }
            return (ret);
        }
    }

    private static Object[] toArray(Artefato artefato) {
        int ii = 0;
        Object[] cArray = new Object[getArtefatoColNames().length];
        cArray[ii++] = artefatoFacade.getNome(artefato);
        cArray[ii++] = artefatoFacade.getPrimario(artefato);
        cArray[ii++] = artefatoFacade.getValor(artefato);
        cArray[ii++] = artefatoFacade.getAlinhamento(artefato);
        cArray[ii++] = artefatoFacade.getLocalCoordenadas(artefato);
        cArray[ii++] = artefatoFacade.getOwnerNome(artefato);
        cArray[ii++] = artefatoFacade.getOwnerNacaoNome(artefato);
        cArray[ii++] = artefatoFacade.getLatente(artefato);
        cArray[ii++] = artefatoFacade.getDescricao(artefato);
        cArray[ii++] = artefatoFacade.getValorPoder(artefato);
        return cArray;
    }

    public static List listaByNacao(Nacao filtro) {
        List<Artefato> ret = new ArrayList();
        if (filtro == null) {
            ret.addAll(listFactory.listArtefatos());
        } else {
            ret.addAll(artefatoFacade.getArtefatos(listFactory.listArtefatos(), filtro));
        }
        return ret;
    }

    public static List listaByFiltro(String filtro) {
        List<Artefato> ret = new ArrayList();
        if (filtro.equalsIgnoreCase("all")) {
            ret.addAll(listFactory.listArtefatos());
        } else if (filtro.equalsIgnoreCase("own")) {
            Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
            for (Nacao nacao : jAtivo.getNacoes().values()) {
                ret.addAll(artefatoFacade.getArtefatos(listFactory.listArtefatos(), nacao));
            }
        } else if (filtro.equalsIgnoreCase("itemlost")) {
            for (Artefato item : listFactory.listArtefatos()) {
                if (!artefatoFacade.isPosse(item)) {
                    ret.add(item);
                }
            }
        } else if (filtro.equalsIgnoreCase("team")) {
            Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
            for (Nacao nacao : listFactory.listNacoes().values()) {
                if (jAtivo.isJogadorAliado(nacao) || jAtivo.isNacao(nacao)) {
                    ret.addAll(artefatoFacade.getArtefatos(listFactory.listArtefatos(), nacao));
                }
            }
        } else if (filtro.equalsIgnoreCase("allies")) {
            Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
            for (Nacao nacao : listFactory.listNacoes().values()) {
                if (jAtivo.isJogadorAliado(nacao) && !jAtivo.isNacao(nacao)) {
                    ret.addAll(artefatoFacade.getArtefatos(listFactory.listArtefatos(), nacao));
                }
            }
        } else if (filtro.equalsIgnoreCase("enemies")) {
            Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
            for (Nacao nacao : listFactory.listNacoes().values()) {
                if (!jAtivo.isJogadorAliado(nacao) && !jAtivo.isNacao(nacao)) {
                    ret.addAll(artefatoFacade.getArtefatos(listFactory.listArtefatos(), nacao));
                }
            }
        }
        return ret;
    }

    public static List<String> getInfo(Artefato artefato) {
        StringRet ret = new StringRet();
        ret.addTab(artefatoFacade.getNome(artefato));
        return ret.getList();
    }
}
