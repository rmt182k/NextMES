package my.id.roytamba.nextmes.module.generator.grid.controller;

import java.util.ArrayList;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Optional;
import javafx.scene.layout.Priority;

public class GridGeneratorController {

    @FXML private Spinner<Integer> spinnerCols;
    @FXML private HBox columnsContainer;
    @FXML private ComboBox<String> cmbFormat;
    @FXML private TextArea txtOutput;
    @FXML private ComboBox<String> cmbTemplate;
    
    private List<TextField> headerFields = new ArrayList<>();
    private List<TextArea> dataAreas = new ArrayList<>();
    
    private final String TEMPLATE_PROPS_PATH = "src/main/resources/grid_templates.properties";
    private Properties templateProps = new Properties();

    @FXML
    public void initialize() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2);
        spinnerCols.setValueFactory(valueFactory);
        
        cmbFormat.getItems().addAll("JSON (Array of Objects)", "Array 2D (List of Lists)", "CSV (Comma Separated Values)");
        cmbFormat.getSelectionModel().selectFirst();
        
        loadTemplates();
        cmbTemplate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals("-- Template Baru --")) {
                loadTemplateSettings(newVal);
            }
        });
        
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
        } else if ("CSV (Comma Separated Values)".equals(format)) {
            for (int c = 0; c < headerFields.size(); c++) {
                sb.append("\"").append(escapeCsv(headerFields.get(c).getText().trim())).append("\"");
                if (c < headerFields.size() - 1) sb.append(",");
            }
            sb.append("\n");
            for (int r = 0; r < maxLines; r++) {
                for (int c = 0; c < headerFields.size(); c++) {
                    String[] lines = allData.get(c);
                    String value = "";
                    if (r < lines.length) {
                        value = escapeCsv(lines[r].trim());
                    }
                    sb.append("\"").append(value).append("\"");
                    if (c < headerFields.size() - 1) sb.append(",");
                }
                sb.append("\n");
            }
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

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private String escapeCsv(String raw) {
        if (raw == null) return "";
        return raw.replace("\"", "\"\"");
    }
    
    private void loadTemplates() {
        cmbTemplate.getItems().clear();
        cmbTemplate.getItems().add("-- Template Baru --");
        File file = new File(TEMPLATE_PROPS_PATH);
        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {
            templateProps.load(fis);
            List<String> templates = templateProps.stringPropertyNames().stream()
                .filter(k -> k.endsWith(".cols"))
                .map(k -> k.substring(0, k.length() - 5))
                .distinct()
                .toList();
            cmbTemplate.getItems().addAll(templates);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadTemplateSettings(String templateName) {
        String colsStr = templateProps.getProperty(templateName + ".cols");
        if (colsStr != null) {
            try {
                int cols = Integer.parseInt(colsStr);
                spinnerCols.getValueFactory().setValue(cols);
                handleCreateColumns(null);
                
                for (int i = 0; i < cols; i++) {
                    String header = templateProps.getProperty(templateName + ".h" + i);
                    if (header != null && i < headerFields.size()) {
                        headerFields.get(i).setText(header);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    public void handleSaveTemplate(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Simpan Template");
        dialog.setHeaderText("Simpan konfigurasi kolom saat ini sebagai Template");
        dialog.setContentText("Nama Template:");

        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Peringatan", "Nama template tidak boleh kosong!");
                return;
            }
            
            String templateName = name.trim();
            int cols = spinnerCols.getValue();
            
            templateProps.setProperty(templateName + ".cols", String.valueOf(cols));
            for (int i = 0; i < cols; i++) {
                if (i < headerFields.size()) {
                    templateProps.setProperty(templateName + ".h" + i, headerFields.get(i).getText());
                }
            }
            
            try (FileOutputStream fos = new FileOutputStream(TEMPLATE_PROPS_PATH)) {
                templateProps.store(fos, "Grid Generator Templates");
                if (!cmbTemplate.getItems().contains(templateName)) {
                    cmbTemplate.getItems().add(templateName);
                }
                cmbTemplate.setValue(templateName);
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Template '" + templateName + "' berhasil disimpan!");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan template!");
            }
        });
    }

    @FXML
    public void handleDeleteTemplate(ActionEvent event) {
        String selected = cmbTemplate.getValue();
        if (selected == null || selected.equals("-- Template Baru --")) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih template yang ingin dihapus terlebih dahulu!");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Hapus");
        confirm.setHeaderText("Hapus Template");
        confirm.setContentText("Apakah Anda yakin ingin menghapus template '" + selected + "'?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            templateProps.keySet().removeIf(k -> k.toString().startsWith(selected + "."));
            try (FileOutputStream fos = new FileOutputStream(TEMPLATE_PROPS_PATH)) {
                templateProps.store(fos, "Grid Generator Templates");
                cmbTemplate.getItems().remove(selected);
                cmbTemplate.setValue("-- Template Baru --");
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Template berhasil dihapus!");
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    public void handleRenameTemplate(ActionEvent event) {
        String selected = cmbTemplate.getValue();
        if (selected == null || selected.equals("-- Template Baru --")) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih template yang ingin diganti namanya!");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog(selected);
        dialog.setTitle("Ganti Nama Template");
        dialog.setHeaderText("Ubah nama template '" + selected + "'");
        dialog.setContentText("Nama Baru:");
        
        dialog.showAndWait().ifPresent(newName -> {
            if (newName.trim().isEmpty() || newName.equals(selected)) return;
            
            String newTemplateName = newName.trim();
            List<String> keysToMigrate = templateProps.stringPropertyNames().stream()
                .filter(k -> k.startsWith(selected + "."))
                .toList();
                
            for (String key : keysToMigrate) {
                String val = templateProps.getProperty(key);
                String newKey = newTemplateName + key.substring(selected.length());
                templateProps.setProperty(newKey, val);
                templateProps.remove(key);
            }
            
            try (FileOutputStream fos = new FileOutputStream(TEMPLATE_PROPS_PATH)) {
                templateProps.store(fos, "Grid Generator Templates");
                int index = cmbTemplate.getItems().indexOf(selected);
                if (index >= 0) {
                    cmbTemplate.getItems().set(index, newTemplateName);
                    cmbTemplate.setValue(newTemplateName);
                }
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Template berhasil diubah namanya!");
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @FXML
    public void handleSaveToFile(ActionEvent event) {
        String text = txtOutput.getText();
        if (text == null || text.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Tidak ada data untuk disimpan! Lakukan Generate terlebih dahulu.");
            return;
        }
        
        String format = cmbFormat.getValue();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan Data");
        
        if ("CSV (Comma Separated Values)".equals(format)) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        } else if ("JSON (Array of Objects)".equals(format)) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        } else {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        
        File file = fileChooser.showSaveDialog(txtOutput.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.print(text);
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data berhasil disimpan ke " + file.getName());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan file!");
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
