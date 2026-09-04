package my.id.roytamba.nextmes.module.alarm.controller;

import java.util.UUID;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import my.id.roytamba.nextmes.module.alarm.model.ReminderModel;
import my.id.roytamba.nextmes.module.alarm.service.DevicePingService;
import my.id.roytamba.nextmes.module.alarm.service.NotificationService;
import my.id.roytamba.nextmes.module.alarm.service.TaskReminderService;

public class AlarmController {

    // Device Monitoring Controls
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private TextField txtSearch;
    @FXML private Label lblServiceStatus;
    @FXML private Label lblTotalDevices;
    @FXML private Label lblTotalOffline;

    @FXML private TableView<DevicePingService.Device> tableDevice;
    @FXML private TableColumn<DevicePingService.Device, String> colType;
    @FXML private TableColumn<DevicePingService.Device, String> colName;
    @FXML private TableColumn<DevicePingService.Device, String> colIp;
    @FXML private TableColumn<DevicePingService.Device, Boolean> colStatus;
    @FXML private TableColumn<DevicePingService.Device, Boolean> colMonitor;

    // Task Reminder Controls
    @FXML private TextField txtReminderTitle;
    @FXML private TextField txtReminderTime;
    @FXML private TableView<ReminderModel> tableReminder;
    @FXML private TableColumn<ReminderModel, String> colReminderTime;
    @FXML private TableColumn<ReminderModel, String> colReminderTitle;
    @FXML private TableColumn<ReminderModel, Boolean> colReminderActive;
    @FXML private TableColumn<ReminderModel, Void> colReminderAction;

    private static final DevicePingService pingService = new DevicePingService();
    private static final TaskReminderService reminderService = new TaskReminderService();
    
    private static boolean isServiceRunning = false;
    
    private final ObservableList<DevicePingService.Device> deviceList = FXCollections.observableArrayList();
    private final ObservableList<ReminderModel> reminderList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupDeviceTab();
        setupReminderTab();
        
