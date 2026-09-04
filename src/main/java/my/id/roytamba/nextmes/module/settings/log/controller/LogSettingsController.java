package my.id.roytamba.nextmes.module.settings.log.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import my.id.roytamba.nextmes.module.monitor.service.PingHeartbeatService;

public class LogSettingsController {

    @FXML private CheckBox chkEnableLog;
    @FXML private Label lblLogSize;

    @FXML private ComboBox<String> cmbDeviceType;
    @FXML private DatePicker dpDate;
    
    @FXML private TableView<LogFileItem> tableLogs;
    @FXML private TableColumn<LogFileItem, String> colFileName;
    @FXML private TableColumn<LogFileItem, String> colDevice;
    @FXML private TableColumn<LogFileItem, String> colIp;
    @FXML private TableColumn<LogFileItem, String> colDate;
    @FXML private TableColumn<LogFileItem, String> colSize;
    @FXML private TableColumn<LogFileItem, Void> colAction;

    private final String LOG_DIR = "logs/ping_monitor";
    private final String CONFIG_FILE = "src/main/resources/config.properties";

    private ObservableList<LogFileItem> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        chkEnableLog.setSelected(PingHeartbeatService.isLoggingEnabled);
        
        cmbDeviceType.getItems().addAll("Semua", "plc", "pc", "printer", "dctool");
        cmbDeviceType.getSelectionModel().select("Semua");
        
        tableLogs.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTable();
        
        cmbDeviceType.valueProperty().addListener((obs, oldV, newV) -> filterData());
        dpDate.valueProperty().addListener((obs, oldV, newV) -> filterData());

        loadLogData();
    }

    private void setupTable() {
        colFileName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFileName()));
        colDevice.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDeviceType()));
        colIp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getIp()));
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateStr()));
        colSize.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSizeStr()));

        colAction.setCellFactory(param -> new TableCell<LogFileItem, Void>() {
            private final Button btnDelete = new Button("Delete");
            private final Button btnOpen = new Button("Open");
            private final HBox pane = new HBox(5, btnOpen, btnDelete);

            {
                btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 2 5;");
                btnOpen.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 2 5;");

                btnDelete.setOnAction(e -> {
                    LogFileItem item = getTableView().getItems().get(getIndex());
                    if (item.getFile().exists() && item.getFile().delete()) {
                        loadLogData();
                    }
                });
                
                btnOpen.setOnAction(e -> {
                    LogFileItem item = getTableView().getItems().get(getIndex());
                    try {
                        if (item.getFile().exists()) {
                            Desktop.getDesktop().open(item.getFile());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    private void loadLogData() {
        masterData.clear();
        File dir = new File(LOG_DIR);
        long totalLength = 0;

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".log"));
            if (files != null) {
                for (File f : files) {
                    totalLength += f.length();
                    masterData.add(parseFile(f));
                }
            }
        }
        
        double sizeMb = totalLength / (1024.0 * 1024.0);
        lblLogSize.setText(String.format("Total Ukuran Log: %.2f MB", sizeMb));

        filterData();
    }

    private LogFileItem parseFile(File f) {
        String name = f.getName(); // format: device_ping_ip_date.log -> misal: plc_ping_192_168_1_10_2026-09-04.log
        String nameNoExt = name.replace(".log", "");
        
        String device = "unknown";
        String ipStr = "unknown";
        String dateStr = "unknown";
        
        try {
            String[] parts = nameNoExt.split("_ping_");
            if (parts.length == 2) {
                device = parts[0];
                String rest = parts[1]; // misal: 192_168_1_10_2026-09-04
                // Tanggal biasanya 10 karakter terakhir "2026-09-04"
                if (rest.length() > 10) {
                    dateStr = rest.substring(rest.length() - 10);
                    ipStr = rest.substring(0, rest.length() - 11).replace("_", ".");
                } else {
                    ipStr = rest.replace("_", ".");
                }
            }
        } catch (Exception e) {
            // Jika tidak sesuai format
        }
        
        double sizeKb = f.length() / 1024.0;
        String sizeStr = String.format("%.2f KB", sizeKb);

        return new LogFileItem(f, name, device, ipStr, dateStr, sizeStr);
    }

    private void filterData() {
        String filterDevice = cmbDeviceType.getValue();
        LocalDate filterDate = dpDate.getValue();
        String dateTarget = filterDate != null ? filterDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;

        List<LogFileItem> filteredList = new ArrayList<>();
        for (LogFileItem item : masterData) {
            boolean matchDevice = "Semua".equals(filterDevice) || filterDevice.equalsIgnoreCase(item.getDeviceType());
            boolean matchDate = (dateTarget == null) || dateTarget.equals(item.getDateStr());
            
            if (matchDevice && matchDate) {
                filteredList.add(item);
            }
        }
        
        tableLogs.setItems(FXCollections.observableArrayList(filteredList));
    }

    @FXML
    public void handleResetFilter() {
        cmbDeviceType.getSelectionModel().select("Semua");
        dpDate.setValue(null);
    }

    @FXML
    public void handleToggleLog() {
        boolean enabled = chkEnableLog.isSelected();
        PingHeartbeatService.isLoggingEnabled = enabled;
        
        // Save to config
        try {
            File propFile = new File(CONFIG_FILE);
            Properties props = new Properties();
            if (propFile.exists()) {
                try (FileInputStream fis = new FileInputStream(propFile)) {
                    props.load(fis);
                }
            }
            props.setProperty("log.enabled", String.valueOf(enabled));
            try (FileOutputStream fos = new FileOutputStream(propFile)) {
                props.store(fos, "Log Settings Updated");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleOpenFolder() {
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) dir.mkdirs();
            new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleClearLogs() {
        File dir = new File(LOG_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".log"));
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
        loadLogData();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText("Log Berhasil Dihapus");
        alert.setContentText("Semua file log monitoring telah dibersihkan.");
        alert.show();
    }

    // Model untuk Tabel
    public static class LogFileItem {
        private final File file;
        private final String fileName;
        private final String deviceType;
        private final String ip;
        private final String dateStr;
        private final String sizeStr;

        public LogFileItem(File file, String fileName, String deviceType, String ip, String dateStr, String sizeStr) {
            this.file = file;
            this.fileName = fileName;
            this.deviceType = deviceType;
            this.ip = ip;
            this.dateStr = dateStr;
            this.sizeStr = sizeStr;
        }

        public File getFile() { return file; }
        public String getFileName() { return fileName; }
        public String getDeviceType() { return deviceType; }
        public String getIp() { return ip; }
        public String getDateStr() { return dateStr; }
        public String getSizeStr() { return sizeStr; }
    }
}
