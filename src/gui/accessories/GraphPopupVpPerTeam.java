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
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
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

/**
 * best example at: https://docs.oracle.com/javafx/2/charts/bar-chart.htm
 *
 * @author jmoura
 */
public class GraphPopupVpPerTeam {

    private static final Log log = LogFactory.getLog(GraphPopupVpPerTeam.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    final static String[] teams = {"-", "RED", "BLUE", "GREEN", "YELLOW"};

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

        Chart chart = createStackedBarChart();
//        Chart chart = createBarChart();
//        Chart chart = createBarChartDynamic();
        chart.setAnimated(true);
        Scene scene = new Scene(chart);

        //show and tell
        window.setScene(scene);
        window.showAndWait();

        fxPanel.setScene(scene);
    }


    private List<String> getTeamNames(List<Nacao> model) {
        List<String> ret = new ArrayList<String>();
        for (Nacao nacao : model) {
            ret.add(nacao.getNome());
        }
        return ret;
    }

    private StackedBarChart createStackedBarChart() {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();

        List<Nacao> nations = new ArrayList<Nacao>(WorldFacadeCounselor.getInstance().getNacoes().values());
        ComparatorFactory.getComparatorNationVictoryPointsSorter(nations);

        //format axis
        yAxis.setLabel(labels.getString("PONTOS.VITORIA"));
        xAxis.setLabel(labels.getString("TEAM"));
        xAxis.setTickMarkVisible(false);
        yAxis.setTickMarkVisible(false);
        List<XYChart.Series<Number, String>> seriesList = new ArrayList<XYChart.Series<Number, String>>();

        for (Nacao nation : nations) {
            //Series 1
            final XYChart.Series<Number, String> series = new XYChart.Series();
            series.setName(nation.getNome());
            series.getData().add(new XYChart.Data(nation.getPontosVitoria(), nation.getTeamFlag()));
            seriesList.add(series);
        }

        final StackedBarChart<Number, String> stackedBarChart = new StackedBarChart<Number, String>(yAxis, xAxis);
        stackedBarChart.setTitle(labels.getString("PONTOS.VITORIA.TEAM"));
        stackedBarChart.getData().addAll(seriesList);
        stackedBarChart.setLegendVisible(true);

        //install tooltips for the graph
        for (final Series< Number, String> series : stackedBarChart.getData()) {
            for (final Data<Number, String> data : series.getData()) {
                Tooltip tooltip = new Tooltip();
                tooltip.setText(String.format("%s - %s", series.getName(), data.getXValue().toString()));
                Tooltip.install(data.getNode(), tooltip);
            }
        }
        return stackedBarChart;
    }
}
