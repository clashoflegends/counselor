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
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;
import utils.CounterStringInt;

/**
 * best example at: https://docs.oracle.com/javafx/2/charts/bar-chart.htm
 *
 * @author jmoura
 */
public class GraphPopupDominationPerTeam {

    private static final Log log = LogFactory.getLog(GraphPopupDominationPerTeam.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final SortedMap<String, Nacao> mapNations = new TreeMap<>();

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
        window.setTitle(labels.getString("PONTOS.DOMINATION.BATTLEROYAL.TEAM"));
        window.setMinWidth(500);

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
        yAxis.setLabel(labels.getString("PONTOS.DOMINATION"));
        xAxis.setLabel(labels.getString("TEAM"));
        xAxis.setTickMarkVisible(false);
        yAxis.setTickMarkVisible(false);

        final StackedBarChart<Number, String> stackedBarChart = new StackedBarChart<>(yAxis, xAxis);
        stackedBarChart.setLegendVisible(false);
        int totalCount = populateData(stackedBarChart);
        stackedBarChart.setTitle(String.format("%s (%s %s)", labels.getString("PONTOS.DOMINATION"), totalCount, labels.getString("TOTAL")));

        return stackedBarChart;
    }

    private int populateData(final StackedBarChart<Number, String> stackedBarChart) {
        doPrepNations();
        List<XYChart.Series<Number, String>> seriesList = new ArrayList<>();
        PointsFacade pf = new PointsFacade();
        CounterStringInt pointsCount = pf.doDominationBattleRoyale(WorldFacadeCounselor.getInstance().getLocais().values(), WorldFacadeCounselor.getInstance().getNacaoNeutra());
        for (String nmNation : pointsCount.getKeys()) {
            //Series 1
            final XYChart.Series<Number, String> series = new XYChart.Series();
            series.setName(nmNation);
            //log.error(String.format("%s-%s = %s", nmNation, mapNations.get(nmNation).getTeamFlag(), pointsCount.getValue(nmNation)));
            series.getData().add(new XYChart.Data(pointsCount.getValue(nmNation), mapNations.get(nmNation).getTeamFlag()));
            seriesList.add(series);
        }

        stackedBarChart.getData().addAll(seriesList);

        //config bars
        styleNew(stackedBarChart);

        //install tooltips for the graph
        for (final Series< Number, String> series : stackedBarChart.getData()) {
            for (final Data<Number, String> data : series.getData()) {
                Tooltip tooltip = new Tooltip();
                tooltip.setText(String.format("%s - %s", series.getName(), data.getXValue().toString()));
                Tooltip.install(data.getNode(), tooltip);
            }
        }
        return pointsCount.getTotal();
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
                style.append(String.format("-fx-background-color: %s; ", getNationColorFill(series.getName())));
                style.append(String.format("-fx-border-color: %s; ", getNationColorBorder(series.getName())));
                data.getNode().setStyle(style.toString());
            }
        }
    }

    private String getNationColorFill(String nmNation) {
        try {
            final String color = SysApoio.colorToHexa(mapNations.get(nmNation).getFillColor());
            log.debug(String.format("%s %s", nmNation, color));
            if (color.equals("#030303")) {
                //invert colors, too much black. Particularly important for WDO where all orcs are black.
                return SysApoio.colorToHexa(mapNations.get(nmNation).getBorderColor());
            } else {
                //use ok color
                return color;
            }
        } catch (NullPointerException e) {
            return "GREY";
        }
    }

    private String getNationColorBorder(String nmNation) {
        try {
            final String color = SysApoio.colorToHexa(mapNations.get(nmNation).getFillColor());
            if (color.equals("#030303")) {
                //invert colors, too much black. Particularly important for WDO where all orcs are black.
                return color;
            } else {
                //use ok color
                return SysApoio.colorToHexa(mapNations.get(nmNation).getBorderColor());
            }
        } catch (NullPointerException e) {
            return "GREY";
        }
    }
}
