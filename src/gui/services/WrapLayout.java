package gui.services;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * A {@link FlowLayout} that reports the height it will actually need once it has wrapped.
 *
 * <h3>The bug this exists for</h3>
 *
 * {@code FlowLayout} DOES wrap its children onto extra rows when it is laid out narrower than they
 * fit - but {@code preferredLayoutSize} answers for a SINGLE row regardless. So a container using it
 * asks its parent for one row's height, gets it, and then draws two rows into it. Inside a
 * {@code BorderLayout} the result is worse than clipping: {@code LINE_START} is given its full
 * preferred width and {@code LINE_END} is still pinned to the right edge, so the two regions
 * intersect and whichever was added first paints over the other.
 *
 * That is exactly what the BattleSim toolbar did. Below about 912 logical pixels the army buttons
 * overlapped the Run and Results pair and, being added first, hid them - and the window's own
 * minimum width was 860, so a player could reach that state on any monitor, at any scale, simply by
 * dragging the window narrow. Measured overlap was 61x22 pixels at 860 and 194x22 at 683.
 *
 * <h3>Why a layout and not a bigger minimum size</h3>
 *
 * Forcing the window wide enough for one row is the fix that stops working the moment somebody adds
 * a button, and it is unavailable anyway on a 1366x768 screen at 150 percent scaling, where the
 * whole window has to fit in 911 logical pixels. A toolbar that takes a second row when it needs one
 * costs 22 pixels of height in the rare case and nothing in the common one.
 *
 * This is the well-known WrapLayout pattern (Rob Camick), kept minimal: the only thing it adds to
 * {@code FlowLayout} is an honest answer about size.
 */
public class WrapLayout extends FlowLayout {

    private static final long serialVersionUID = 1L;

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        final Dimension ret = layoutSize(target, false);
        // the vertical gap is counted once per row, and the row this returns is the LAST one, whose
        // gap the caller has already accounted for
        ret.width -= getHgap() + 1;
        return ret;
    }

    /**
     * Walks the children exactly as {@code layoutContainer} will, and returns what that costs.
     *
     * The target width is the one genuinely awkward part. A container that has never been laid out
     * has width 0, and wrapping everything onto its own row would report an absurd height; so an
     * unsized container falls back to its parent's width, and failing that to no wrapping at all,
     * which is plain FlowLayout behaviour and no worse than before.
     */
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            Container container = target;
            while (container.getSize().width == 0 && container.getParent() != null) {
                container = container.getParent();
            }
            targetWidth = container.getSize().width;
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }

            final Insets insets = target.getInsets();
            final int horizontalInsetsAndGap = insets.left + insets.right + getHgap() * 2;
            final int maxWidth = targetWidth - horizontalInsetsAndGap;

            final Dimension ret = new Dimension(0, 0);
            int rowWidth = 0, rowHeight = 0;
            for (int ii = 0; ii < target.getComponentCount(); ii++) {
                final java.awt.Component one = target.getComponent(ii);
                if (!one.isVisible()) {
                    continue;
                }
                final Dimension size = preferred ? one.getPreferredSize() : one.getMinimumSize();
                if (rowWidth + size.width > maxWidth && rowWidth != 0) {
                    addRow(ret, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth != 0) {
                    rowWidth += getHgap();
                }
                rowWidth += size.width;
                rowHeight = Math.max(rowHeight, size.height);
            }
            addRow(ret, rowWidth, rowHeight);

            ret.width += horizontalInsetsAndGap;
            ret.height += insets.top + insets.bottom + getVgap() * 2;

            // Inside a scroll pane the viewport is one pixel wider than the reported width during a
            // layout pass, which makes the last row wrap and then unwrap forever. Giving back the
            // pixel stops the loop.
            final java.awt.Component scrollPane =
                    SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (scrollPane != null && target.isValid()) {
                ret.width -= getHgap() + 1;
            }
            return ret;
        }
    }

    private void addRow(Dimension dimension, int rowWidth, int rowHeight) {
        dimension.width = Math.max(dimension.width, rowWidth);
        if (dimension.height > 0) {
            dimension.height += getVgap();
        }
        dimension.height += rowHeight;
    }
}
