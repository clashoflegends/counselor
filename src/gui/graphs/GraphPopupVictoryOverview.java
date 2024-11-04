/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.graphs;

import business.facade.PointsFacade;
import business.services.ComparatorFactory;
import control.facade.WorldFacadeCounselor;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tooltip;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.swing.SwingUtilities;
import model.Habilidade;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import utils.CounterStringInt;

/**
 * best example at: https://docs.oracle.com/javafx/2/charts/bar-chart.htm
 *
 * @author jmoura
 */
public class GraphPopupVictoryOverview {

    private static final Log log = LogFactory.getLog(GraphPopupVictoryOverview.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final SortedMap<String, Nacao> mapNations = new TreeMap<>();
    private final String chartTitle = "PONTOS.VITORIA.OVERVIEW";

    public void start() {
        //leave it here because we don't know from what thread it's coming from?
        SwingUtilities.invokeLater(() -> {
            initAndShowFXGUI();
        });
    }

    private void initAndShowFXGUI() {
        // This method is invoked on the EDT thread
        final JFXPanel fxPanel = new JFXPanel();

        Platform.runLater(() -> {
            initFX(fxPanel);
        });
    }

    private void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        Stage window = new Stage();
        window.initModality(Modality.NONE);
        window.setTitle(labels.getString(chartTitle));
        window.setMinWidth(1000);

        StackedBarChart chart = createStackedBarChart();
        chart.setAnimated(true);
        Scene scene = new Scene(chart);

        //show and tell
        window.setScene(scene);
        window.showAndWait();

        fxPanel.setScene(scene);

    }

    private StackedBarChart createStackedBarChart() {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();

        //format axis
        //yAxis.setLabel(labels.getString("COUNT"));
        //xAxis.setLabel(labels.getString("TEAM"));
        xAxis.setTickMarkVisible(false);
        yAxis.setTickMarkVisible(false);

        final StackedBarChart<Number, String> stackedBarChart = new StackedBarChart<>(yAxis, xAxis);
        stackedBarChart.setLegendVisible(false);
        populateData(stackedBarChart);
        //stackedBarChart.setTitle(String.format("%s (%s %s)", labels.getString(chartTitle), totalCount, labels.getString("TOTAL")));
        stackedBarChart.setTitle(labels.getString(chartTitle));

        return stackedBarChart;
    }

