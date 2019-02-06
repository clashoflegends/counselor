/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories;

import business.services.ComparatorFactory;
import control.facade.WorldFacadeCounselor;
import gui.services.SampleTableModel;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
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
import javax.swing.event.TableModelListener;
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

    private List<String> getNames(List<Nacao> model) {
        List<String> ret = new ArrayList<String>();
        for (Nacao nacao : model) {
            ret.add(nacao.getNome());
        }
        return ret;
    }
    final static String[] teams = {"-", "RED", "BLUE", "GREEN", "YELLOW"};

    private StackedBarChart createStackedBarChart() {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();

        List<Nacao> nations = new ArrayList<Nacao>(WorldFacadeCounselor.getInstance().getNacoes().values());
        ComparatorFactory.getComparatorNationVictoryPointsSorter(nations);
        xAxis.setCategories(FXCollections.<String>observableList(getNames(nations)));

        //format axis
        yAxis.setTickUnit(200);
        yAxis.setLabel(labels.getString("PONTOS.VITORIA"));
        //xAxis.setLabel(labels.getString("NACAO"));
        xAxis.setTickMarkVisible(false);
        List<XYChart.Series<Number, String>> seriesList = new ArrayList<XYChart.Series<Number, String>>();
        for (String teamName : teams) {

            //Series 1
            final XYChart.Series<Number, String> series = new XYChart.Series();
            series.setName(teamName);

            for (Nacao nation : WorldFacadeCounselor.getInstance().getNacoes().values()) {
                if (!teamName.equals(nation.getTeamFlag())) {
                    continue;
                }
                series.getData().add(new XYChart.Data(nation.getPontosVitoria(), nation.getNome()));
            }
            seriesList.add(series);
        }

        final StackedBarChart<Number, String> stackedBarChart = new StackedBarChart<Number, String>(yAxis, xAxis);
//        stackedBarChart.setTitle(labels.getString("PONTOS.VITORIA"));
        stackedBarChart.getData().addAll(seriesList);
        stackedBarChart.setLegendVisible(false);
//        stackedBarChart.setCategoryGap(0.2);

        return stackedBarChart;
    }

    private StackedBarChart createStackedBarChartAllSameColors() {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();

        List<Nacao> nations = new ArrayList<Nacao>(WorldFacadeCounselor.getInstance().getNacoes().values());
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
        final StackedBarChart<Number, String> stackedBarChart = new StackedBarChart<Number, String>(yAxis, xAxis);
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
        tableModel.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    final int row = e.getFirstRow();
                    final int column = e.getColumn();
                    final Object value = ((SampleTableModel) e.getSource()).getValueAt(row, column);

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            XYChart.Series<String, Number> s = (XYChart.Series<String, Number>) aChart.getData().get(row);
                            BarChart.Data data = s.getData().get(column);
                            data.setYValue(value);
                        }
                    });
                }
            }
        });
        return aChart;
    }

    private BarChart createBarChart() {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();

        List<Nacao> nations = new ArrayList<Nacao>(WorldFacadeCounselor.getInstance().getNacoes().values());
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
        final BarChart<Number, String> stackedBarChart = new BarChart<Number, String>(yAxis, xAxis);
//        stackedBarChart.setTitle(labels.getString("PONTOS.VITORIA"));
        stackedBarChart.getData().addAll(series);
        stackedBarChart.setLegendVisible(false);
//        stackedBarChart.setCategoryGap(0.2);

        return stackedBarChart;
    }
}
