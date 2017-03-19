/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.services;

import baseLib.GenericoTableModel;
import business.facade.AcaoFacade;
import business.facade.CenarioFacade;
import business.facade.NacaoFacade;
import business.facades.ListFactory;
import business.facades.WorldFacadeCounselor;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import model.Cenario;
import model.ExtratoDetail;
import model.Mercado;
import model.Nacao;
import model.Ordem;
import model.PersonagemOrdem;
import model.Produto;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author Gurgel
 */
public class FinancasConverter implements Serializable {

    public static final int FILTRO_PROPRIOS = 1;
    public static final int FILTRO_TODOS = 0;
    private static final Log log = LogFactory.getLog(FinancasConverter.class);
    private static final NacaoFacade nacaoFacade = new NacaoFacade();
    private static final CenarioFacade cenarioFacade = new CenarioFacade();
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private static final AcaoFacade acaoFacade = new AcaoFacade();
    public static final int SIZE = 14;

    public static GenericoTableModel getExtratoTableModel(Nacao nacao) {
        String[] colNames = new String[]{labels.getString("NOME"), labels.getString("VALOR"),
            labels.getString("SALDO"), labels.getString("SEQUENCIA")};
        Object[][] resumo = new Object[nacao.getExtratoSize()][colNames.length];
        if (nacao == null) {
            throw new UnsupportedOperationException(labels.getString("NOT.IMPLEMENTED"));
        } else {
            try {
                int ii = 0;
                for (Iterator<ExtratoDetail> it = nacao.getExtrato().getExtratoDetalhes(); it.hasNext();) {
                    ExtratoDetail exDet = it.next();
                    resumo[ii][0] = exDet.getDescricao();
                    resumo[ii][1] = exDet.getValor();
                    resumo[ii][2] = exDet.getSaldoAtual();
                    resumo[ii][3] = exDet.getSequencial() + 1;
                    ii++;
                }
            } catch (IndexOutOfBoundsException ex) {
                throw new UnsupportedOperationException(labels.getString("NOT.IMPLEMENTED"));
            }
        }
        GenericoTableModel model = new GenericoTableModel(colNames, resumo,
                new Class[]{java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class});
        return model;
    }

    /*
     Exércitos/Esquadras 	3.600
     Centros Pop. 	3.750
     Personagens 	9.000
     Total 	16.350
     Nível atual de Impostos 	40%
     Arrecadação esperada para o próximo turno 	15.850
     Resultado esperado para o próximo turno 	-500
     Reservas de Ouro 	49.500
     */
    public GenericoTableModel getProjecaoTableModel(Nacao nacao, List<PersonagemOrdem> listPo) {
        String[] colNames = new String[]{labels.getString("NOME"), labels.getString("VALOR")};
        final int tableSize;
        if (listPo.isEmpty()) {
            tableSize = SIZE;
        } else {
            tableSize = SIZE + listPo.size() + 3;
        }
        Object[][] dados = new Object[tableSize][colNames.length];
        if (nacao == null) {
            throw new UnsupportedOperationException(labels.getString("NOT.IMPLEMENTED"));
        } else {
            final Cenario cenario = WorldFacadeCounselor.getInstance().getCenario();
            ListFactory lf = new ListFactory();
            int ii = 0;
            int valorAcoes = 0;
            int exercitos = nacaoFacade.getCustoExercitoNacao(nacao, lf.listExercitos().values()) * -1;
            int exeBonus = nacaoFacade.getDescontoExercitoNacao(nacao, lf.listExercitos().values());
            int cidadesUpkeep = nacaoFacade.getCustoCidades(nacao, cenario) * -1;
            int personagens = nacaoFacade.getCustoPersonagens(nacao, cenario) * -1;
            int arrecadacao = nacaoFacade.getArrecadacao(nacao);
            int ouroProd = nacaoFacade.getProducao(nacao, cenario.getMoney(), cenario, WorldFacadeCounselor.getInstance().getTurno());
            final int custos = exercitos + cidadesUpkeep + personagens + exeBonus;
            if (exeBonus != 0) {
                dados[ii][0] = labels.getString("FINANCAS.CURRENT.DISCOUNT.ARMIES");
                dados[ii++][1] = exeBonus;
            }
            dados[ii][0] = labels.getString("FINANCAS.CURRENT.UPKEEP.ARMIES");
            dados[ii++][1] = exercitos;
            dados[ii][0] = labels.getString("FINANCAS.CURRENT.UPKEEP.CHARS");
            dados[ii++][1] = personagens;
            dados[ii][0] = labels.getString("FINANCAS.CURRENT.UPKEEP.CITIES");
            dados[ii++][1] = cidadesUpkeep;
            dados[ii][0] = labels.getString("FINANCAS.CURRENT.COSTS");
            dados[ii++][1] = (custos);
            dados[ii][0] = " ";
            dados[ii++][1] = null;
            dados[ii][0] = labels.getString("FINANCAS.CURRENT.TAXES.PERCENT");
            dados[ii++][1] = nacaoFacade.getImpostos(nacao);
            dados[ii][0] = labels.getString("FINANCAS.CURRENT.TAXES.GATHER");
            dados[ii++][1] = arrecadacao;
            dados[ii][0] = labels.getString("FINANCAS.CURRENT.GOLD.GATHER");
            dados[ii++][1] = ouroProd;
            dados[ii][0] = labels.getString("FINANCAS.CURRENT.REVENUE");
            dados[ii++][1] = arrecadacao + ouroProd;
            dados[ii][0] = " ";
            dados[ii++][1] = null;
            dados[ii][0] = labels.getString("FINANCAS.CURRENT.TREASURY");
            dados[ii++][1] = nacaoFacade.getMoney(nacao);
            dados[ii][0] = labels.getString("FINANCAS.CURRENT.UPKEEP.DEFICT");
            dados[ii++][1] = (arrecadacao + ouroProd) + (custos);
            dados[ii][0] = labels.getString("FINANCAS.FORECAST.RESERVE");
            final int moneyFinal = (arrecadacao + ouroProd) + (custos) + nacaoFacade.getMoney(nacao);
            dados[ii++][1] = moneyFinal;
            if (!listPo.isEmpty()) {
                dados[ii][0] = " ";
                dados[ii++][1] = null;

                for (PersonagemOrdem po : listPo) {
                    final Ordem ordem = po.getOrdem();
                    dados[ii][0] = String.format("%s - %s", po.getNome(), ordem.getDescricao());
                    dados[ii++][1] = acaoFacade.getCusto(ordem) * -1;
                    valorAcoes -= acaoFacade.getCusto(ordem);
                }
                dados[ii][0] = labels.getString("FINANCAS.COST.ACTIONS");
                dados[ii++][1] = valorAcoes;
                //skip one row
                dados[ii][0] = " ";
                dados[ii++][1] = null;
                final int decay = nacaoFacade.getGoldDecay(nacao, moneyFinal + valorAcoes, cenario) * -1;
                if (decay != 0) {
                    dados[ii][0] = labels.getString("FINANCAS.FORECAST.DECAY");
                    dados[ii++][1] = decay;
                }
                dados[ii][0] = labels.getString("FINANCAS.FORECAST.FINAL");
                dados[ii++][1] = moneyFinal + valorAcoes + decay;
            }
        }
        GenericoTableModel model = new GenericoTableModel(colNames, dados,
                new Class[]{java.lang.String.class, java.lang.Integer.class});
        return model;
    }

