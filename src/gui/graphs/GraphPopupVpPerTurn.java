/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.graphs;

import business.services.ComparatorFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
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
    private final List<Nacao> nationsList;
    private VictoryPointsGame victoryPoints;

    public GraphPopupVpPerTurn(Collection<Nacao> nations) {
        nationsList = new ArrayList<>(nations);
        //sort by points
        ComparatorFactory.getComparatorNationVictoryPointsSorter(nationsList);

    }

    public void start(VictoryPointsGame victoryPointsAllTurns) {
        victoryPoints = victoryPointsAllTurns;
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
        window.setTitle(labels.getString("PONTOS.VITORIA.GAME"));
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
        NumberAxis yAxis = new NumberAxis(0, 5000, 500);
        yAxis.setLabel(labels.getString("PONTOS.VITORIA.TOTAL"));

        //creating graph itself
        StackedAreaChart<String, Number> areaChart = new StackedAreaChart(xAxis, yAxis);
        areaChart.setTitle(labels.getString("PONTOS.VITORIA.HISTORY"));
        areaChart.setLegendVisible(false);
        areaChart.setCreateSymbols(false);

        //populating graph data
        populateData(areaChart);

        //FIXME: this works for the first 8 colors, then it does not scale.
        //Node node = areaChart.lookup(".default-color0.chart-series-area-fill");
        // set the first series fill to translucent pale green
        //node.setStyle(String.format("-fx-fill: #000000; "));
        return areaChart;
    }

    private void populateData(final StackedAreaChart<String, Number> areaChart) {
        List series = new ArrayList();
        //Prepare XYChart.Series objects by setting data 
        for (Nacao nation : nationsList) {
            //initialize serie
            XYChart.Series serieNation = new XYChart.Series();
            serieNation.setName(nation.getNome());
            SortedMap<Integer, Integer> nationPoints = victoryPoints.getNationPoints(nation);
            for (Integer turn : victoryPoints.getTurnList()) {
                final XYChart.Data item = new XYChart.Data(turn + "", nationPoints.get(turn));
                serieNation.getData().add(item);

//                //setting colors of charts
//                Node fill = serieNation.getNode().lookup(".chart-series-area-fill");
//                fill.setStyle("-fx-fill: #fff7ad;");
//                Node line = serieNation.getNode().lookup(".chart-series-area-line");
//                line.setStyle("-fx-stroke: #8bc34a;"
//                        + "-fx-stroke-width: 3px;"); // set width of line
            }
            //add to list
            series.add(serieNation);
        }

        //Setting the data to area chart        
        areaChart.getData().addAll(series);

        //install tooltips with series names for the graph
        for (final XYChart.Series<String, Number> items : areaChart.getData()) {
            Tooltip.install(items.getNode(), new Tooltip(items.getName()));
        }

//        //trying to figure out how to select colors for the area 
//        int ii = 0;
//        for (Nacao nation : nationsList) {
//            final Node node = areaChart.getData().get(ii++).getNode();
//            node.setStyle(String.format("-fx-fill: %s; ", getNationColorFill(nation)));
//            node.setStyle(String.format("-fx-background-color: %s; ", getNationColorFill(nation)));
//            node.setStyle(String.format("-fx-border-color: %s; ", getNationColorBorder(nation)));
//        }
//        for (XYChart.Series<String, Number> ss : areaChart.getData()) {
//            for (XYChart.Series<String, Number> series1 : ss.getChart().getData()) {
//                series1.getNode().setStyle("-fx-fill: #000000; ");
//            }
//        }
    }

    private String getNationColorFill(Nacao nation) {
        try {
            final String color = SysApoio.colorToHexa(nation.getFillColor());
            log.debug(String.format("%s %s", nation.getNome(), color));
            if (color.equals("#030303")) {
                //invert colors, too much black. Particularly important for WDO where all orcs are black.
                return SysApoio.colorToHexa(nation.getBorderColor());
            } else {
                //use ok color
                return color;
            }
        } catch (NullPointerException e) {
            return "GREY";
        }
    }

    private String getNationColorBorder(Nacao nation) {
        try {
            final String color = SysApoio.colorToHexa(nation.getFillColor());
            if (color.equals("#030303")) {
                //invert colors, too much black. Particularly important for WDO where all orcs are black.
                return color;
            } else {
                //use ok color
                return SysApoio.colorToHexa(nation.getBorderColor());
            }
        } catch (NullPointerException e) {
            return "GREY";
        }
    }
}
