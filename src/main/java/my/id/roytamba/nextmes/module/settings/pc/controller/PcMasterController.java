package my.id.roytamba.nextmes.module.settings.pc.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.FileChooser;
import my.id.roytamba.nextmes.module.settings.pc.model.PcModel;

public class PcMasterController {

    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextField txtName;
    @FXML private TextField txtIp;
    @FXML private Button btnDetails;
    @FXML private TextField txtSearch;

    @FXML private TableView<PcModel> tablePc;
    @FXML private TableColumn<PcModel, String> colId;
    @FXML private TableColumn<PcModel, String> colCategory;
    @FXML private TableColumn<PcModel, String> colName;
    @FXML private TableColumn<PcModel, String> colIp;

    private final String PC_PROPS_PATH = "src/main/resources/pc.properties";
    private final ObservableList<PcModel> pcList = FXCollections.observableArrayList();
    private Properties props = new Properties();
    private Map<String, String> currentDetails = new HashMap<>();

    @FXML
    public void initialize() {
        cmbCategory.getItems().addAll("PAINT SHOP", "BODY SHOP", "GA SHOP", "Lainnya");
        cmbCategory.setValue("PAINT SHOP");
        cmbCategory.setEditable(true);

        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colCategory.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colIp.setCellValueFactory(cellData -> cellData.getValue().ipProperty());

        FilteredList<PcModel> filteredData = new FilteredList<>(pcList, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(pc -> {
                if (newValue == null || newValue.isEmpty()) return true;
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

        // Listener untuk menampilkan detail saat baris tabel diklik
        tablePc.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            currentDetails.clear();
            if (newSelection != null) {
                cmbCategory.setValue(newSelection.getCategory());
                txtName.setText(newSelection.getName());
                txtIp.setText(newSelection.getIp());
                
                if (newSelection.getDetails() != null) {
                    currentDetails.putAll(newSelection.getDetails());
                }
            } else {
                txtName.clear();
                txtIp.clear();
            }
            btnDetails.setText("⚙️ Konfigurasi Ekstra (" + currentDetails.size() + " item)");
        });

        loadData();
    }

    private void loadData() {
        pcList.clear();
        File propFile = new File(PC_PROPS_PATH);
        if (!propFile.exists()) return;

        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
            List<String> ids = props.stringPropertyNames().stream()
                    .filter(k -> k.startsWith("pc.") && k.endsWith(".category"))
                    .map(k -> k.substring(3, k.length() - 9))
                    .distinct()
                    .collect(Collectors.toList());

            for (String idStr : ids) {
                String prefix = "pc." + idStr + ".";
                Map<String, String> details = new HashMap<>();

                for (String propName : props.stringPropertyNames()) {
                    if (propName.startsWith(prefix)) {
                        String detailKey = propName.substring(prefix.length());
                        // Jangan masukkan category, name, ip ke dalam map details agar tidak dobel
                        if (!detailKey.equals("category") && !detailKey.equals("name") && !detailKey.equals("ip")) {
                            details.put(detailKey, props.getProperty(propName));
                        }
                    }
                }

                String cat = props.getProperty(prefix + "category", "");
                String name = props.getProperty(prefix + "name", "");
                String ip = props.getProperty(prefix + "ip", "");

                pcList.add(new PcModel(idStr, cat, name, ip, details));
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

        PcModel selected = tablePc.getSelectionModel().getSelectedItem();
        String idToSave;
        boolean isNew = false;
        
        if (selected != null) {
            idToSave = selected.getId();
            
            // Hapus properties lama untuk ID ini agar detail lama yang dihapus user ikut terhapus di properties
            List<String> keysToRemove = new ArrayList<>();
            for (String propName : props.stringPropertyNames()) {
                if (propName.startsWith("pc." + idToSave + ".")) {
                    keysToRemove.add(propName);
                }
            }
            for (String k : keysToRemove) {
                props.remove(k);
            }
            pcList.remove(selected);
        } else {
            int maxId = 0;
            for (PcModel model : pcList) {
                try {
                    int curr = Integer.parseInt(model.getId());
                    if (curr > maxId) maxId = curr;
                } catch (NumberFormatException e) { }
            }
            idToSave = String.valueOf(maxId + 1);
            isNew = true;
        }

        String prefix = "pc." + idToSave + ".";
        props.setProperty(prefix + "category", cat);
        props.setProperty(prefix + "name", name);
        props.setProperty(prefix + "ip", ip);

        Map<String, String> newDetailsMap = new HashMap<>(currentDetails);
        for (Map.Entry<String, String> entry : newDetailsMap.entrySet()) {
            props.setProperty(prefix + entry.getKey(), entry.getValue());
        }

        savePropertiesToFile();

        pcList.add(new PcModel(idToSave, cat, name, ip, newDetailsMap));
        
        tablePc.getSelectionModel().clearSelection();
        txtName.clear();
        txtIp.clear();
        currentDetails.clear();
        btnDetails.setText("⚙️ Konfigurasi Ekstra (0 item)");

        showAlert(Alert.AlertType.INFORMATION, "Sukses", isNew ? "MES PC berhasil ditambahkan." : "MES PC berhasil diperbarui.");
    }

    @FXML
    public void handleDelete(ActionEvent event) {
        PcModel selected = tablePc.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih PC yang ingin dihapus terlebih dahulu!");
            return;
        }

        // Hapus dari properties (cari semua key yang diawali dengan id)
        String prefixToRemove = "pc." + selected.getId() + ".";
        props.keySet().removeIf(k -> k.toString().startsWith(prefixToRemove));

        savePropertiesToFile();
        pcList.remove(selected);
        tablePc.getSelectionModel().clearSelection();
        txtName.clear();
        txtIp.clear();
        currentDetails.clear();
        btnDetails.setText("⚙️ Konfigurasi Ekstra (0 item)");

        showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data MES PC berhasil dihapus.");
    }

    @FXML
    public void handleImportCSV(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih File CSV Master PC");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(tablePc.getScene().getWindow());
        
        if (file != null) {
            importCSV(file);
        }
    }

    private void importCSV(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) return;
            
            String[] headers = headerLine.split(",", -1);
            
            int maxId = 0;
            for (PcModel model : pcList) {
                try {
                    int curr = Integer.parseInt(model.getId());
                    if (curr > maxId) maxId = curr;
                } catch (NumberFormatException e) { }
            }
            
            int count = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] values = line.split(",", -1);
                
