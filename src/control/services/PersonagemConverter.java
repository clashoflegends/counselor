/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoComboBoxModel;
import baseLib.GenericoTableModel;
import baseLib.IBaseModel;
import business.facade.ArtefatoFacade;
import business.facade.CenarioFacade;
import business.facade.FeiticoFacade;
import business.facade.LocalFacade;
import business.facade.OrdemFacade;
import business.facade.PersonagemFacade;
import business.facades.ListFactory;
import business.facades.WorldFacadeCounselor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import model.Artefato;
import model.Cenario;
import model.Feitico;
import model.Jogador;
import model.Local;
import model.Nacao;
import model.Ordem;
import model.Personagem;
import model.PersonagemFeitico;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import utils.StringRet;

/**
 *
 * @author Gurgel
 */
public class PersonagemConverter implements Serializable {

    public static final int ORDEM_COL_INDEX_START = 1;
//    private static final int FILTRO_PROPRIOS = 1;
//    private static final int FILTRO_TODOS = 0;
    private static final Log log = LogFactory.getLog(PersonagemConverter.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final PersonagemFacade personagemFacade = new PersonagemFacade();
    private static final LocalFacade localFacade = new LocalFacade();
    private static final OrdemFacade ordemFacade = new OrdemFacade();
    private static final CenarioFacade cenarioFacade = new CenarioFacade();
    private static final FeiticoFacade feiticoFacade = new FeiticoFacade();
    private static final ListFactory listFactory = new ListFactory();

    public static GenericoTableModel getPersonagemModel(List<Personagem> lista) {
        List<Class> classes = new ArrayList<Class>(30);
        GenericoTableModel model = new GenericoTableModel(
                getPersonagemColNames(classes),
                getPersonagemAsArray(lista),
                classes.toArray(new Class[0]));
        return model;
    }

    private static String[] getPersonagemColNames(List<Class> classes) {
        int qtOrdens = WorldFacadeCounselor.getInstance().getOrdensQtMax();
        List<String> colNames = new ArrayList<String>(30);
        colNames.add(labels.getString("NOME"));
        classes.add(java.lang.String.class);
        for (int nn = 1; nn <= qtOrdens; nn++) {
            colNames.add(String.format("%s %s", labels.getString("ACAO"), nn));
//            classes.add(String.class);
            classes.add(PersonagemOrdem.class);

        }
        colNames.add(labels.getString("LOCAL"));
        classes.add(Local.class);
        colNames.add(labels.getString("COMANDANTE"));
        classes.add(java.lang.Integer.class);
        if (WorldFacadeCounselor.getInstance().hasRogue() || !WorldFacadeCounselor.getInstance().getCenario().isLom()) {
            colNames.add(labels.getString("AGENTE"));
            classes.add(java.lang.Integer.class);
        }
        if (WorldFacadeCounselor.getInstance().hasDiplomat()) {
            colNames.add(labels.getString("EMISSARIO"));
            classes.add(java.lang.Integer.class);
        }
        if (WorldFacadeCounselor.getInstance().hasWizard()) {
            colNames.add(labels.getString("MAGO"));
            classes.add(java.lang.Integer.class);
        }
        colNames.add(labels.getString("FURTIVIDADE"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("DUELO"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("VITALIDADE"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("NACAO"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("CUSTO.MANUTENCAO"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("ARTEFATOS"));
        classes.add(java.lang.Integer.class);
        colNames.add(labels.getString("CLIMA"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("TERRENO"));
        classes.add(java.lang.String.class);
        colNames.add(labels.getString("OBS"));
        classes.add(java.lang.String.class);
        return (colNames.toArray(new String[0]));
    }

    private static Object[] personagemToArray(Personagem personagem) {
        int qtOrdens = WorldFacadeCounselor.getInstance().getOrdensQtMax();
        int ii = 0;
        String[] temp;
        Object[] cArray = new Object[getPersonagemColNames(new ArrayList<Class>(30)).length];
        Local local = personagemFacade.getLocal(personagem);
        Local localOrigem = personagemFacade.getLocalOrigem(personagem);
        //inicia array
        cArray[ii++] = personagemFacade.getNome(personagem);
        for (int nn = 0; nn < qtOrdens; nn++) {
            temp = ordemFacade.getOrdemDisplay(personagem, nn, WorldFacadeCounselor.getInstance().getCenario(), WorldFacadeCounselor.getInstance().getJogadorAtivo());
            cArray[ORDEM_COL_INDEX_START + nn] = temp[0] + temp[1];
            ii++;
        }
        cArray[ii++] = localFacade.getCoordenadas(local);
        cArray[ii++] = personagem.getPericiaComandante();
        if (WorldFacadeCounselor.getInstance().hasRogue() || !WorldFacadeCounselor.getInstance().getCenario().isLom()) {
            cArray[ii++] = personagem.getPericiaAgente();
        }
        if (WorldFacadeCounselor.getInstance().hasDiplomat()) {
            cArray[ii++] = personagem.getPericiaEmissario();
        }
        if (WorldFacadeCounselor.getInstance().hasWizard()) {
            cArray[ii++] = personagem.getPericiaMago();
        }
        cArray[ii++] = personagem.getPericiaFurtividade();
        cArray[ii++] = personagem.getDuelo();
        cArray[ii++] = personagem.getVida();
        cArray[ii++] = personagemFacade.getNacaoNome(personagem);
        cArray[ii++] = personagemFacade.getUpkeepMoney(personagem);
        cArray[ii++] = personagemFacade.getArtefatos(personagem).size();
        cArray[ii++] = localFacade.getClima(local);
        cArray[ii++] = localFacade.getTerrenoNome(local);
        if (isProprio(personagem)) {
            cArray[ii++] = labels.getString("PROPRIO");
        } else if (personagemFacade.isComandaExercito(personagem)) {
            cArray[ii++] = labels.getString("COMANDANTE");
        } else {
            cArray[ii++] = labels.getString("AVISTADO");
        }
        return cArray;
    }

    public static boolean isProprio(Personagem personagem) {
        boolean ret = false;
        try {
            Jogador jativo = WorldFacadeCounselor.getInstance().getPartida().getJogadorAtivo();
            Jogador jpersonagem = personagem.getNacao().getOwner();
            //log.info(jativo + "/" + jpersonagem);
            if (jativo == jpersonagem) {
                ret = true;
            }
        } catch (NullPointerException ex) {
            ret = false;
        }
        return ret;
    }

    private static Object[][] getPersonagemAsArray(List<Personagem> listaExibir) {
        if (listaExibir.isEmpty()) {
            Object[][] ret = {{"", "", "", "", "", ""}};
            return (ret);
        } else {
            int ii = 0;
            Object[][] ret = new Object[listaExibir.size()][getPersonagemColNames(new ArrayList<Class>(30)).length];
            for (Personagem personagem : listaExibir) {
                ret[ii++] = PersonagemConverter.personagemToArray(personagem);
            }
            return (ret);
        }
    }

    public static GenericoTableModel getArtefatoModel(Personagem personagem) {
        GenericoTableModel model = new GenericoTableModel(
                getArtefatoColNames(),
                getArtefatosAsArray(personagem),
                new Class[]{
                    java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
                });
        return model;
    }

    private static String[] getArtefatoColNames() {
        //PENDING: COlocar artefato em uso e se o personagem pode usar o artefato ou nao(disable color?)
        String[] colNames = {labels.getString("NOME"), labels.getString("PODER"), labels.getString("VALOR"),
            labels.getString("TIPO"), labels.getString("LATENTE")
        };
        return (colNames);
    }

    private static Object[][] getArtefatosAsArray(Personagem personagem) {
        int ii = 0;
        ArtefatoFacade artefatoFacade = new ArtefatoFacade();
        Collection<Artefato> lista = personagemFacade.getArtefatos(personagem);
        Object[][] ret = new Object[lista.size()][getArtefatoColNames().length];
        for (Artefato artefato : lista) {
            int i = 0;
            ret[ii][i++] = artefatoFacade.getNome(artefato);
            ret[ii][i++] = artefatoFacade.getPrimario(artefato);
            ret[ii][i++] = artefatoFacade.getValor(artefato);
            ret[ii][i++] = artefatoFacade.getDescricao(artefato);
            ret[ii][i++] = artefatoFacade.getLatente(artefato);
            ii++;
        }
        return (ret);
    }

    public static GenericoTableModel getOrdemModel(List<Personagem> listaExibida) {
        GenericoTableModel model = new GenericoTableModel(
                new String[]{labels.getString("PERSONAGEM"), labels.getString("ACAO"), labels.getString("PARAMETRO")},
                getOrdemAsArray(listaExibida),
                new Class[]{
                    java.lang.String.class, java.lang.String.class, java.lang.String.class
                });
        return model;
    }

    private static String[][] getOrdemAsArray(List listaExibida) {
        int ii = 0;
        String[][] ret = new String[listaExibida.size()][3];
        for (Iterator it = listaExibida.iterator(); it.hasNext();) {
            ret[ii++] = (String[]) it.next();
        }
        return (ret);
    }

    public static GenericoComboBoxModel getFeiticoComboModel(Ordem ordem, Personagem personagem) {
        IBaseModel[] items;
        //se personagem==null, entao lista todas as magias possiveis
        if (personagem == null) {
            //retorna uma lista de feiticos, filtrada pela ordem
            items = personagemFacade.listFeiticoByOrdem(ordem, WorldFacadeCounselor.getInstance().getCenario().getFeiticos());
        } else {
            //retorna uma lista de Personagem.PersonagemFeitico
            items = personagemFacade.listFeiticoByOrdem(ordem, personagem);
        }
        GenericoComboBoxModel model = new GenericoComboBoxModel(items);
        return model;
    }

    public static GenericoTableModel getFeiticoModel(Personagem personagem) {
        GenericoTableModel model = new GenericoTableModel(
                getFeiticoColNames(),
                getFeiticosAsArray(personagem),
                new Class[]{
                    java.lang.String.class, java.lang.Integer.class, java.lang.String.class,
                    java.lang.String.class, java.lang.String.class
                });
        return model;
    }

    private static String[] getFeiticoColNames() {
        String[] colNames = {
            labels.getString("FEITICO"),
            labels.getString("HABILIDADE"),
            labels.getString("ACAO"),
            labels.getString("TOMO"),
            labels.getString("DIFICULDADE")
        };
        return (colNames);
    }

    private static Object[][] getFeiticosAsArray(Personagem personagem) {
        int ii = 0;
        //<feitico.numero, {feitico, habilidade}>
        Iterator lista = personagem.getFeiticos().values().iterator();
        Object[][] ret = new Object[personagem.getFeiticos().size()][getFeiticoColNames().length];
        while (lista.hasNext()) {
            PersonagemFeitico magia = (PersonagemFeitico) lista.next();
            Feitico feitico = magia.getFeitico();
            Integer habilidade = magia.getHabilidade();
            int nn = 0;
            ret[ii][nn++] = feiticoFacade.getNome(feitico);
            ret[ii][nn++] = habilidade;
            ret[ii][nn++] = feiticoFacade.getOrdemNome(feitico);
            ret[ii][nn++] = feiticoFacade.getLivroFeitico(feitico);
            ret[ii][nn++] = feiticoFacade.getDificuldadeDisplay(feitico);
            ii++;
        }
//        if (ii == 0) {
//            ret = new Object[1][getFeiticoColNames().length];
//            int i = 0;
//            ret[ii][i++] = "-";
//            ret[ii][i++] = "-";
//            ret[ii][i++] = "-";
//            ret[ii][i++] = "-";
//            ret[ii][i++] = "-";
//            ii++;
//        }
        return (ret);
    }

    public static List<Personagem> listaByNacao(Nacao filtro) {
        List<Personagem> ret = new ArrayList();
        for (Personagem personagem : listFactory.listPersonagens()) {
            if (filtro == null) {
                //todos
                ret.add(personagem);
            } else if (filtro == personagem.getNacao()) {
                //da nacao indicada
                ret.add(personagem);
            }
        }
        return ret;
    }

    public static List<Personagem> listaByFiltro(String filtro) {
        List<Personagem> ret = new ArrayList();
        if (filtro.equalsIgnoreCase("all")) {
            ret.addAll(listFactory.listPersonagens());
        } else if (filtro.equalsIgnoreCase("own")) {
            Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (jAtivo.isNacao(personagem.getNacao())) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("capital")) {
            Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (jAtivo.isNacao(personagem.getNacao()) && personagemFacade.isInCapital(personagem)) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("army")) {
            Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (jAtivo.isNacao(personagem.getNacao()) && personagemFacade.isComandaExercito(personagem)) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("double")) {
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (personagemFacade.isDoubleAgent(personagem)) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("team")) {
            Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (jAtivo.isJogadorAliado(personagem.getNacao()) || jAtivo.isNacao(personagem.getNacao())) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("allies")) {
            Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (jAtivo.isJogadorAliado(personagem.getNacao()) && !jAtivo.isNacao(personagem.getNacao())) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("enemies")) {
            Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (!jAtivo.isJogadorAliado(personagem.getNacao()) && !jAtivo.isNacao(personagem.getNacao())) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("mypcinfo")) {
            Jogador jAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (!jAtivo.isJogadorAliado(personagem.getNacao()) && !jAtivo.isNacao(personagem.getNacao())) {
                        ret.add(personagem);
                    } else if (personagemFacade.isDoubleAgent(personagem)) {
                        ret.add(personagem);
                    } else if (jAtivo.isNacao(personagem.getNacao())) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("COMANDANTE")) {
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (personagem.isComandante()) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("AGENTE")) {
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (personagem.isAgente()) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("EMISSARIO")) {
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (personagem.isEmissario()) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        } else if (filtro.equalsIgnoreCase("MAGO")) {
            for (Personagem personagem : listFactory.listPersonagens()) {
                try {
                    if (personagem.isMago()) {
                        ret.add(personagem);
                    }
                } catch (NullPointerException e) {
                }
            }
        }
        return ret;
    }

    public static List listaOrdens() {
        List ret = new ArrayList();
        Jogador jogadorAtivo = WorldFacadeCounselor.getInstance().getJogadorAtivo();
        //lista todos os personagens, carregando para o xml
        for (Iterator<Personagem> iter = WorldFacadeCounselor.getInstance().getPersonagens(); iter.hasNext();) {
            Personagem personagem = iter.next();
            if (jogadorAtivo.isNacao(personagem.getNacao())) {
                for (int index = 0; index < personagem.getAcaoSize(); index++) {
                    String[] elem = {personagem.getNome(),
                        ordemFacade.getOrdem(personagem, index).getNome(),
                        ordemFacade.getParametrosId(personagem, index).
                        toString().replace('[', ' ').replace(']', ' ').trim()
                    };
                    ret.add(elem);
                }
            }
        }
        return ret;
    }

    /*
     * Níveis : Comandante 0 Agente 0 Emissário 65 Mago 0 Vitalidade 100
     * Furtividade 0 Duelo 32 Artefatos : Nenhum Feiticos (+0) : Nenhum
     * ________________________________________ A Musa se encontrava em 1919
     * (Litoral). A nação não recebeu nenhuma ordem. A Musa está em 1919
     * (Litoral), Aldeia de Rhodes da nação Persia.
     */
    public static String getResultado(Personagem personagem) {
        //Cenario cenario = WorldFacadeCounselor.getInstance().getCenario();
        return personagemFacade.getResultado(personagem) + getPericias(personagem);
    }

    public static String getPericias(Personagem personagem) {
        String mask1 = "   %s %d(%d)\n";
        String mask2 = "   %s %d\n";
        Cenario cenario = WorldFacadeCounselor.getInstance().getCenario();
        String ret = "";
        if (personagem.getPericiaAgenteNatural() > 0) {
            if (personagem.getPericiaAgente() != personagem.getPericiaAgenteNatural()) {
                ret += String.format(mask1,
                        cenarioFacade.getTituloPericia(cenario,
                                CenarioFacade.ROGUE,
                                personagem.getPericiaAgenteNatural()),
                        personagem.getPericiaAgente(),
                        personagem.getPericiaAgenteNatural());
            } else {
                ret += String.format(mask2,
                        cenarioFacade.getTituloPericia(cenario,
                                CenarioFacade.ROGUE,
                                personagem.getPericiaAgenteNatural()),
                        personagem.getPericiaAgenteNatural());
            }
        }
        if (personagem.getPericiaComandanteNatural() > 0) {
            if (personagem.getPericiaComandante() != personagem.getPericiaComandanteNatural()) {
                ret += String.format(mask1,
                        cenarioFacade.getTituloPericia(cenario,
                                CenarioFacade.COMANDANTE,
                                personagem.getPericiaComandanteNatural()),
                        personagem.getPericiaComandante(),
                        personagem.getPericiaComandanteNatural());
            } else {
                ret += String.format(mask2,
                        cenarioFacade.getTituloPericia(cenario,
                                CenarioFacade.COMANDANTE,
                                personagem.getPericiaComandanteNatural()),
                        personagem.getPericiaComandanteNatural());
            }
        }
        if (personagem.getPericiaEmissarioNatural() > 0) {
            if (personagem.getPericiaEmissario() != personagem.getPericiaEmissarioNatural()) {
                ret += String.format(mask1,
                        cenarioFacade.getTituloPericia(cenario,
                                CenarioFacade.DIPLOMAT,
                                personagem.getPericiaEmissarioNatural()),
                        personagem.getPericiaEmissario(),
                        personagem.getPericiaEmissarioNatural());
            } else {
                ret += String.format(mask2,
                        cenarioFacade.getTituloPericia(cenario,
                                CenarioFacade.DIPLOMAT,
                                personagem.getPericiaEmissarioNatural()),
                        personagem.getPericiaEmissarioNatural());
            }
        }
        if (personagem.getPericiaMagoNatural() > 0) {
            if (personagem.getPericiaMago() != personagem.getPericiaMagoNatural()) {
                ret += String.format(mask1,
                        cenarioFacade.getTituloPericia(cenario,
                                CenarioFacade.WIZARD,
                                personagem.getPericiaMagoNatural()),
                        personagem.getPericiaMago(),
                        personagem.getPericiaMagoNatural());
            } else {
                ret += String.format(mask2,
                        cenarioFacade.getTituloPericia(cenario,
                                CenarioFacade.WIZARD,
                                personagem.getPericiaMagoNatural()),
                        personagem.getPericiaMagoNatural());
            }
        }
        //Furtividade nao é pericia, pode ser aumentada mesmo sendo 0
        if (personagem.getPericiaFurtividade() > 0) {
            if (personagem.getPericiaFurtividade() != personagem.getPericiaFurtividadeNatural()) {
                ret += String.format(mask1,
                        labels.getString("FURTIVIDADE"),
                        personagem.getPericiaFurtividadeNatural(),
                        personagem.getPericiaFurtividade());
            } else {
                ret += String.format(mask2,
                        labels.getString("FURTIVIDADE"),
                        personagem.getPericiaFurtividadeNatural());
            }
        }
        if (personagem.getDuelo() > 0) {
            ret += String.format(mask2,
                    labels.getString("DUELO"),
                    personagem.getDuelo());
        }
        return ret;
    }

    public static List<String> getInfo(Personagem personagem) {
        StringRet ret = new StringRet();
        if (personagemFacade.isMorto(personagem)) {
            //do nothing, just skip.
        } else {
            ret.addTab(String.format(labels.getString("S.DE.S"), personagemFacade.getNome(personagem), personagemFacade.getNacaoNome(personagem)));
        }
        return ret.getList();
    }
}
