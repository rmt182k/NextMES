package my.id.roytamba.nextmes.module.monitor.pc.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.control.Separator;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import my.id.roytamba.nextmes.module.monitor.util.PingChartModal;
import my.id.roytamba.nextmes.module.monitor.service.PingHeartbeatService;

public class PcMonitorController {

    @FXML
    private Spinner<Integer> spinnerInterval;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbCategory;
    @FXML
    private VBox mainContainer;

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

        cmbCategory.getItems().add("Semua");
        cmbCategory.getSelectionModel().selectFirst();
        cmbCategory.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        loadPCData();
        startMonitoring();
    }

    private void loadPCData() {
        File propFile = new File(PC_PROPS_PATH);
        if (!propFile.exists()) {
            return;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
        } catch (Exception e) {
            return;
        }

        Map<String, List<PcData>> categoryMap = new LinkedHashMap<>();

        // Ambil semua ID PC secara unik
        List<String> ids = props.stringPropertyNames().stream()
                .filter(k -> k.startsWith("pc.") && k.endsWith(".category"))
                .map(k -> k.substring(3, k.length() - 9))
                .distinct()
                .collect(Collectors.toList());

        for (String idStr : ids) {
            String prefix = "pc." + idStr + ".";
            String cat = props.getProperty(prefix + "category");
            String name = props.getProperty(prefix + "name");
            String ip = props.getProperty(prefix + "ip");

            if (cat != null && name != null && ip != null) {
                // Kumpulkan seluruh sisa kolom untuk ditampung ke dalam map
                Map<String, String> details = new LinkedHashMap<>();
                for (String propName : props.stringPropertyNames()) {
                    if (propName.startsWith(prefix)) {
                        String propKey = propName.substring(prefix.length());
                        details.put(propKey, props.getProperty(propName));
                    }
                }
                PingHeartbeatService.getInstance().registerIp("pc", ip);
                categoryMap.computeIfAbsent(cat, k -> new ArrayList<>()).add(new PcData(name, ip, details));
            }
        }

        for (Map.Entry<String, List<PcData>> entry : categoryMap.entrySet()) {
            cmbCategory.getItems().add(entry.getKey());
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
            VBox card = createPcCard(pc.name, pc.ip, pc.details);
            flowGrid.getChildren().add(card);
            allPcCards.add(card);
        }

        wrapper.getChildren().addAll(lblTitle, flowGrid);
        categoryWrappers.add(wrapper);
        mainContainer.getChildren().add(wrapper);
    }

    private VBox createPcCard(String name, String ip, Map<String, String> details) {
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(5);
        card.setPadding(new Insets(15, 10, 15, 10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #dcdcdc; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
        card.setPrefSize(240, 130);
        card.setMaxSize(240, 130);

        // Pass data detail langsung ke modal
        card.setOnMouseClicked(e -> showDetailModal(name, ip, details));

        Label icon = new Label("💻");
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

    private void showDetailModal(String pcName, String ip, Map<String, String> details) {
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.setTitle("Detail PC - " + pcName);

        // Header
        VBox headerBox = new VBox(5);
        headerBox.setPadding(new Insets(20));
        headerBox.setStyle("-fx-background-color: #1e293b;"); // Slate-800
        
        Label title = new Label(pcName);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);

        Label lblIp = new Label("IP: " + ip);
        lblIp.setFont(Font.font("Segoe UI", 14));
        lblIp.setTextFill(Color.web("#94a3b8")); // Slate-400
        
        headerBox.getChildren().addAll(title, lblIp);

        // Content
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));
        grid.setStyle("-fx-background-color: white;");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(150);
        col1.setPrefWidth(180);
        col1.setMaxWidth(250);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        if (details.isEmpty()) {
            Label noData = new Label("Tidak ada data detail tambahan.");
            noData.setFont(Font.font("Segoe UI", 14));
            noData.setTextFill(Color.GRAY);
            grid.add(noData, 0, 0);
        } else {
            int row = 0;
            for (Map.Entry<String, String> entry : details.entrySet()) {
                String key = entry.getKey();
                if (key.equals("name") || key.equals("ip") || key.equals("category")) {
                    continue;
                }

                Label lblKey = new Label(key);
                lblKey.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                lblKey.setTextFill(Color.web("#64748b")); // Slate-500

                Label lblVal = new Label(entry.getValue());
                lblVal.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
                lblVal.setTextFill(Color.web("#0f172a")); // Slate-900
                lblVal.setWrapText(true);

                grid.add(lblKey, 0, row);
                grid.add(lblVal, 1, row);
                
                row++;
                
                Separator sep = new Separator();
                grid.add(sep, 0, row, 2, 1);
                row++;
            }
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: white;");
        scroll.setPrefHeight(450);

        // Footer
        HBox footerBox = new HBox();
        footerBox.setPadding(new Insets(15, 25, 15, 25));
        footerBox.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");
        footerBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnPing = new Button("📡 Ping Monitor");
        btnPing.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btnPing.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
        btnPing.setPadding(new Insets(10, 20, 10, 20));
        btnPing.setOnAction(e -> {
            PingChartModal.show(pcName, ip);
        });
        
        btnPing.setOnMouseEntered(e -> btnPing.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;"));
        btnPing.setOnMouseExited(e -> btnPing.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;"));

        Button btnRemote = new Button("💻 Remote PC");
        btnRemote.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btnRemote.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
        btnRemote.setPadding(new Insets(10, 20, 10, 20));
        btnRemote.setOnAction(e -> {
            try {
                java.io.File propFile = new java.io.File("src/main/resources/remote.properties");
                String vncPath = "C:\\Program Files\\uvnc bvba\\UltraVNC\\vncviewer.exe";
                String vncUser = "admin.roymt";
                String vncPass = "VietNamChienThang#$202620";

                if (propFile.exists()) {
                    java.util.Properties props = new java.util.Properties();
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(propFile)) {
                        props.load(fis);
                        vncPath = props.getProperty("vnc.path", vncPath);
                        vncUser = props.getProperty("vnc.user", vncUser);
                        vncPass = props.getProperty("vnc.password", vncPass);
                    }
                }
                
                ProcessBuilder pb = new ProcessBuilder(vncPath, "-connect", ip + "::5900", "-user", vncUser, "-password", vncPass, "-autoscaling");
                pb.start();
                
                System.out.println("Membuka Remote PC untuk IP: " + ip);
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Remote Error");
                    alert.setHeaderText("Gagal membuka VNC Viewer");
                    alert.setContentText(ex.getMessage());
                    alert.show();
                });
            }
        });
        
        btnRemote.setOnMouseEntered(e -> btnRemote.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;"));
        btnRemote.setOnMouseExited(e -> btnRemote.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;"));

        // Tambahkan gap antar tombol
        footerBox.setSpacing(10);
        footerBox.getChildren().addAll(btnPing, btnRemote);

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(headerBox);
        mainLayout.setCenter(scroll);
        mainLayout.setBottom(footerBox);

        Scene scene = new Scene(mainLayout, 600, 600);
        modalStage.setScene(scene);
        modalStage.show();
    }

    private boolean isSmartMatch(String text, String[] keywords) {
        String lower = text.toLowerCase();
        for (String kw : keywords) {
            if (!lower.contains(kw)) {
                return false;
            }
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

                if (match) {
                    hasVisibleCard = true;
                }
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
        if (monitoringTimer != null) {
            monitoringTimer.stop();
        }
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
        Map<String, String> details;

        PcData(String name, String ip, Map<String, String> details) {
            this.name = name;
            this.ip = ip;
            this.details = details;
        }
    }
}
