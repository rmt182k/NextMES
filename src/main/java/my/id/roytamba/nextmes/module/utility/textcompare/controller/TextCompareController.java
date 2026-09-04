package my.id.roytamba.nextmes.module.utility.textcompare.controller;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebView;
import java.util.Arrays;
import java.util.List;

public class TextCompareController {

    @FXML private ComboBox<String> cmbPrecision;
    @FXML private ComboBox<String> cmbLayout;
    @FXML private TextArea txtOriginal;
    @FXML private TextArea txtModified;
    @FXML private WebView webViewResult;

    @FXML
    public void initialize() {
        cmbPrecision.getItems().addAll("Smart", "Word", "Character");
        cmbPrecision.getSelectionModel().select("Smart");

        cmbLayout.getItems().addAll("Split", "Unified");
        cmbLayout.getSelectionModel().select("Split");
    }

    @FXML
    public void handleCompare() {
        String originalText = txtOriginal.getText();
        String modifiedText = txtModified.getText();

        if (originalText == null) originalText = "";
        if (modifiedText == null) modifiedText = "";

        List<String> originalLines = Arrays.asList(originalText.split("\n", -1));
        List<String> modifiedLines = Arrays.asList(modifiedText.split("\n", -1));

        String precision = cmbPrecision.getValue();
        boolean isSplit = "Split".equals(cmbLayout.getValue());

        DiffRowGenerator.Builder builder = DiffRowGenerator.create()
                .showInlineDiffs(true)
                .inlineDiffByWord("Word".equals(precision))
                .oldTag(f -> f ? "<del style='background:#ffb3b3; text-decoration:none;'>" : "</del>")
                .newTag(f -> f ? "<ins style='background:#b3ffb3; text-decoration:none;'>" : "</ins>");

        if ("Character".equals(precision)) {
            // DiffRowGenerator doesn't have inlineDiffByCharacter explicitly, 
            // but setting inlineDiffByWord(false) defaults to character-like behavior in some versions,
            // or we just rely on the default character diff.
            builder.inlineDiffByWord(false);
        } else if ("Smart".equals(precision)) {
            builder.inlineDiffByWord(true); // Treat smart as word for now, or mix
        }

        DiffRowGenerator generator = builder.build();
        List<DiffRow> rows;
        try {
            rows = generator.generateDiffRows(originalLines, modifiedLines);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
            .append("body { font-family: monospace; font-size: 14px; margin: 0; padding: 10px; }")
            .append("table { width: 100%; border-collapse: collapse; }")
            .append("td { padding: 4px; vertical-align: top; border-bottom: 1px solid #eee; white-space: pre-wrap; }")
            .append(".line-num { width: 40px; color: #999; text-align: right; padding-right: 10px; user-select: none; border-right: 1px solid #ddd; }")
            .append(".del-row { background-color: #ffe6e6; }")
            .append(".ins-row { background-color: #e6ffe6; }")
            .append("</style></head><body>");

        if (isSplit) {
            html.append("<table>");
            html.append("<tr><th colspan='2'>Original</th><th colspan='2'>Modified</th></tr>");
            int lineOld = 1;
            int lineNew = 1;
            for (DiffRow row : rows) {
                String left = row.getOldLine();
                String right = row.getNewLine();
                
                String rowClass = "";
                if (row.getTag() == DiffRow.Tag.DELETE) rowClass = "del-row";
                else if (row.getTag() == DiffRow.Tag.INSERT) rowClass = "ins-row";
                else if (row.getTag() == DiffRow.Tag.CHANGE) rowClass = "change-row";

                html.append("<tr class='").append(rowClass).append("'>");
                
                // Left side
                if (row.getTag() == DiffRow.Tag.INSERT) {
                    html.append("<td class='line-num'></td><td></td>");
                } else {
                    html.append("<td class='line-num'>").append(lineOld++).append("</td>");
                    html.append("<td>").append(left).append("</td>");
                }
                
                // Right side
                if (row.getTag() == DiffRow.Tag.DELETE) {
                    html.append("<td class='line-num'></td><td></td>");
                } else {
                    html.append("<td class='line-num'>").append(lineNew++).append("</td>");
                    html.append("<td>").append(right).append("</td>");
                }
                html.append("</tr>");
            }
            html.append("</table>");
        } else {
            // Unified Layout
            html.append("<table>");
            int lineOld = 1;
            int lineNew = 1;
            for (DiffRow row : rows) {
                String rowClass = "";
                if (row.getTag() == DiffRow.Tag.EQUAL) {
                    html.append("<tr>");
                    html.append("<td class='line-num'>").append(lineOld++).append("</td>");
                    html.append("<td class='line-num'>").append(lineNew++).append("</td>");
                    html.append("<td>").append(row.getOldLine()).append("</td>");
                    html.append("</tr>");
                } else if (row.getTag() == DiffRow.Tag.DELETE) {
                    html.append("<tr class='del-row'>");
                    html.append("<td class='line-num'>").append(lineOld++).append("</td>");
                    html.append("<td class='line-num'></td>");
                    html.append("<td>").append(row.getOldLine()).append("</td>");
                    html.append("</tr>");
                } else if (row.getTag() == DiffRow.Tag.INSERT) {
                    html.append("<tr class='ins-row'>");
                    html.append("<td class='line-num'></td>");
                    html.append("<td class='line-num'>").append(lineNew++).append("</td>");
                    html.append("<td>").append(row.getNewLine()).append("</td>");
                    html.append("</tr>");
                } else if (row.getTag() == DiffRow.Tag.CHANGE) {
                    // Show deletion first, then insertion
                    html.append("<tr class='del-row'>");
                    html.append("<td class='line-num'>").append(lineOld++).append("</td>");
                    html.append("<td class='line-num'></td>");
                    html.append("<td>").append(row.getOldLine()).append("</td>");
                    html.append("</tr>");

                    html.append("<tr class='ins-row'>");
                    html.append("<td class='line-num'></td>");
                    html.append("<td class='line-num'>").append(lineNew++).append("</td>");
                    html.append("<td>").append(row.getNewLine()).append("</td>");
                    html.append("</tr>");
                }
            }
            html.append("</table>");
        }
        
        html.append("</body></html>");
        webViewResult.getEngine().loadContent(html.toString());
    }

    @FXML
    public void handleClear() {
        txtOriginal.clear();
        txtModified.clear();
        webViewResult.getEngine().loadContent("");
    }
}
