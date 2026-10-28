package gui.services;

import java.awt.Component;
import java.util.function.Function;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Website-supplied text is always displayed as text, never as Swing HTML. */
public final class WebsiteResponseMessages {
    private WebsiteResponseMessages() { }

    public static String format(Function<String, String> labels, String message) {
        return labels.apply("WEBSITE.HTML.MESSAGE") + "\n"
                + (message == null || message.isEmpty() ? labels.apply("WEBSITE.HTML.EMPTY") : message);
    }

    public static void show(Component parent, Function<String, String> labels, String message) {
        JOptionPane.showMessageDialog(parent, content(format(labels, message)),
                labels.apply("WEBSITE.HTML.TITLE"), JOptionPane.WARNING_MESSAGE);
    }

    static JScrollPane content(String message) {
        JTextArea text = new JTextArea(message, 8, 56);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new JLabel().getFont());
        return new JScrollPane(text);
    }
}
