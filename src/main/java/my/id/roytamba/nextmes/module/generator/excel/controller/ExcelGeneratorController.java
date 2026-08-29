package my.id.roytamba.nextmes.module.generator.excel.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class ExcelGeneratorController {

    @FXML private TableView<ObservableList<String>> tableData;
    @FXML private CheckBox chkFirstRowHeader;
    @FXML private TextArea txtTemplate;
    @FXML private TextArea txtOutput;

    private ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
    private ObservableList<String> currentHeaders = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        tableData.setItems(data);
        tableData.setPlaceholder(new Label("Paste data Excel Anda di sini (Klik lalu tekan Ctrl+V)"));

        tableData.setOnKeyPressed(this::handleTableKeyPress);
    }

    private void handleTableKeyPress(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.V) {
            handlePaste();
        }
    }

    @FXML
    public void handlePaste() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            String clipString = clipboard.getString();
            parseClipboardData(clipString);
        }
    }

    private void parseClipboardData(String text) {
        tableData.getColumns().clear();
        data.clear();
        currentHeaders.clear();

        if (text == null || text.isEmpty()) return;

        String[] rows = text.split("\\R");
        if (rows.length == 0) return;

        boolean isHeader = chkFirstRowHeader.isSelected();
        int startIndex = 0;
        
        String[] firstRowCols = rows[0].split("\t", -1);
        int colCount = firstRowCols.length;

        for (int i = 0; i < colCount; i++) {
            final int colIndex = i;
            String headerName = isHeader ? firstRowCols[i] : "Kolom " + (i + 1);
            currentHeaders.add(headerName);
            
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(headerName);
            column.setCellValueFactory(param -> {
                if (colIndex < param.getValue().size()) {
                    return new SimpleStringProperty(param.getValue().get(colIndex));
                } else {
                    return new SimpleStringProperty("");
                }
            });
            tableData.getColumns().add(column);
        }

        if (isHeader) {
            startIndex = 1;
        }

        for (int i = startIndex; i < rows.length; i++) {
            String[] cells = rows[i].split("\t", -1);
            if (cells.length == 1 && cells[0].trim().isEmpty()) {
                continue; // Abaikan baris kosong di akhir
            }
            
            ObservableList<String> rowData = FXCollections.observableArrayList();
            for (String cell : cells) {
                rowData.add(cell);
            }
            data.add(rowData);
        }
    }

    @FXML
    public void handleClear() {
        tableData.getColumns().clear();
        data.clear();
        currentHeaders.clear();
        txtOutput.clear();
    }

    @FXML
    public void handleGenerate() {
        String template = txtTemplate.getText();
        if (template == null || template.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Format Template tidak boleh kosong!");
            return;
        }

        if (data.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Data tabel masih kosong!");
            return;
        }

        StringBuilder result = new StringBuilder();

        for (ObservableList<String> row : data) {
            String rowResult = template;
            for (int i = 0; i < currentHeaders.size(); i++) {
                String header = currentHeaders.get(i);
                String value = i < row.size() ? row.get(i) : "";
                
                // Case insensitive replace bisa dilakukan jika diperlukan, tapi exact match lebih aman
                rowResult = rowResult.replace("{" + header + "}", value);
            }
            result.append(rowResult).append("\n");
        }

        txtOutput.setText(result.toString());
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
