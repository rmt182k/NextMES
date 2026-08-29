package my.id.roytamba.nextmes.module.generator.formatter.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class WoFormatterController {

    @FXML private TextArea txtInput;
    @FXML private TextArea txtOutput;
    @FXML private ComboBox<String> cmbBracket;

    @FXML
    public void initialize() {
        cmbBracket.getItems().addAll(
                "( ) - Parentheses",
                "[ ] - Brackets",
                "{ } - Braces"
        );
        cmbBracket.setValue("( ) - Parentheses");
    }

    @FXML
    public void handleGenerate(ActionEvent event) {
        String inputText = txtInput.getText();
        if (inputText == null || inputText.trim().isEmpty()) {
            txtOutput.setText("");
            return;
        }

        String[] lines = inputText.split("\\R");
        StringBuilder result = new StringBuilder();

        // Tentukan karakter pembuka dan penutup
        String bracketChoice = cmbBracket.getValue();
        String openBracket = "(";
        String closeBracket = ")";
        
        if (bracketChoice.startsWith("[")) {
            openBracket = "[";
            closeBracket = "]";
        } else if (bracketChoice.startsWith("{")) {
            openBracket = "{";
            closeBracket = "}";
        }

        result.append(openBracket).append("\n");

        boolean first = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (!first) {
                result.append(",\n");
            }

            // Indentasi dan penambahan kutip tunggal
            result.append("    '").append(trimmed).append("'");
            first = false;
        }

        result.append("\n").append(closeBracket);
        
        txtOutput.setText(result.toString());
    }

    @FXML
    public void handleCopy(ActionEvent event) {
        String outputText = txtOutput.getText();
        if (outputText == null || outputText.isEmpty()) {
            return;
        }

        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(outputText);
        clipboard.setContent(content);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sukses");
        alert.setHeaderText(null);
        alert.setContentText("Hasil generate berhasil disalin ke Clipboard!");
        alert.showAndWait();
    }
}
