/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.accessories;

import business.services.ComparatorFactory;
import control.facade.WorldFacadeCounselor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.swing.SwingUtilities;
import model.Nacao;
import model.VictoryPointsGame;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 * best example at: https://www.tutorialspoint.com/javafx/stacked_area_chart.htm
 *
 * @author jmoura
 */
public class GraphPopupVpPerTurn {

    private static final Log log = LogFactory.getLog(GraphPopupVpPerTurn.class);
    private static final BundleManager labels = SettingsManager.getInstance().getBundleManager();
    private final SortedMap<String, Nacao> mapNations = new TreeMap<String, Nacao>();
    private VictoryPointsGame victoryPoints;

    public void start(VictoryPointsGame victoryPointsAllTurns) {
        victoryPoints = victoryPointsAllTurns;
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

        StackedAreaChart chart = createStackedAreaChart();
        chart.setAnimated(true);
        Scene scene = new Scene(chart);

        //show and tell
        window.setScene(scene);
        window.showAndWait();

        fxPanel.setScene(scene);

    }

    private StackedAreaChart<String, Number> createStackedAreaChart() {
        //Defining the X axis               
        CategoryAxis xAxis = new CategoryAxis();

        xAxis.setCategories(FXCollections.<String>observableList(victoryPoints.getTurnListAsString()));
        //xAxis.setCategories(FXCollections.<String>observableArrayList(Arrays.asList("1750", "1800", "1850", "1900", "1950", "1999", "2050")));

        //Defining the Y axis 
        NumberAxis yAxis = new NumberAxis(0, 10000, 2500);
        yAxis.setLabel(labels.getString("PONTOS.VITORIA"));

        //creating graph itself
        StackedAreaChart<String, Number> areaChart = new StackedAreaChart(xAxis, yAxis);
        areaChart.setTitle(labels.getString("PONTOS.VITORIA.TEAM"));
        areaChart.setLegendVisible(true);

        //populating graph data
        populateData(areaChart);

        return areaChart;
    }

    private void populateData(final StackedAreaChart<String, Number> areaChart) {
        List series = new ArrayList();
        //Prepare XYChart.Series objects by setting data 
        for (Nacao nation : victoryPoints.getNationsList()) {
            //initialize serie
            XYChart.Series serieNation = new XYChart.Series();
            serieNation.setName(nation.getNome());
            SortedMap<Integer, Integer> nationPoints = victoryPoints.getNationPoints(nation);
            for (Integer turn : victoryPoints.getTurnList()) {
                //series1.getData().add(new XYChart.Data("1750", 502));
                serieNation.getData().add(new XYChart.Data(turn + "", nationPoints.get(turn)));
            }
            //add to list
            series.add(serieNation);

        }
        //Setting the data to area chart        
        areaChart.getData().addAll(series);
    }

    private List<Nacao> doPrepNations() {
        List<Nacao> nations = new ArrayList<Nacao>(WorldFacadeCounselor.getInstance().getNacoes().values());
        //sort by points
        ComparatorFactory.getComparatorNationVictoryPointsSorter(nations);
        for (Nacao nation : nations) {
            //collect information
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
