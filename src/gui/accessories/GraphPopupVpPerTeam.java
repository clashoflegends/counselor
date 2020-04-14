/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories;

import business.services.ComparatorFactory;
import control.facade.WorldFacadeCounselor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
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

/**
 * best example at: https://docs.oracle.com/javafx/2/charts/bar-chart.htm
 *
 * @author jmoura
 */
public class GraphPopupVpPerTeam {

    private static final Log log = LogFactory.getLog(GraphPopupVpPerTeam.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final Set<String> teams = new TreeSet<String>();
    private final SortedMap<String, Nacao> mapNations = new TreeMap<String, Nacao>();

    public void start() {
        //leave it here because we don't know from what thread it's coming from?
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initAndShowFXGUI();
            }
        });
    }

    private void initAndShowFXGUI() {
        // This method is invoked on the EDT thread
        final JFXPanel fxPanel = new JFXPanel();

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(fxPanel);
            }
        });
    }

    private void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(labels.getString("PONTOS.VITORIA"));
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
        yAxis.setLabel(labels.getString("PONTOS.VITORIA"));
        xAxis.setLabel(labels.getString("TEAM"));
        xAxis.setTickMarkVisible(false);
        yAxis.setTickMarkVisible(false);

        final StackedBarChart<Number, String> stackedBarChart = new StackedBarChart<Number, String>(yAxis, xAxis);
        stackedBarChart.setTitle(labels.getString("PONTOS.VITORIA.TEAM"));
        stackedBarChart.setLegendVisible(true);
        populateData(stackedBarChart);

        return stackedBarChart;
    }

    private void populateData(final StackedBarChart<Number, String> stackedBarChart) {
        List<Nacao> nations = doPrepNations();
        List<XYChart.Series<Number, String>> seriesList = new ArrayList<XYChart.Series<Number, String>>();
        for (Nacao nation : nations) {
            //Series 1
            final XYChart.Series<Number, String> series = new XYChart.Series();
            series.setName(nation.getNome());
            series.getData().add(new XYChart.Data(nation.getPontosVitoria(), nation.getTeamFlag()));
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
    }

    private List<Nacao> doPrepNations() {
        List<Nacao> nations = new ArrayList<Nacao>(WorldFacadeCounselor.getInstance().getNacoes().values());
        //sort by points
        ComparatorFactory.getComparatorNationVictoryPointsSorter(nations);
        for (Nacao nation : nations) {
            //collect information
            teams.add(nation.getTeamFlag());
            mapNations.put(nation.getNome(), nation);
        }
        return nations;
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
}
