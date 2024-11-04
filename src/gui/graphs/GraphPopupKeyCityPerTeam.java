/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.graphs;

import business.facade.PointsFacade;
import control.facade.WorldFacadeCounselor;
import java.util.SortedMap;
import java.util.TreeMap;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Tooltip;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.swing.SwingUtilities;
import model.Nacao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.BundleManager;
import persistenceCommons.SettingsManager;
import utils.CounterStringInt;

/**
 * best example at: https://docs.oracle.com/javafx/2/charts/pie-chart.htm
 *
 * @author jmoura
 */
public class GraphPopupKeyCityPerTeam {

    private static final Log log = LogFactory.getLog(GraphPopupKeyCityPerTeam.class);
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
        window.setTitle(labels.getString("PONTOS.KEYCITY.TEAM"));
        window.setMinWidth(500);

        PieChart chart = createPizzaChartTemp();
        chart.setAnimated(true);
        Scene scene = new Scene(chart);

        //show and tell
        window.setScene(scene);
        window.showAndWait();

        fxPanel.setScene(scene);

    }

    private PieChart createPizzaChartTemp() {
        CounterStringInt totalCount = populateData();
        final double total = totalCount.getTotal();

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (String nmTeam : totalCount.getKeys()) {
            pieChartData.add(new PieChart.Data(nmTeam, totalCount.getValue(nmTeam)));
        }
        final PieChart chart = new PieChart(pieChartData);
        chart.setTitle(String.format("%s (%s %s)", labels.getString("PONTOS.KEYCITY.TEAM"), total, labels.getString("TOTAL")));
        chart.setLegendVisible(false);
        chart.setLabelLineLength(10);
        //install tooltips for the graph
        chart.getData().stream().forEach(data -> {
            final String percentage = String.format("%.0f\n%.1f%%", data.getPieValue(), (data.getPieValue() / total * 100));
            Tooltip toolTip = new Tooltip(percentage);
            Tooltip.install(data.getNode(), toolTip);
        });
        applyCustomColorSequence(
                pieChartData,
                "grey",
                "navy",
                "red",
                "yellow",
                "green"
        );
        return chart;
    }

    private CounterStringInt populateData() {
        final PointsFacade pf = new PointsFacade();
        final CounterStringInt pointsCount = pf.doVictoryDominationTeam(WorldFacadeCounselor.getInstance().getLocais().values(), WorldFacadeCounselor.getInstance().getNacaoNeutra());
        return pointsCount;
    }

    private void applyCustomColorSequence(ObservableList<PieChart.Data> pieChartData, String... pieColors) {
        int i = 0;
        for (PieChart.Data data : pieChartData) {
            data.getNode().setStyle("-fx-pie-color: " + pieColors[i % pieColors.length] + ";");
            i++;
        }
    }
}
