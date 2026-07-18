package gui.charts;

import business.ImageManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * "What should I grow?" (N2): a single-series HORIZONTAL bar of your nation on each power lever expressed
 * as a percent of the GAME AVERAGE (100 = average). A dashed baseline marks 100%, so a newbie reads at a
 * glance where they are ahead of / behind the field. Levers that matter for THIS game's active victory
 * conditions are drawn in a strong colour (emphasis); the rest are muted grey, teaching which stat to
 * chase for the win. One {@link DataSetForChart} per lever: grouping = lever, value = % of average,
 * color = the bar colour, emphasis = "priority for this game's victory".
 */
public final class ChartGrowth extends JFrame {

    private static final String SERIES = "you";   // single series; the key is never shown (legend off)

    private final List<DataSetForChart> dataSet;
    private final String subtitle;
    private final String caveat;
    private final String valueAxisLabel;
    private final String baselineLabel;

    public ChartGrowth(String title, List<DataSetForChart> dataSet, String subtitle, String caveat,
            String valueAxisLabel, String baselineLabel) {
        super(title);
        setIconImage(ImageManager.getInstance().getBarChartIcon());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.dataSet = dataSet;
        this.subtitle = subtitle;
        this.caveat = caveat;
        this.valueAxisLabel = valueAxisLabel;
        this.baselineLabel = baselineLabel;
    }

    /** For parity with the ComponentFactory.showChart* lifecycle. */
    public void doStart() {
        final JFreeChart chart = createChart(createDataset());
        final ChartPanel panel = new ChartPanel(chart, false);
        panel.setPreferredSize(new Dimension(560, 360));
        setContentPane(panel);
    }

    private CategoryDataset createDataset() {
        final DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (DataSetForChart item : dataSet) {
            ds.addValue(item.getValue(), SERIES, item.getGrouping());
        }
        return ds;
    }

    private JFreeChart createChart(CategoryDataset dataset) {
        final JFreeChart chart = org.jfree.chart.ChartFactory.createBarChart(
                getTitle(), "", valueAxisLabel, dataset, PlotOrientation.HORIZONTAL,
                false, true, false);
        chart.setBackgroundPaint(Color.WHITE);

        final CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setRangeGridlinesVisible(true);

        // per-lever colour: priority levers (emphasis) get their colour, the rest are muted grey.
        final Map<String, Paint> paintByLever = new LinkedHashMap<>();
        for (DataSetForChart d : dataSet) {
            paintByLever.put(d.getGrouping(), d.isEmphasis() ? d.getColor() : new Color(190, 190, 190));
        }
        final BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                final Object lever = getPlot().getDataset().getColumnKey(column);
                final Paint p = paintByLever.get(String.valueOf(lever));
                return p != null ? p : super.getItemPaint(row, column);
            }
        };
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setDefaultToolTipGenerator(
                new org.jfree.chart.labels.StandardCategoryToolTipGenerator());
        plot.setRenderer(renderer);

        // "game average" baseline at 100%.
        final ValueMarker avg = new ValueMarker(100.0);
        avg.setPaint(Color.DARK_GRAY);
        avg.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                1f, new float[]{6f, 4f}, 0f));
        avg.setLabel(baselineLabel);
        avg.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        avg.setLabelPaint(Color.DARK_GRAY);
        avg.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        avg.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
        plot.addRangeMarker(avg, Layer.FOREGROUND);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setLowerBound(0.0);

        if (caveat != null && !caveat.isEmpty()) {
            final TextTitle note = new TextTitle(this.caveat, new Font("SansSerif", Font.ITALIC, 9));
            note.setPaint(Color.GRAY);
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
