package my.id.roytamba.nextmes.module.settings.pc.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
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
import my.id.roytamba.nextmes.module.settings.pc.model.PcModel;

public class PcMasterController {

    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextField txtName;
    @FXML private TextField txtIp;
    @FXML private TextField txtSearch;

    @FXML private TableView<PcModel> tablePc;
    @FXML private TableColumn<PcModel, String> colId;
    @FXML private TableColumn<PcModel, String> colCategory;
    @FXML private TableColumn<PcModel, String> colName;
    @FXML private TableColumn<PcModel, String> colIp;

    private final String PC_PROPS_PATH = "src/main/resources/pc.properties";
    private final ObservableList<PcModel> pcList = FXCollections.observableArrayList();
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
        FilteredList<PcModel> filteredData = new FilteredList<>(pcList, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(pc -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String[] keywords = newValue.toLowerCase().trim().split("\\s+");
                String nameLower = pc.getName().toLowerCase();
                String ipLower = pc.getIp().toLowerCase();
                String catLower = pc.getCategory().toLowerCase();

                for (String kw : keywords) {
                    if (!nameLower.contains(kw) && !ipLower.contains(kw) && !catLower.contains(kw)) {
                        return false;
                    }
                }
                return true;
            });
        });

        SortedList<PcModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePc.comparatorProperty());
        tablePc.setItems(sortedData);

        // Load Data Awal
        loadData();
    }

    private void loadData() {
        pcList.clear();
        File propFile = new File(PC_PROPS_PATH);
        if (!propFile.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
            
            List<String> keys = props.stringPropertyNames().stream()
                    .filter(k -> k.startsWith("pc.") && k.endsWith(".category"))
                    .collect(Collectors.toList());

            for (String key : keys) {
                String idStr = key.substring(3, key.length() - 9);
                String cat = props.getProperty("pc." + idStr + ".category", "");
                String name = props.getProperty("pc." + idStr + ".name", "");
                String ip = props.getProperty("pc." + idStr + ".ip", "");
                
                pcList.add(new PcModel(idStr, cat, name, ip));
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
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Nama PC dan IP Address tidak boleh kosong!");
            return;
        }

        int maxId = 0;
        for (PcModel model : pcList) {
            try {
                int curr = Integer.parseInt(model.getId());
                if (curr > maxId) maxId = curr;
            } catch (NumberFormatException e) { }
        }
        
        String newId = String.valueOf(maxId + 1);

        props.setProperty("pc." + newId + ".category", cat);
        props.setProperty("pc." + newId + ".name", name);
        props.setProperty("pc." + newId + ".ip", ip);

        savePropertiesToFile();

        pcList.add(new PcModel(newId, cat, name, ip));
        txtName.clear();
        txtIp.clear();
        
        showAlert(Alert.AlertType.INFORMATION, "Sukses", "MES PC berhasil ditambahkan.");
    }

    @FXML
    public void handleDelete(ActionEvent event) {
        PcModel selected = tablePc.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data PC di tabel terlebih dahulu untuk dihapus!");
            return;
        }

        String id = selected.getId();
        
        props.remove("pc." + id + ".category");
        props.remove("pc." + id + ".name");
        props.remove("pc." + id + ".ip");

        savePropertiesToFile();

        pcList.remove(selected);
        
        showAlert(Alert.AlertType.INFORMATION, "Sukses", "MES PC berhasil dihapus.");
    }

    private void savePropertiesToFile() {
        File propFile = new File(PC_PROPS_PATH);
        try (FileOutputStream fos = new FileOutputStream(propFile)) {
            props.store(fos, "Data Master PC");
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