        if (isServiceRunning) {
            updateServiceStatusLabel();
            btnStop.setDisable(false);
            btnStart.setDisable(true);
        } else {
            updateServiceStatusLabel();
            btnStop.setDisable(true);
            btnStart.setDisable(false);
        }
    }

    private void setupDeviceTab() {
        colType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().type));
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().name));
        colIp.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().ip));

        colStatus.setCellValueFactory(cell -> new SimpleBooleanProperty(cell.getValue().isOnline));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean isOnline, boolean empty) {
                super.updateItem(isOnline, empty);
                if (empty || isOnline == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(isOnline ? "Online" : "Offline");
                    lbl.setStyle(isOnline ? "-fx-text-fill: #10b981; -fx-font-weight: bold;" : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colMonitor.setCellValueFactory(cell -> new SimpleBooleanProperty(cell.getValue().isMonitored));
        
        CheckBox chkAll = new CheckBox();
        chkAll.setOnAction(e -> {
            boolean selected = chkAll.isSelected();
            for (DevicePingService.Device dev : deviceList) {
                dev.isMonitored = selected;
            }
            tableDevice.refresh();
        });
        colMonitor.setGraphic(chkAll);
        
        colMonitor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean isMonitored, boolean empty) {
                super.updateItem(isMonitored, empty);
                if (empty || isMonitored == null) {
                    setGraphic(null);
                } else {
                    CheckBox chk = new CheckBox();
                    chk.setSelected(isMonitored);
                    chk.setOnAction(e -> {
                        DevicePingService.Device dev = getTableView().getItems().get(getIndex());
                        dev.isMonitored = chk.isSelected();
                    });
                    HBox box = new HBox(chk);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        FilteredList<DevicePingService.Device> filteredData = new FilteredList<>(deviceList, p -> true);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(dev -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return dev.name.toLowerCase().contains(lower) || 
                       dev.ip.toLowerCase().contains(lower) ||
                       dev.type.toLowerCase().contains(lower);
            });
        });

        tableDevice.setItems(filteredData);
        pingService.setOnStatusChangeCallback(this::refreshDeviceTable);
        
        if (!isServiceRunning) {
            pingService.reloadDevices();
        }
        refreshDeviceTable();
    }

    private void setupReminderTab() {
        colReminderTime.setCellValueFactory(cell -> cell.getValue().timeProperty());
        colReminderTime.setStyle("-fx-font-weight: bold; -fx-alignment: center;");
        
        colReminderTitle.setCellValueFactory(cell -> cell.getValue().titleProperty());
        
        colReminderActive.setCellValueFactory(cell -> cell.getValue().activeProperty());
        colReminderActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean isActive, boolean empty) {
                super.updateItem(isActive, empty);
                if (empty || isActive == null) {
                    setGraphic(null);
                } else {
                    CheckBox chk = new CheckBox(isActive ? "Aktif" : "Selesai/Non-aktif");
                    chk.setSelected(isActive);
                    chk.setOnAction(e -> {
                        ReminderModel r = getTableView().getItems().get(getIndex());
                        r.setActive(chk.isSelected());
                        reminderService.saveReminder(r);
                    });
                    setGraphic(chk);
                }
            }
        });

        colReminderAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Hapus");
            {
                btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");
                btnDelete.setOnAction(e -> {
                    ReminderModel r = getTableView().getItems().get(getIndex());
                    reminderService.deleteReminder(r.getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(btnDelete);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        tableReminder.setItems(reminderList);
        reminderService.setOnUpdateCallback(this::refreshReminderTable);
        
        if (!isServiceRunning) {
            reminderService.reloadReminders();
        }
        refreshReminderTable();
    }

    private void refreshDeviceTable() {
        deviceList.setAll(pingService.getDevices());
        tableDevice.refresh();
        updateStats();
    }

    private void refreshReminderTable() {
        reminderList.setAll(reminderService.getReminders());
        tableReminder.refresh();
    }

    private void updateStats() {
        lblTotalDevices.setText(String.valueOf(deviceList.size()));
        long offlineCount = deviceList.stream().filter(d -> !d.isOnline && d.isMonitored).count();
        lblTotalOffline.setText(String.valueOf(offlineCount));
    }

    private void updateServiceStatusLabel() {
        if (isServiceRunning) {
            lblServiceStatus.setText("Service Running");
            lblServiceStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        } else {
            lblServiceStatus.setText("Service Stopped");
            lblServiceStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }
    }

    @FXML
    public void handleStartMonitoring(ActionEvent event) {
        pingService.startService();
        reminderService.startService();
        isServiceRunning = true;
        btnStart.setDisable(true);
        btnStop.setDisable(false);
        updateServiceStatusLabel();
    }

    @FXML
    public void handleStopMonitoring(ActionEvent event) {
        pingService.stopService();
        reminderService.stopService();
        isServiceRunning = false;
        btnStart.setDisable(false);
        btnStop.setDisable(true);
        updateServiceStatusLabel();
    }

    @FXML
    public void handleReload(ActionEvent event) {
        pingService.reloadDevices();
        refreshDeviceTable();
    }



    @FXML
    public void handleAddReminder(ActionEvent event) {
        String title = txtReminderTitle.getText().trim();
        String time = txtReminderTime.getText().trim();

        if (title.isEmpty() || time.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validasi Error");
            alert.setHeaderText(null);
            alert.setContentText("Pesan Pengingat dan Waktu tidak boleh kosong!");
            alert.showAndWait();
            return;
        }

        if (!time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validasi Error");
            alert.setHeaderText(null);
            alert.setContentText("Format waktu salah! Gunakan format HH:mm (misal: 08:30 atau 14:00).");
            alert.showAndWait();
            return;
        }

        String id = UUID.randomUUID().toString().substring(0, 8);
        ReminderModel newReminder = new ReminderModel(id, title, time, true);
        reminderService.saveReminder(newReminder);

        txtReminderTitle.clear();
        txtReminderTime.clear();
    }
}
