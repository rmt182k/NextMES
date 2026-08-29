package my.id.roytamba.nextmes.module.generator.grid.controller;

import java.util.ArrayList;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class GridGeneratorController {

    @FXML private Spinner<Integer> spinnerCols;
    @FXML private HBox columnsContainer;
    @FXML private ComboBox<String> cmbFormat;
    @FXML private TextArea txtOutput;
    
    private List<TextField> headerFields = new ArrayList<>();
    private List<TextArea> dataAreas = new ArrayList<>();

    @FXML
    public void initialize() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2);
        spinnerCols.setValueFactory(valueFactory);
        
        cmbFormat.getItems().addAll("JSON (Array of Objects)", "Array 2D (List of Lists)");
        cmbFormat.getSelectionModel().selectFirst();
        
        handleCreateColumns(null);
    }

    @FXML
    public void handleCreateColumns(ActionEvent event) {
        columnsContainer.getChildren().clear();
        headerFields.clear();
        dataAreas.clear();
        
        int cols = spinnerCols.getValue();
        
        for (int i = 0; i < cols; i++) {
            VBox colBox = new VBox(5);
            HBox.setHgrow(colBox, Priority.ALWAYS);
            
            TextField txtHeader = new TextField("Kolom " + (i + 1));
            txtHeader.setFont(Font.font("Segoe UI", 13));
            txtHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-radius: 4;");
            
            TextArea txtData = new TextArea();
            txtData.setFont(Font.font("Consolas", 13));
            txtData.setPromptText("Paste/ketik data di sini...");
            VBox.setVgrow(txtData, Priority.ALWAYS);
            
            headerFields.add(txtHeader);
            dataAreas.add(txtData);
            
            colBox.getChildren().addAll(txtHeader, txtData);
            columnsContainer.getChildren().add(colBox);
        }
    }

    @FXML
    public void handleClearData(ActionEvent event) {
        for (TextArea area : dataAreas) {
            area.clear();
        }
        txtOutput.clear();
    }

    @FXML
    public void handleGenerate(ActionEvent event) {
        if (dataAreas.isEmpty()) return;
        
        // Find max lines across all text areas
        List<String[]> allData = new ArrayList<>();
        int maxLines = 0;
        
        for (TextArea area : dataAreas) {
            String text = area.getText();
            if (text.isEmpty()) {
                allData.add(new String[0]);
            } else {
                String[] lines = text.split("\\R");
                allData.add(lines);
                if (lines.length > maxLines) {
                    maxLines = lines.length;
                }
            }
        }
        
        if (maxLines == 0) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Semua kolom data masih kosong!");
            return;
        }
        
        String format = cmbFormat.getValue();
        StringBuilder sb = new StringBuilder();
        
        if ("JSON (Array of Objects)".equals(format)) {
            sb.append("[\n");
            for (int r = 0; r < maxLines; r++) {
                sb.append("  {");
                for (int c = 0; c < headerFields.size(); c++) {
                    String header = escapeJson(headerFields.get(c).getText().trim());
                    String[] lines = allData.get(c);
                    String value = "";
                    if (r < lines.length) {
                        value = escapeJson(lines[r].trim());
                    }
                    
                    sb.append("\"").append(header).append("\": \"").append(value).append("\"");
                    if (c < headerFields.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("}");
                if (r < maxLines - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("\n]");
            
        } else if ("Array 2D (List of Lists)".equals(format)) {
            sb.append("[\n");
            for (int r = 0; r < maxLines; r++) {
                sb.append("  [");
                for (int c = 0; c < headerFields.size(); c++) {
                    String[] lines = allData.get(c);
                    String value = "";
                    if (r < lines.length) {
                        value = escapeJson(lines[r].trim());
                    }
                    
                    sb.append("\"").append(value).append("\"");
                    if (c < headerFields.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("]");
                if (r < maxLines - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("]");
        }
        
        txtOutput.setText(sb.toString());
    }
    
    @FXML
    public void handleCopy(ActionEvent event) {
        String text = txtOutput.getText();
        if (text != null && !text.isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data hasil generate berhasil disalin ke Clipboard!");
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
