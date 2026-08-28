package my.id.roytamba.nextmes.module.settings.plc.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import my.id.roytamba.nextmes.module.settings.plc.model.PlcModel;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

public class PlcMasterController {

    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextField txtName;
    @FXML private TextField txtIp;
    @FXML private TextField txtSearch;

    @FXML private TableView<PlcModel> tablePlc;
    @FXML private TableColumn<PlcModel, String> colId;
    @FXML private TableColumn<PlcModel, String> colCategory;
    @FXML private TableColumn<PlcModel, String> colName;
    @FXML private TableColumn<PlcModel, String> colIp;

    private final String PLC_PROPS_PATH = "src/main/resources/plc.properties";
    private final ObservableList<PlcModel> plcList = FXCollections.observableArrayList();
    private Properties props = new Properties();

    @FXML
    public void initialize() {
        // Setup Kategori Default
        cmbCategory.getItems().addAll("PAINT SHOP", "BODY SHOP", "GA SHOP", "Lainnya");
        cmbCategory.setValue("PAINT SHOP");

        // Setup Binding Tabel
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colCategory.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colIp.setCellValueFactory(cellData -> cellData.getValue().ipProperty());

        // Setup FilteredList untuk fitur Smart Search
        FilteredList<PlcModel> filteredData = new FilteredList<>(plcList, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(plc -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String[] keywords = newValue.toLowerCase().trim().split("\\s+");
                String nameLower = plc.getName().toLowerCase();
                String ipLower = plc.getIp().toLowerCase();
                String catLower = plc.getCategory().toLowerCase();

                // Algoritma Smart Search: Semua keyword harus terpenuhi
                for (String kw : keywords) {
                    if (!nameLower.contains(kw) && !ipLower.contains(kw) && !catLower.contains(kw)) {
                        return false;
                    }
                }
                return true;
            });
        });

        SortedList<PlcModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePlc.comparatorProperty());
        tablePlc.setItems(sortedData);

        // Load Data Awal
        loadData();
    }

    private void loadData() {
        plcList.clear();
        File propFile = new File(PLC_PROPS_PATH);
        if (!propFile.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
            
            // Ekstrak ID yang valid
            List<String> keys = props.stringPropertyNames().stream()
                    .filter(k -> k.startsWith("plc.") && k.endsWith(".category"))
                    .collect(Collectors.toList());

            for (String key : keys) {
                String idStr = key.substring(4, key.length() - 9);
                String cat = props.getProperty("plc." + idStr + ".category", "");
                String name = props.getProperty("plc." + idStr + ".name", "");
                String ip = props.getProperty("plc." + idStr + ".ip", "");
                
                plcList.add(new PlcModel(idStr, cat, name, ip));
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
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Nama Mesin dan IP Address tidak boleh kosong!");
            return;
        }

        // Cari ID terbesar untuk membuat ID baru
        int maxId = 0;
        for (PlcModel model : plcList) {
            try {
                int curr = Integer.parseInt(model.getId());
                if (curr > maxId) maxId = curr;
            } catch (NumberFormatException e) { }
        }
        
        String newId = String.valueOf(maxId + 1);

        // Update properties di memory
        props.setProperty("plc." + newId + ".category", cat);
        props.setProperty("plc." + newId + ".name", name);
        props.setProperty("plc." + newId + ".ip", ip);

        savePropertiesToFile();

        // Refresh Tabel dan Bersihkan Form
        plcList.add(new PlcModel(newId, cat, name, ip));
        txtName.clear();
        txtIp.clear();
        
        showAlert(Alert.AlertType.INFORMATION, "Sukses", "Mesin PLC berhasil ditambahkan.");
    }

    @FXML
    public void handleDelete(ActionEvent event) {
        PlcModel selected = tablePlc.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data PLC di tabel terlebih dahulu untuk dihapus!");
            return;
        }

        String id = selected.getId();
        
        // Hapus dari properties di memory
        props.remove("plc." + id + ".category");
        props.remove("plc." + id + ".name");
        props.remove("plc." + id + ".ip");

        savePropertiesToFile();

        // Hapus dari tabel UI
        plcList.remove(selected);
        
        showAlert(Alert.AlertType.INFORMATION, "Sukses", "Mesin PLC berhasil dihapus.");
    }

    private void savePropertiesToFile() {
        File propFile = new File(PLC_PROPS_PATH);
        try (FileOutputStream fos = new FileOutputStream(propFile)) {
            props.store(fos, "Data Master PLC");
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