    public static GenericoTableModel getMercadoModel(Nacao nacao) {
        GenericoTableModel model = new GenericoTableModel(
                getMercadoColNames(),
                getMercadoAsArray(nacao),
                new Class[]{java.lang.String.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class
                });
        return model;
    }

    private static Object[][] getMercadoAsArray(Nacao nacao) {
        if (nacao == null) {
            Object[][] ret = {{"", ""}};
            return (ret);
        } else {
            try {
                final Cenario cenario = WorldFacadeCounselor.getInstance().getCenario();
                Produto[] produtos = cenarioFacade.listProdutos(cenario, 1);
                int ii = 0, nn = 0;
                Mercado mercado = WorldFacadeCounselor.getInstance().getMercado();
                Object[][] ret = new Object[produtos.length][getMercadoColNames().length];
                for (Produto produto : produtos) {
                    int prod = nacaoFacade.getProducao(nacao, produto, cenario, WorldFacadeCounselor.getInstance().getTurno());
                    int est = nacaoFacade.getEstoque(nacao, produto);
                    int unit = mercado.getProdutoVlVenda(produto);
                    ret[ii][nn++] = produto.getNome();
                    ret[ii][nn++] = est;
                    ret[ii][nn++] = prod;
                    ret[ii][nn++] = (prod + est) * unit;
                    ret[ii][nn++] = unit;
                    ret[ii][nn++] = mercado.getProdutoVlCompra(produto);
                    ret[ii][nn++] = mercado.getProdutoQtDisponivel(produto);
                    ii++;
                    nn = 0;
                }
                return (ret);
            } catch (IndexOutOfBoundsException ex) {
                throw new UnsupportedOperationException(labels.getString("NOT.IMPLEMENTED"));
            }
        }
    }

    public static String[] getMercadoColNames() {
        /*
         * Unidades disponíveis no mercado
         * Preço de compra por unidade
         * Preço de venda por unidade
         * Estoques atuais
         * Produção esperada
         * Preço de venda esperado
         */
        String[] ret = new String[7];
        int ii = 0;
        ret[ii++] = labels.getString("PRODUTO");
        ret[ii++] = labels.getString("FINANCAS.MERCADO.ESTOQUES.ATUAIS");
        ret[ii++] = labels.getString("FINANCAS.MERCADO.PRODUCTION.EXPECTED");
        ret[ii++] = labels.getString("FINANCAS.MERCADO.TOTAL.GOLD");
        ret[ii++] = labels.getString("FINANCAS.MERCADO.VENDA.UNITARIA");
        ret[ii++] = labels.getString("FINANCAS.MERCADO.COMPRA.UNITARIA");
        ret[ii++] = labels.getString("FINANCAS.MERCADO.AVAILABLE.UNITS");
        return ret;
    }
}