    private void populateData(final StackedBarChart<Number, String> stackedBarChart) {
        //FIXME: if team or NOT team
        //FIXME: Add label or tooltip on original quantities. i.e. 15:11
        PointsFacade pf = new PointsFacade();

        CounterStringInt pointsCount;
        String seriesName;

        doPrepNations();

        List<XYChart.Series<Number, String>> seriesList = new ArrayList<>();
        //DB.POWER.VSK=Victory goal: Domination, game ends when one nation has more key cities (original capital locations) than all opponents combined starting on %s turn. Or 3:1 for teams
        seriesName = getHabilidateName(";VSK;");
        if (seriesName != null) {
            pointsCount = pf.doVictoryDominationUsThem(WorldFacadeCounselor.getInstance().getLocais().values(), WorldFacadeCounselor.getInstance().getJogadorAtivo());
            doSeries(pointsCount, seriesName, seriesList);
        }

        //DB.POWER.VSP=Victory goal: Score, game ends when one nation has more victory points than all opponents combined starting on %s turn. Or 3:1 for teams
        seriesName = getHabilidateName(";VSP;");
        if (seriesName != null) {
            pointsCount = pf.doVictoryScoreUsThem(WorldFacadeCounselor.getInstance().getNacoes().values(), WorldFacadeCounselor.getInstance().getJogadorAtivo());
            doSeries(pointsCount, seriesName, seriesList);
        }

        //DB.POWER.VSC=Victory goal: Conquest, game ends when one nation has more burghs and metropolis than all opponents combined starting on %s turn.  Or 3:1 for teams
        seriesName = getHabilidateName(";VSC;");
        if (seriesName != null) {
            pointsCount = pf.doVictoryConquestUsThem(WorldFacadeCounselor.getInstance().getCidades(), WorldFacadeCounselor.getInstance().getJogadorAtivo());
            doSeries(pointsCount, seriesName, seriesList);
        }

        //DB.POWER.VSS=Victory goal: Supremacy, game ends when a team has twice as many nations as the other teams starting on %s turn. Or 3:1 for teams
        seriesName = getHabilidateName(";VSS;");
        if (seriesName != null) {
            pointsCount = pf.doVictorySupremacyUsThem(WorldFacadeCounselor.getInstance().getNacoes().values(), WorldFacadeCounselor.getInstance().getJogadorAtivo());
            doSeries(pointsCount, seriesName, seriesList);
        }

        //DB.POWER.VCP=Victory goal: Battle Royale, game ends when one nation has more key cities points than all opponents combined starting on %s turn.  Or 3:1 for teams
        seriesName = getHabilidateName(";VCP;");
        if (seriesName != null) {
            pointsCount = pf.doDominationBattleRoyaleUsThem(WorldFacadeCounselor.getInstance().getLocais().values(), WorldFacadeCounselor.getInstance().getJogadorAtivo());
            doSeries(pointsCount, seriesName, seriesList);
        }

        //add all to graph
        stackedBarChart.getData().addAll(seriesList);

        //config bar colours
        styleNew(stackedBarChart);

        //install tooltips for the graph
        for (final Series< Number, String> series : stackedBarChart.getData()) {
            for (final Data<Number, String> data : series.getData()) {
                Tooltip tooltip = new Tooltip();
                tooltip.setText(String.format("%s - %s", series.getName(), data.getXValue().toString()));
                Tooltip.install(data.getNode(), tooltip);
            }
        }
    }

    private String getHabilidateName(String cdAbility) {
        //Partida obverrides Scenario
        Habilidade hab1 = WorldFacadeCounselor.getInstance().getPartida().getHabilidades().get(cdAbility);
        if (hab1 != null) {
            return hab1.getNome();
        }
        Habilidade hab2 = WorldFacadeCounselor.getInstance().getCenario().getHabilidades().get(cdAbility);
        if (hab2 != null) {
            return hab2.getNome();
        } else {
            return null;
        }
    }

    private void doSeries(CounterStringInt pointsCount, String seriesName, List<Series<Number, String>> seriesList) {
        for (String flagTeam : pointsCount.getKeys()) {
            //Series 1
            final XYChart.Series<Number, String> series = new XYChart.Series();
            series.setName(flagTeam);
//            log.error(String.format("%s-%s = %s", nmNation, mapNations.get(nmNation).getTeamFlag(), pointsCount.getValue(nmNation) ));
            series.getData().add(new XYChart.Data(pointsCount.getValuePercent(flagTeam), seriesName));
            seriesList.add(series);
        }
    }

    private List<Nacao> doPrepNations() {
        List<Nacao> nations = new ArrayList<>(WorldFacadeCounselor.getInstance().getNacoes().values());
        //sort by points
        ComparatorFactory.getComparatorNationVictoryPointsSorter(nations);
        for (Nacao nation : nations) {
            //collect information
            mapNations.put(nation.getNome(), nation);
        }
        return nations;
    }

    private void styleNew(StackedBarChart<Number, String> chart) {
        for (final XYChart.Series< Number, String> series : chart.getData()) {
            for (final XYChart.Data<Number, String> data : series.getData()) {
                StringBuilder style = new StringBuilder();
                if (series.getName().equals("Them")) {
                    style.append(String.format("-fx-background-color: %s; ", "GREY"));
                } else {
                    style.append(String.format("-fx-background-color: %s; ", "GREEN"));
                }
                data.getNode().setStyle(style.toString());
            }
        }
    }

}
