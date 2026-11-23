package coderoom.widgets.clue;

import coderoom.shared.ui.SpecimenMark;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;

/** Formats stored notes as readable paragraphs and separate Java samples. */
final class ClueReading extends JEditorPane {
    private static final Pattern JAVA_LINE = Pattern.compile(
            "(?m)^\\s*(?:class\\s+|[{}]|(?:if|for|while)\\s*\\(|(?:int|boolean|String|char|double|var)\\b[ \\t\\[]).*|;\\s*$");
    private static final Pattern TOKEN = Pattern.compile(
            "//[^\\n]*|'[^'\\n]'|\\[[^]\\n]+]|\\b(?:int|boolean|String|char|double|var|if|else|for|while|return|continue|true|false|null|new)\\b|\\b\\d+\\b");

    private String note = "";
    private int renderedSize = -1;
    private int renderedWidth = -1;

    ClueReading() {
        setEditorKit(new HTMLEditorKit() {
            @Override public ViewFactory getViewFactory() {
                ViewFactory base = super.getViewFactory();
                return (Element element) -> {
                    View view = base.create(element);
                    if (view instanceof ImageView image) { image.setLoadsSynchronously(true); }
                    return view;
                };
            }
        });
        setEditable(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder());
        setOpaque(false);
        getAccessibleContext().setAccessibleName("메모 본문");
    }

    void showNote(String text) {
        if (!note.equals(text)) {
            note         = text;
            renderedSize = -1;
        }
    }

    int fit(int width, int limit) {
        int height = 0;
        for (int size = 17; size >= 14; size--) {
            if (renderedSize != size || renderedWidth != width) { showHtml(size, width); }
            View root = getUI().getRootView(this);
            root.setSize(Math.max(1, width), 10_000);
            height = (int) Math.ceil(root.getPreferredSpan(View.Y_AXIS));
            if (height <= limit && root.getMinimumSpan(View.X_AXIS) <= width) { break; }
        }
        return height;
    }

    private void showHtml(int size, int width) {
        renderedSize  = size;
        renderedWidth = width;
        var codeFont = new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, size - 1);
        int columns  = Math.max(16, (width - 30) / getFontMetrics(codeFont).charWidth('m'));
        String[] sections = note.strip().split("\\R[ \\t]*\\R");
        var html = new StringBuilder("""
                <html><head><style>
                body {color:#382315;font-family:sans-serif;font-size:%dpt;margin:6px;}
                p {margin:0 0 7px 0;}
                pre {background-color:#1e1814;color:#fff0d3;font-family:monospaced;font-size:%dpt;padding:7px;margin:0 0 7px 0;}
                </style></head><body>
                """.formatted(size, size - 1));
        for (String section : sections) {
            if (JAVA_LINE.matcher(section).find()) {
                html.append("<pre>").append(highlight(wrapCode(section, columns))).append("</pre>");
            } else {
                html.append("<p>").append(escape(section).replace("\n", "<br>")).append("</p>");
            }
        }
        setText(html.append("</body></html>").toString());
        setCaretPosition(0);
    }

    private static String wrapCode(String code, int columns) {
        var wrapped = new StringBuilder();
        for (String line : code.split("\\R")) {
            if (!wrapped.isEmpty()) { wrapped.append('\n'); }
            String indent = " ".repeat(line.length() - line.stripLeading().length() + 4);
            while (line.length() > columns) {
                int boundary = Math.max(line.lastIndexOf(' ', columns), line.lastIndexOf('(', columns - 1) + 1);
                if (boundary <= indent.length() || line.contains("'") || line.contains("\"")) { break; }
                wrapped.append(line, 0, boundary).append('\n');
                line = indent + line.substring(boundary).stripLeading();
            }
            wrapped.append(line);
        }
        return wrapped.toString();
    }

    private static String highlight(String code) {
        var tokens = TOKEN.matcher(code);
        var html   = new StringBuilder();
        int offset = 0;
        while (tokens.find()) {
            String token = tokens.group();
            html.append(escape(code.substring(offset, tokens.start())));
            SpecimenMark mark = token.startsWith("'") ? SpecimenMark.fromSymbol(token.charAt(1)) : null;
            if (mark != null) {
                html.append("'<img src=\"").append(ClueReading.class.getResource(mark.imagePath()))
                        .append("\" width=\"16\" height=\"16\" align=\"middle\" alt=\"")
                        .append(mark.description()).append("\">'");
            } else {
                String color = token.startsWith("//") ? "#c9bca4" : token.startsWith("[") ? "#ffcc88" : "#a9dad3";
                html.append("<span style='color:").append(color).append("'>").append(escape(token)).append("</span>");
            }
            offset = tokens.end();
        }
        return html.append(escape(code.substring(offset))).toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
