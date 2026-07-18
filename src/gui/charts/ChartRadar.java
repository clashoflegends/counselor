package gui.charts;

import business.ImageManager;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Radar / spider chart (JFreeChart {@link SpiderWebPlot}): one polygon per series over the shared axes.
 * Used by the Nation Power Comparison (X1) - you vs your top rivals across normalised power metrics, your
 * own polygon emphasised (thicker outline). Each {@link DataSetForChart}: key = series (nation),
 * grouping = axis (metric), value = 0..100 (already normalised by the caller), color = series colour,
 * emphasis = the player's own nation.
 */
public final class ChartRadar extends JFrame {

    private final List<DataSetForChart> dataSet;
    private final String subtitle;
    private final String caveat;

    public ChartRadar(String title, List<DataSetForChart> dataSet, String subtitle, String caveat) {
        super(title);
        setIconImage(ImageManager.getInstance().getBarChartIcon());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.dataSet = dataSet;
        this.subtitle = subtitle;
        this.caveat = caveat;
    }

    /** For parity with the ComponentFactory.showChart* lifecycle. */
    public void doStart() {
        final JFreeChart chart = createChart(createDataset());
        final ChartPanel panel = new ChartPanel(chart, false);
        panel.setPreferredSize(new Dimension(560, 500));
        setContentPane(panel);
    }

    private CategoryDataset createDataset() {
        final DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (DataSetForChart item : dataSet) {
            ds.addValue(item.getValue(), item.getKey(), item.getGrouping());
        }
        return ds;
    }

    private JFreeChart createChart(CategoryDataset dataset) {
        final SpiderWebPlot plot = new SpiderWebPlot(dataset);
        plot.setStartAngle(90D);
        plot.setInteriorGap(0.30D);
        plot.setMaxValue(100D);            // values are pre-normalised to 0..100
        plot.setWebFilled(false);          // outlines only, so overlapping polygons stay readable
        plot.setAxisLinePaint(java.awt.Color.LIGHT_GRAY);

        // per-series colour; the player's own nation gets a thicker outline
        final Map<String, DataSetForChart> styleByKey = new LinkedHashMap<>();
        for (DataSetForChart d : dataSet) {
            styleByKey.putIfAbsent(d.getKey(), d);
        }
        int i = 0;
        for (Object rowKey : dataset.getRowKeys()) {
            final DataSetForChart s = styleByKey.get((String) rowKey);
            if (s != null) {
                plot.setSeriesPaint(i, s.getColor());
                plot.setSeriesOutlineStroke(i, new BasicStroke(s.isEmphasis() ? 3.5f : 1.5f));
            }
            i++;
        }

        final JFreeChart chart = new JFreeChart(getTitle(), JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        if (caveat != null && !caveat.isEmpty()) {
            final TextTitle note = new TextTitle(this.caveat, new Font("SansSerif", Font.ITALIC, 9));
            note.setPaint(java.awt.Color.GRAY);
            note.setPosition(RectangleEdge.BOTTOM);
            note.setHorizontalAlignment(HorizontalAlignment.LEFT);
            chart.addSubtitle(note);
        }
        if (subtitle != null) {
            final TextTitle source = new TextTitle(this.subtitle, new Font("SansSerif", Font.PLAIN, 10));
            source.setPosition(RectangleEdge.BOTTOM);
            source.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            chart.addSubtitle(source);
        }
        return chart;
    }
}