                String name = "";
                String ip = "";
                String category = "Lainnya";
                Map<String, String> detailsMap = new HashMap<>();
                
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    String h = headers[i].trim();
                    String v = values[i].trim();
                    
                    if (h.equalsIgnoreCase("OperatorTerminal") || h.equalsIgnoreCase("name")) {
                        name = v;
                    } else if (h.equalsIgnoreCase("IPAddress") || h.equalsIgnoreCase("ip")) {
                        ip = v;
                    } else if (h.equalsIgnoreCase("Workplace") || h.equalsIgnoreCase("category")) {
                        category = v.isEmpty() ? "Lainnya" : v;
                    } else {
                        if (!v.isEmpty()) {
                            detailsMap.put(h, v);
                        }
                    }
                }
                
                if (name.isEmpty() && ip.isEmpty()) continue;
                
                maxId++;
                String idStr = String.valueOf(maxId);
                String prefix = "pc." + idStr + ".";
                
                props.setProperty(prefix + "category", category);
                props.setProperty(prefix + "name", name);
                props.setProperty(prefix + "ip", ip);
                
                for (Map.Entry<String, String> entry : detailsMap.entrySet()) {
                    props.setProperty(prefix + entry.getKey(), entry.getValue());
                }
                
                pcList.add(new PcModel(idStr, category, name, ip, detailsMap));
                count++;
            }
            
            savePropertiesToFile();
            showAlert(Alert.AlertType.INFORMATION, "Import Berhasil", count + " data PC berhasil diimport ke master data.");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membaca file CSV: " + e.getMessage());
        }
    }

    private void savePropertiesToFile() {
        File propFile = new File(PC_PROPS_PATH);
        try (FileOutputStream fos = new FileOutputStream(propFile)) {
            props.store(fos, "Data Master PC Full Detail");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan konfigurasi.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static class DetailRow {
        public final SimpleStringProperty key;
        public final SimpleStringProperty value;
        public DetailRow(String k, String v) {
            this.key = new SimpleStringProperty(k);
            this.value = new SimpleStringProperty(v);
        }
    }

    @FXML
    public void openDetailsModal(ActionEvent event) {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Konfigurasi Ekstra");
        dialog.setHeaderText("Kelola atribut tambahan untuk PC ini");

        ButtonType btnSimpanType = new ButtonType("Simpan", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSimpanType, ButtonType.CANCEL);

        TableView<DetailRow> detailTable = new TableView<>();
        TableColumn<DetailRow, String> colK = new TableColumn<>("Key (Properti)");
        colK.setCellValueFactory(data -> data.getValue().key);
        colK.setPrefWidth(150);
        
        TableColumn<DetailRow, String> colV = new TableColumn<>("Value (Nilai)");
        colV.setCellValueFactory(data -> data.getValue().value);
        colV.setPrefWidth(250);

        detailTable.getColumns().addAll(colK, colV);

        ObservableList<DetailRow> rowData = FXCollections.observableArrayList();
        for (Map.Entry<String, String> entry : currentDetails.entrySet()) {
            rowData.add(new DetailRow(entry.getKey(), entry.getValue()));
        }
        detailTable.setItems(rowData);

        TextField txtK = new TextField();
        txtK.setPromptText("Key (Cth: WorkplaceID)");
        TextField txtV = new TextField();
        txtV.setPromptText("Value");
        Button btnAdd = new Button("Tambah");
        Button btnDel = new Button("Hapus Pilihan");
        
        btnAdd.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-cursor: hand;");
        btnDel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand;");

        btnAdd.setOnAction(e -> {
            if (!txtK.getText().isEmpty() && !txtV.getText().isEmpty()) {
                rowData.add(new DetailRow(txtK.getText(), txtV.getText()));
                txtK.clear();
                txtV.clear();
            }
        });
        
        btnDel.setOnAction(e -> {
            DetailRow sel = detailTable.getSelectionModel().getSelectedItem();
            if(sel != null) rowData.remove(sel);
        });

        HBox inputPane = new HBox(10, txtK, txtV, btnAdd, btnDel);
        VBox vbox = new VBox(10, detailTable, inputPane);
        vbox.setPadding(new Insets(10));
        
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSimpanType) {
                Map<String, String> res = new HashMap<>();
                for (DetailRow r : rowData) {
                    res.put(r.key.get(), r.value.get());
                }
                return res;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(newMap -> {
            currentDetails.clear();
            currentDetails.putAll(newMap);
            btnDetails.setText("⚙️ Konfigurasi Ekstra (" + currentDetails.size() + " item)");
        });
    }
}
