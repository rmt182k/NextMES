package my.id.roytamba.nextmes.module.monitor.pc.controller;

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

public class PcMonitorController {

    @FXML private Spinner<Integer> spinnerInterval;
    @FXML private TextField txtSearch;
    @FXML private VBox mainContainer;

    private final String PC_PROPS_PATH = "src/main/resources/pc.properties";
    private Timeline monitoringTimer;
    private int pingIntervalMs = 5000;
    private ExecutorService executorService;

    private final List<VBox> categoryWrappers = new ArrayList<>();
    private final List<VBox> allPcCards = new ArrayList<>();

    @FXML
    public void initialize() {
        executorService = Executors.newFixedThreadPool(15);

        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 300, 5);
        spinnerInterval.setValueFactory(valueFactory);
        spinnerInterval.valueProperty().addListener((obs, oldVal, newVal) -> {
            pingIntervalMs = newVal * 1000;
            restartMonitoring();
        });

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterPC(newVal));

        loadPCData();
        startMonitoring();
    }

    private void loadPCData() {
        File propFile = new File(PC_PROPS_PATH);
        if (!propFile.exists()) return;

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
        } catch (Exception e) { return; }

        Map<String, List<PcData>> categoryMap = new LinkedHashMap<>();

        List<String> keys = props.stringPropertyNames().stream()
                .filter(k -> k.startsWith("pc.") && k.endsWith(".category"))
                .collect(Collectors.toList());

        for (String key : keys) {
            String idStr = key.substring(3, key.length() - 9);
            String cat = props.getProperty("pc." + idStr + ".category");
            String name = props.getProperty("pc." + idStr + ".name");
            String ip = props.getProperty("pc." + idStr + ".ip");

            if (cat != null && name != null && ip != null) {
                categoryMap.computeIfAbsent(cat, k -> new ArrayList<>()).add(new PcData(name, ip));
            }
        }

        for (Map.Entry<String, List<PcData>> entry : categoryMap.entrySet()) {
            createCategoryUI(entry.getKey(), entry.getValue());
        }
    }

    private void createCategoryUI(String categoryName, List<PcData> pcList) {
        VBox wrapper = new VBox();
        wrapper.setSpacing(10);
        wrapper.setPadding(new Insets(0, 0, 20, 0));

        Label lblTitle = new Label(categoryName);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web("#646464"));

        FlowPane flowGrid = new FlowPane();
        flowGrid.setHgap(15);
        flowGrid.setVgap(15);

        for (PcData pc : pcList) {
            VBox card = createPcCard(pc.name, pc.ip);
            flowGrid.getChildren().add(card);
            allPcCards.add(card);
        }

        wrapper.getChildren().addAll(lblTitle, flowGrid);
        categoryWrappers.add(wrapper);
        mainContainer.getChildren().add(wrapper);
    }

    private VBox createPcCard(String name, String ip) {
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(5);
        card.setPadding(new Insets(15, 10, 15, 10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #dcdcdc; -fx-border-radius: 5; -fx-background-radius: 5;");
        card.setPrefSize(240, 130);
        card.setMaxSize(240, 130);

        Label icon = new Label("💻"); // Icon PC
        icon.setFont(Font.font("Segoe UI Emoji", 36));

        Label lblName = new Label(name);
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        Label lblIp = new Label(ip);
        lblIp.setFont(Font.font("Segoe UI", 12));
        lblIp.setTextFill(Color.GRAY);

        Label lblStatus = new Label("⏳ Checking...");
        lblStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblStatus.setTextFill(Color.web("#f39c12")); 

        card.getChildren().addAll(icon, lblName, lblIp, lblStatus);

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

    private void filterPC(String query) {
        String[] keywords = query.toLowerCase().trim().split("\\s+");

        for (VBox wrapper : categoryWrappers) {
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
        refreshStatus();
    }

    private void restartMonitoring() {
        if (monitoringTimer != null) monitoringTimer.stop();
        startMonitoring();
    }

    private void refreshStatus() {
        for (VBox card : allPcCards) {
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

    private static class PcData {
        String name;
        String ip;
        PcData(String name, String ip) {
            this.name = name;
            this.ip = ip;
        }
    }
}
