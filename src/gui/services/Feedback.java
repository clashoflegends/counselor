package gui.services;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ResourceBundle;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** Durable operation feedback. Recording never opens a dialog or takes keyboard focus. */
public final class Feedback {
    /** In-view download progress; Swing's ProgressMonitor would open a modal window after a delay. */
    public static final class Progress {
        private final Component parent;
        private final String title;
        private final JPanel panel = new JPanel(new BorderLayout(0, 8));
        private final javax.swing.JProgressBar bar = new javax.swing.JProgressBar(0, 100);
        private final JLabel note = new JLabel();
        private boolean recorded;

        public Progress(Component parent, String title) {
            this.parent = parent;
            this.title = title;
            bar.setStringPainted(true);
            note.putClientProperty("html.disable", Boolean.TRUE);
            panel.add(note, BorderLayout.NORTH);
            panel.add(bar, BorderLayout.CENTER);
        }

        public void setProgress(int percent) {
            bar.setValue(percent);
            if (!recorded && percent > 0) {
                recorded = true;
                report(parent, title, panel);
            }
        }

        public void setNote(String message) { note.setText(message); }
    }
    private static final DefaultListModel<Entry> ENTRIES = new DefaultListModel<>();
    private static JDialog history;
    private static JList<Entry> list;
    private static JPanel detail;

    private record Entry(String title, JComponent content, java.time.LocalTime time) {
        @Override public String toString() { return time.withNano(0) + "  " + title; }
    }

    private Feedback() { }

    public static String text(String key) {
        String language = persistenceCommons.SettingsManager.getInstance().getConfig("language", "en");
        return ResourceBundle.getBundle("gui.services.feedback", java.util.Locale.forLanguageTag(language)).getString(key);
    }

    public static JPanel actions(String message, String[] captions, Runnable... actions) {
        java.util.function.BooleanSupplier[] choices = new java.util.function.BooleanSupplier[actions.length];
        for (int i = 0; i < actions.length; i++) {
            Runnable action = actions[i];
            choices[i] = () -> { action.run(); return true; };
        }
        return choices(message, captions, choices);
    }

    /** Returning false means the gesture was cancelled; keep the recovery choices usable. */
    public static JPanel choices(String message, String[] captions, java.util.function.BooleanSupplier... actions) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JScrollPane(message(message)), BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        for (int i = 0; i < captions.length; i++) {
            javax.swing.JButton button = new javax.swing.JButton(captions[i]);
            java.util.function.BooleanSupplier action = actions[i];
            button.addActionListener(e -> {
                for (Component sibling : buttons.getComponents()) sibling.setEnabled(false);
                boolean consumed = false;
                try { consumed = action.getAsBoolean(); }
                finally {
                    if (!consumed) for (Component sibling : buttons.getComponents()) sibling.setEnabled(true);
                }
            });
            buttons.add(button);
        }
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    public static JTextArea message(String message) {
        JTextArea text = new JTextArea(message == null ? "" : message, 8, 52);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new JLabel().getFont());
        text.setCaretPosition(0);
        return text;
    }

    public static void report(Component parent, String title, String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> report(parent, title, message));
            return;
        }
        report(parent, title, new JScrollPane(message(message)));
    }

    public static void report(Component parent, String title, JComponent content) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> report(parent, title, content));
            return;
        }
        Entry selected = list == null ? null : list.getSelectedValue();
        Entry entry = new Entry(title, content, java.time.LocalTime.now());
        ENTRIES.add(0, entry);
        if (list != null) list.setSelectedIndex(selected == null ? 0 : ENTRIES.indexOf(selected));
        // Deliberately retain operation results for this session: a later toast must not erase an
        // earlier unsaved-file or uncertain-delivery recovery. No credentials or messages are logged.
        Window owner = owner(parent);
        Toast.show(owner, title + "\n" + text("details"), null, () -> open(owner, entry));
    }

    public static Window owner(Component parent) {
        Window window = parent instanceof Window w ? w
                : parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (window == null) {
            for (java.awt.Frame frame : java.awt.Frame.getFrames()) {
                if (frame.isShowing()) { window = frame; break; }
            }
        }
        while (window != null && window.getOwner() != null) window = window.getOwner();
        return window;
    }

    /** Explicitly opened from the status label or a notification; one modeless view per session. */
    public static void open(Component parent) {
        open(parent, null);
    }

    private static void open(Component parent, Entry requested) {
        if (history != null && history.isDisplayable()) {
            if (requested != null) list.setSelectedValue(requested, true);
            history.setVisible(true);
            history.toFront();
            return;
        }
        history = new JDialog(owner(parent), text("history"), java.awt.Dialog.ModalityType.MODELESS);
        AppIcon.applyTo(history);
        history.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        list = new JList<>(ENTRIES);
        list.setCellRenderer((view, entry, index, selected, focus) -> {
            JLabel label = new JLabel();
            label.putClientProperty("html.disable", Boolean.TRUE);
            label.setText(entry.toString());
            label.setOpaque(true);
            label.setBackground(selected ? view.getSelectionBackground() : view.getBackground());
            label.setForeground(selected ? view.getSelectionForeground() : view.getForeground());
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
            return label;
        });
        detail = new JPanel(new BorderLayout());
        final JList<Entry> entriesView = list;
        final JPanel detailView = detail;
        final JDialog window = history;
        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            detailView.removeAll();
            Entry selected = entriesView.getSelectedValue();
            if (selected != null) detailView.add(selected.content(), BorderLayout.CENTER);
            detailView.revalidate();
            detailView.repaint();
        });
        history.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                entriesView.setModel(new DefaultListModel<>()); // detach the disposed view from session history
                if (history == window) { history = null; list = null; detail = null; }
            }
        });
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), detail);
        split.setDividerLocation(240);
        history.add(split);
        history.setSize(new Dimension(840, 440));
        history.setLocationRelativeTo(parent);
        if (!ENTRIES.isEmpty()) list.setSelectedIndex(requested == null ? 0 : ENTRIES.indexOf(requested));
        history.setVisible(true);
    }
}
