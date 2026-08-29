package my.id.roytamba.nextmes.module.monitor.plc.controller;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class PlcMonitorController {

    @FXML
    private Spinner<Integer> spinnerInterval;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbCategory;
    @FXML
    private VBox mainContainer;

    private final String PLC_PROPS_PATH = "src/main/resources/plc.properties";
    private Timeline monitoringTimer;
    private int pingIntervalMs = 5000;
    private ExecutorService executorService;

    // Struktur penyimpanan dinamis
    private final List<VBox> categoryWrappers = new ArrayList<>();
    private final List<VBox> allPlcCards = new ArrayList<>();

    @FXML
    public void initialize() {
        executorService = Executors.newFixedThreadPool(15);

        // Setup Spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 300, 5);
        spinnerInterval.setValueFactory(valueFactory);
        spinnerInterval.valueProperty().addListener((obs, oldVal, newVal) -> {
            pingIntervalMs = newVal * 1000;
            restartMonitoring();
        });

        // Setup Category Filter
        cmbCategory.getItems().add("Semua");
        cmbCategory.getSelectionModel().selectFirst();
        cmbCategory.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        
        // Setup Search Listener
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        loadPLCData();
        startMonitoring();
    }

    private void loadPLCData() {
        File propFile = new File(PLC_PROPS_PATH);
        if (!propFile.exists()) {
            System.err.println("plc.properties not found!");
            return;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Parse properties (mencari plc.X.category, plc.X.name, plc.X.ip)
        Map<String, List<PlcData>> categoryMap = new LinkedHashMap<>();

        // Kita cari max ID (asumsi ID berurutan atau kita cari semua key)
        List<String> keys = props.stringPropertyNames().stream()
                .filter(k -> k.startsWith("plc.") && k.endsWith(".category"))
                .collect(Collectors.toList());

        for (String key : keys) {
            String idStr = key.substring(4, key.length() - 9); // potong "plc." dan ".category"
            String cat = props.getProperty("plc." + idStr + ".category");
            String name = props.getProperty("plc." + idStr + ".name");
            String ip = props.getProperty("plc." + idStr + ".ip");

            if (cat != null && name != null && ip != null) {
                categoryMap.computeIfAbsent(cat, k -> new ArrayList<>()).add(new PlcData(name, ip));
            }
        }

        // Render UI
        for (Map.Entry<String, List<PlcData>> entry : categoryMap.entrySet()) {
            cmbCategory.getItems().add(entry.getKey());
            createCategoryUI(entry.getKey(), entry.getValue());
        }
    }

    private void createCategoryUI(String categoryName, List<PlcData> plcList) {
        VBox wrapper = new VBox();
        wrapper.setSpacing(10);
        wrapper.setPadding(new Insets(0, 0, 20, 0));

        Label lblTitle = new Label(categoryName);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web("#646464"));

        FlowPane flowGrid = new FlowPane();
        flowGrid.setHgap(15);
        flowGrid.setVgap(15);

        for (PlcData plc : plcList) {
            VBox card = createPlcCard(plc.name, plc.ip);
            flowGrid.getChildren().add(card);
            allPlcCards.add(card);
        }

        wrapper.getChildren().addAll(lblTitle, flowGrid);
        categoryWrappers.add(wrapper);
        mainContainer.getChildren().add(wrapper);
    }

    private VBox createPlcCard(String name, String ip) {
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(5);
        card.setPadding(new Insets(15, 10, 15, 10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #dcdcdc; -fx-border-radius: 5; -fx-background-radius: 5;");
        card.setPrefSize(240, 130);
        card.setMaxSize(240, 130);

        Label icon = new Label("⚙");
        icon.setFont(Font.font("Segoe UI Emoji", 36));

        Label lblName = new Label(name);
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        Label lblIp = new Label(ip);
        lblIp.setFont(Font.font("Segoe UI", 12));
        lblIp.setTextFill(Color.GRAY);

        Label lblStatus = new Label("⏳ Checking...");
        lblStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblStatus.setTextFill(Color.web("#f39c12")); // Orange

        card.getChildren().addAll(icon, lblName, lblIp, lblStatus);

        // Simpan data di properties node agar mudah diakses saat searching dan ping
        card.getProperties().put("ip", ip);
        card.getProperties().put("name", name);
        card.getProperties().put("statusLabel", lblStatus);

        return card;
    }

    private boolean isSmartMatch(String text, String[] keywords) {
        String lower = text.toLowerCase();
        for (String kw : keywords) {
            if (!lower.contains(kw)) return false;
        }
        return true;
    }

    private void applyFilters() {
        String query = txtSearch.getText();
        String[] keywords = query == null ? new String[0] : query.toLowerCase().trim().split("\\s+");
        String selectedCategory = cmbCategory.getValue();
        boolean filterByCategory = selectedCategory != null && !"Semua".equals(selectedCategory);

        for (VBox wrapper : categoryWrappers) {
            Label lblTitle = (Label) wrapper.getChildren().get(0);
            String catName = lblTitle.getText();
            
            if (filterByCategory && !catName.equals(selectedCategory)) {
                wrapper.setVisible(false);
                wrapper.setManaged(false);
                continue;
            }

            FlowPane grid = (FlowPane) wrapper.getChildren().get(1);
            boolean hasVisibleCard = false;

            for (Node node : grid.getChildren()) {
                VBox card = (VBox) node;
                String name = (String) card.getProperties().get("name");
                String ip = (String) card.getProperties().get("ip");

                boolean match = isSmartMatch(name, keywords) || isSmartMatch(ip, keywords);
                card.setVisible(match);
                card.setManaged(match);

                if (match) hasVisibleCard = true;
            }
            
            wrapper.setVisible(hasVisibleCard);
            wrapper.setManaged(hasVisibleCard);
        }
    }

    private void startMonitoring() {
        monitoringTimer = new Timeline(new KeyFrame(Duration.millis(pingIntervalMs), e -> refreshStatus()));
        monitoringTimer.setCycleCount(Timeline.INDEFINITE);
        monitoringTimer.play();
        
        // Panggilan pertama
        refreshStatus();
    }

    private void restartMonitoring() {
        if (monitoringTimer != null) {
            monitoringTimer.stop();
        }
        startMonitoring();
    }

    private void refreshStatus() {
        for (VBox card : allPlcCards) {
            if (card.isVisible()) {
                String ip = (String) card.getProperties().get("ip");
                Label lblStatus = (Label) card.getProperties().get("statusLabel");

                executorService.submit(() -> {
                    boolean online = pingHost(ip, 2000);

                    Platform.runLater(() -> {
                        if (online) {
                            lblStatus.setText("🟢 ONLINE");
                            lblStatus.setTextFill(Color.web("#28a745"));
                        } else {
                            lblStatus.setText("🔴 OFFLINE");
                            lblStatus.setTextFill(Color.web("#dc3545"));
                        }
                    });
                });
            }
        }
    }

    private boolean pingHost(String ip, int timeout) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isReachable(timeout);
        } catch (Exception ex) {
            return false;
        }
    }

    // Class pembantu
    private static class PlcData {
        String name;
        String ip;
        PlcData(String name, String ip) {
            this.name = name;
            this.ip = ip;
        }
    }
}
