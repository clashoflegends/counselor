/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.graphs;

import business.services.ComparatorFactory;
import control.facade.WorldFacadeCounselor;
import gui.services.SampleTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;

/**
 *
 * @author jmoura
 */
public class GraphPopupScoreByNation {

    private static final Log log = LogFactory.getLog(GraphPopupScoreByNation.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final Set<String> teams = new TreeSet<>();
    private final SortedMap<String, Nacao> mapNations = new TreeMap<>();

    public void start() {
        //leave it here because we don't know from what thread it's coming from?
        SwingUtilities.invokeLater(this::initAndShowFXGUI);
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
        window.setTitle(labels.getString("PONTOS.VITORIA.NATION"));
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

    private List<String> getNames(List<Nacao> model) {
        List<String> ret = new ArrayList<>();
        for (Nacao nation : model) {
            ret.add(nation.getNome());

        }
        return ret;
    }

    private List<Nacao> doPrepNations() {
        List<Nacao> nations = new ArrayList<>(WorldFacadeCounselor.getInstance().getNacoes().values());
        //sort by points
        ComparatorFactory.getComparatorNationVictoryPointsSorter(nations);
        for (Nacao nation : nations) {
            //collect information
            teams.add(nation.getTeamFlag());
            mapNations.put(nation.getNome(), nation);
        }
        return nations;
    }

    private void styleNew(StackedBarChart<Number, String> chart) {
        for (final XYChart.Series< Number, String> series : chart.getData()) {
            for (final XYChart.Data<Number, String> data : series.getData()) {
                StringBuilder style = new StringBuilder();
                if (series.getName().equals("-")) {
                    style.append(String.format("-fx-background-color: %s; ", "GREY"));
                } else {
                    style.append(String.format("-fx-background-color: %s; ", series.getName()));
                }
                data.getNode().setStyle(style.toString());
            }
        }
    }

    private StackedBarChart createStackedBarChart() {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();

        List<Nacao> nations = doPrepNations();
        xAxis.setCategories(FXCollections.<String>observableList(getNames(nations)));

        //format axis
        yAxis.setTickUnit(200);
        yAxis.setLabel(labels.getString("PONTOS.VITORIA"));
        //xAxis.setLabel(labels.getString("NACAO"));
        xAxis.setTickMarkVisible(false);
        List<XYChart.Series<Number, String>> seriesList = new ArrayList<>();
        for (String teamName : teams) {

            //Series 1
            final XYChart.Series<Number, String> series = new XYChart.Series();
            series.setName(teamName);

            for (Nacao nation : nations) {
                if (!teamName.equals(nation.getTeamFlag())) {
                    continue;
                }
                series.getData().add(new XYChart.Data(nation.getPontosVitoria(), nation.getNome()));
            }
            seriesList.add(series);
        }

        final StackedBarChart<Number, String> stackedBarChart = new StackedBarChart<>(yAxis, xAxis);
//        stackedBarChart.setTitle(labels.getString("PONTOS.VITORIA"));
        stackedBarChart.getData().addAll(seriesList);
        stackedBarChart.setLegendVisible(false);
        styleNew(stackedBarChart);

        return stackedBarChart;
    }

    private StackedBarChart createStackedBarChartAllSameColors() {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();

        List<Nacao> nations = new ArrayList<>(WorldFacadeCounselor.getInstance().getNacoes().values());
        ComparatorFactory.getComparatorNationVictoryPointsSorter(nations);
        xAxis.setCategories(FXCollections.<String>observableList(getNames(nations)));

        //format axis
        yAxis.setTickUnit(200);
        yAxis.setLabel(labels.getString("PONTOS.VITORIA"));
        //xAxis.setLabel(labels.getString("NACAO"));
        xAxis.setTickMarkVisible(false);

        //Series 1
        XYChart.Series<Number, String> series = new XYChart.Series();
        //series1.setName("XYChart.Series 1");

        for (Nacao nation : WorldFacadeCounselor.getInstance().getNacoes().values()) {
            final XYChart.Data data = new XYChart.Data(nation.getPontosVitoria(), nation.getNome());
            series.getData().add(data);
        }

        //
        final StackedBarChart<Number, String> stackedBarChart = new StackedBarChart<>(yAxis, xAxis);
//        stackedBarChart.setTitle(labels.getString("PONTOS.VITORIA"));
        stackedBarChart.getData().addAll(series);
        stackedBarChart.setLegendVisible(false);
//        stackedBarChart.setCategoryGap(0.2);

        return stackedBarChart;
    }

    private BarChart createBarChartDynamic() {
        SampleTableModel tableModel = new SampleTableModel();
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setCategories(FXCollections.<String>observableArrayList(tableModel.getColumnNames()));
        xAxis.setLabel("Year");

        double tickUnit = tableModel.getTickUnit();

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickUnit(tickUnit);
        yAxis.setLabel("Units Sold");

        final BarChart aChart = new BarChart(xAxis, yAxis, tableModel.getBarChartData());
        aChart.setAnimated(true);
        tableModel.addTableModelListener((TableModelEvent e) -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                final int row = e.getFirstRow();
                final int column = e.getColumn();
                final Object value = ((SampleTableModel) e.getSource()).getValueAt(row, column);

                Platform.runLater(() -> {
                    XYChart.Series<String, Number> s = (XYChart.Series<String, Number>) aChart.getData().get(row);
                    BarChart.Data data = s.getData().get(column);
                    data.setYValue(value);
                });
            }
        });
        return aChart;
    }

    private BarChart createBarChart() {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();

        List<Nacao> nations = new ArrayList<>(WorldFacadeCounselor.getInstance().getNacoes().values());
        ComparatorFactory.getComparatorNationVictoryPointsSorter(nations);
        xAxis.setCategories(FXCollections.<String>observableList(getNames(nations)));

        //format axis
        yAxis.setTickUnit(200);
        yAxis.setLabel(labels.getString("PONTOS.VITORIA"));
        //xAxis.setLabel(labels.getString("NACAO"));
        xAxis.setTickMarkVisible(false);

        //Series 1
        XYChart.Series<Number, String> series = new XYChart.Series();
        //series1.setName("XYChart.Series 1");

        for (Nacao nation : WorldFacadeCounselor.getInstance().getNacoes().values()) {
            final XYChart.Data data = new XYChart.Data(nation.getPontosVitoria(), nation.getNome());
            series.getData().add(data);
        }

        //
        final BarChart<Number, String> stackedBarChart = new BarChart<>(yAxis, xAxis);
//        stackedBarChart.setTitle(labels.getString("PONTOS.VITORIA"));
        stackedBarChart.getData().addAll(series);
        stackedBarChart.setLegendVisible(false);
//        stackedBarChart.setCategoryGap(0.2);

        return stackedBarChart;
    }

    private void styleOld(StackedBarChart chart) {
        int nSeries = 0;
        Set<Node> nodes = chart.lookupAll(".series" + nSeries);
        for (Node n : nodes) {
            StringBuilder style = new StringBuilder();
            if (true) {
                style.append("-fx-background-color: red; ");
            } else {
                style.append("-fx-background-color: white, blue; ");
            }

            n.setStyle(style.toString());
        }
        nSeries++;

    }
}
