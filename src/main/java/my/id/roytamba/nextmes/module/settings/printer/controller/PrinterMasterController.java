package my.id.roytamba.nextmes.module.settings.printer.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import my.id.roytamba.nextmes.module.settings.printer.model.PrinterModel;

public class PrinterMasterController {

    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextField txtName;
    @FXML private TextField txtIp;
    @FXML private TextField txtSearch;

    @FXML private TableView<PrinterModel> tablePrinter;
    @FXML private TableColumn<PrinterModel, String> colId;
    @FXML private TableColumn<PrinterModel, String> colCategory;
    @FXML private TableColumn<PrinterModel, String> colName;
    @FXML private TableColumn<PrinterModel, String> colIp;

    private final String PRINTER_PROPS_PATH = "src/main/resources/printer.properties";
    private final ObservableList<PrinterModel> printerList = FXCollections.observableArrayList();
    private Properties props = new Properties();

    @FXML
    public void initialize() {
        // Setup Kategori Default
        cmbCategory.getItems().addAll("PAINT SHOP", "BODY SHOP", "GA SHOP", "Lainnya");
        cmbCategory.setValue("PAINT SHOP");
        cmbCategory.setEditable(true);

        // Setup Binding Tabel
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colCategory.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colIp.setCellValueFactory(cellData -> cellData.getValue().ipProperty());

        // Setup FilteredList untuk fitur Smart Search
        FilteredList<PrinterModel> filteredData = new FilteredList<>(printerList, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(printer -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String[] keywords = newValue.toLowerCase().trim().split("\\s+");
                String nameLower = printer.getName().toLowerCase();
                String ipLower = printer.getIp().toLowerCase();
                String catLower = printer.getCategory().toLowerCase();

                for (String kw : keywords) {
                    if (!nameLower.contains(kw) && !ipLower.contains(kw) && !catLower.contains(kw)) {
                        return false;
                    }
                }
                return true;
            });
        });

        SortedList<PrinterModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePrinter.comparatorProperty());
        tablePrinter.setItems(sortedData);

        // Load Data Awal
        loadData();
    }

    private void loadData() {
        printerList.clear();
        File propFile = new File(PRINTER_PROPS_PATH);
        if (!propFile.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
            
            List<String> keys = props.stringPropertyNames().stream()
                    .filter(k -> k.startsWith("printer.") && k.endsWith(".category"))
                    .collect(Collectors.toList());

            for (String key : keys) {
                String idStr = key.substring(8, key.length() - 9);
                String cat = props.getProperty("printer." + idStr + ".category", "");
                String name = props.getProperty("printer." + idStr + ".name", "");
                String ip = props.getProperty("printer." + idStr + ".ip", "");
                
                printerList.add(new PrinterModel(idStr, cat, name, ip));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSave(ActionEvent event) {
        String cat = cmbCategory.getValue();
        String name = txtName.getText().trim();
        String ip = txtIp.getText().trim();

        if (name.isEmpty() || ip.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Nama Printer dan IP Address tidak boleh kosong!");
            return;
        }

        int maxId = 0;
        for (PrinterModel model : printerList) {
            try {
                int curr = Integer.parseInt(model.getId());
                if (curr > maxId) maxId = curr;
            } catch (NumberFormatException e) { }
        }
        
        String newId = String.valueOf(maxId + 1);

        props.setProperty("printer." + newId + ".category", cat);
        props.setProperty("printer." + newId + ".name", name);
        props.setProperty("printer." + newId + ".ip", ip);

        savePropertiesToFile();

        printerList.add(new PrinterModel(newId, cat, name, ip));
        txtName.clear();
        txtIp.clear();
        
        showAlert(Alert.AlertType.INFORMATION, "Sukses", "Printer berhasil ditambahkan.");
    }

    @FXML
    public void handleDelete(ActionEvent event) {
        PrinterModel selected = tablePrinter.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data Printer di tabel terlebih dahulu untuk dihapus!");
            return;
        }

        String id = selected.getId();
        
        props.remove("printer." + id + ".category");
        props.remove("printer." + id + ".name");
        props.remove("printer." + id + ".ip");

        savePropertiesToFile();

        printerList.remove(selected);
        
        showAlert(Alert.AlertType.INFORMATION, "Sukses", "Printer berhasil dihapus.");
    }

    private void savePropertiesToFile() {
        File propFile = new File(PRINTER_PROPS_PATH);
        try (FileOutputStream fos = new FileOutputStream(propFile)) {
            props.store(fos, "Data Master Printer");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan konfigurasi ke file properties.");
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
