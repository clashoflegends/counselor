/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import baseLib.SysApoio;
import baseLib.SysProperties;
import java.io.Serializable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.SettingsManager;

/**
 *
 * @author gurgel
 */
public class Main implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(Main.class);

    /**
     * @param args the command line arguments
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        // Invokes Gui to display turn results
        log.info("Starting...");
        log.info("Counselor version: " + SysApoio.getVersion("version_counselor"));
        log.info("Commons version: " + SysApoio.getVersion("version_commons"));
        SettingsManager.getInstance().setConfigurationMode("Client");
        SettingsManager.getInstance().setLanguage(SysProperties.getProps("language", "en"));
        String autoload = SysProperties.getProps("autoLoad");
        SettingsManager.getInstance().setWorldBuilder(SysProperties.getProps("worldBuilder", "0").equalsIgnoreCase("1"));
        SettingsManager.getInstance().setRadialMenu(SysProperties.getProps("newUi", "1").equalsIgnoreCase("1"));

        new PbmApplication(autoload).start(); //true to autoload results file
    }
    //The list below is too long and outdated. Please, ignore until trasnlated and converted to issues.
    /*
     * === Minor bugs ===
     * filtrar ordens que o personagem pode dar no momento, ordens possiveis apos o movimento (tipo transferir produtos depois de mover para a capital)
     * ajustar labels dos parametros
     * filtrar magias que o personagem pode aprender.
     * rever help da ordem
     * tab nacao com relacionamentos e outras infos...
     * exibir personagem, artefato, cidade e outras infos percebidas via ordem
     * cadastrar o extrato das financas da nacao
     * 
     * TODO: 
     * ALterar o server para gerar XML para todas as partidas marcadas
     * verticalizar o Mercado, guardando o id_produto
     * ordernar list de personagens por nome?
     * ordenar combo de magias? (Magia_All)
     * coordenar com o mapa
     * grupos
     * arrumar MapControl por funcoes... reduzir a quantidade de linhas por metodo
     * DirecaoExercito pode exibir as coordenadas que o exercito esta passando e desenhar seta no mapa?
     * Imprimir mapcontrol em layers, permitindo visao do terreno, visao dos personagens, duplo clique para posicionar coisas, etc...
     * ajustar as colunas das grids
     * Colocar o look and feel windows
     * programar filtros
     * programar dados adicionais nos tab.
     * criticar ordens
     * programar tabs
     * internacionalizar
     * ordem nao enviada aparece como UPGRADE RELATIONS, alterar para ordem BLANK?
     * ajustar o tooltip de help completo das ordens
     * dividir exercito deve completar com 0?
     * 
     * carregar as ordens salvas (LOAD)
     * ajustar diretorio de salva e abertura de xml
     * criar protecao para nao sobrescrever arquivo de resultado com as ordens
     * criar protecao para nao abrir resultado no lugar de ordens e vice versa
     * criar header com turno e ancao das ordens, para evitar abrir ordens com resultado errado
     * converter xml de exportacao de ordens para xml?
     * salvar ordens com header e em xml para carga mais rapida?
     *
     * Gera Turno:
     -emissario 35 ampliou um burgo para cidade com lealdade 75
     - "Calíope presenteou Eurydice com ouro e jóias no valor de 5.000, que foram enviadas a sua capital.", mas eurydice nao recebeu nenhuma mensagem em seus detalhes...
     -verificar consumo de couro e montarias quando criando exercito E recrutando, possivel refactoring to improve.

     Client
     -Opcao para todas as ordens, ou apenas orderns possiveis(?)
     -Ele recebeu ordens de AchArt null (null deveria ser oura coisa, talvez zero)
     * 
     */
}
