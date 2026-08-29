package my.id.roytamba.nextmes.module.settings.dctool.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import my.id.roytamba.nextmes.module.settings.dctool.model.DcToolModel;

public class DcToolMasterController {

    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextField txtName;
    @FXML private TextField txtIp;
    
    @FXML private TextField txtSearch;
    @FXML private TableView<DcToolModel> tableDcTool;
    @FXML private TableColumn<DcToolModel, String> colId;
    @FXML private TableColumn<DcToolModel, String> colCategory;
    @FXML private TableColumn<DcToolModel, String> colName;
    @FXML private TableColumn<DcToolModel, String> colIp;

    private final ObservableList<DcToolModel> dctoolList = FXCollections.observableArrayList();
    private FilteredList<DcToolModel> filteredData;

    private final String PROPS_PATH = "src/main/resources/dctool.properties";
    private Properties props = new Properties();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colIp.setCellValueFactory(new PropertyValueFactory<>("ip"));

        filteredData = new FilteredList<>(dctoolList, p -> true);
        tableDcTool.setItems(filteredData);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(tool -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return tool.getName().toLowerCase().contains(lowerCaseFilter) || 
                       tool.getIp().toLowerCase().contains(lowerCaseFilter);
            });
        });
        
        cmbCategory.setEditable(true);

        loadProperties();
    }

    private void loadProperties() {
        File file = new File(PROPS_PATH);
        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
            
            List<String> categories = new ArrayList<>();
            List<String> keys = props.stringPropertyNames().stream()
                    .filter(k -> k.startsWith("dctool.") && k.endsWith(".category"))
                    .toList();

            for (String key : keys) {
                String idStr = key.substring(7, key.length() - 9);
                String cat = props.getProperty("dctool." + idStr + ".category");
                String name = props.getProperty("dctool." + idStr + ".name");
                String ip = props.getProperty("dctool." + idStr + ".ip");

                if (cat != null && name != null && ip != null) {
                    dctoolList.add(new DcToolModel(idStr, cat, name, ip));
                    if (!categories.contains(cat)) categories.add(cat);
                }
            }
            
            cmbCategory.getItems().addAll(categories);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSave(ActionEvent event) {
        String category = cmbCategory.getValue() != null ? cmbCategory.getValue().trim() : "";
        String name = txtName.getText() != null ? txtName.getText().trim() : "";
        String ip = txtIp.getText() != null ? txtIp.getText().trim() : "";

        if (category.isEmpty() || name.isEmpty() || ip.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Kategori, Nama, dan IP tidak boleh kosong!");
            return;
        }

        int maxId = 0;
        for (DcToolModel p : dctoolList) {
            try {
                int curr = Integer.parseInt(p.getId());
                if (curr > maxId) maxId = curr;
            } catch (NumberFormatException e) { }
        }
        
        String newId = String.valueOf(maxId + 1);
        String prefix = "dctool." + newId + ".";

        props.setProperty(prefix + "category", category);
        props.setProperty(prefix + "name", name);
        props.setProperty(prefix + "ip", ip);

        savePropertiesToFile();

        dctoolList.add(new DcToolModel(newId, category, name, ip));

        if (!cmbCategory.getItems().contains(category)) {
            cmbCategory.getItems().add(category);
        }

        txtName.clear();
        txtIp.clear();
        cmbCategory.getSelectionModel().clearSelection();

        showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data DC Tool berhasil disimpan.");
    }

    @FXML
    public void handleDelete(ActionEvent event) {
        DcToolModel selected = tableDcTool.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data DC Tool yang ingin dihapus terlebih dahulu!");
            return;
        }

        String prefixToRemove = "dctool." + selected.getId() + ".";
        props.keySet().removeIf(k -> k.toString().startsWith(prefixToRemove));

        savePropertiesToFile();
        dctoolList.remove(selected);
        tableDcTool.getSelectionModel().clearSelection();

        showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data DC Tool berhasil dihapus.");
    }

    private void savePropertiesToFile() {
        try (FileOutputStream fos = new FileOutputStream(PROPS_PATH)) {
            props.store(fos, "DC Tool Configuration");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan konfigurasi!");
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
