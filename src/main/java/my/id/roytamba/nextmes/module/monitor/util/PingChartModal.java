package my.id.roytamba.nextmes.module.monitor.util;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import my.id.roytamba.nextmes.module.monitor.service.PingHeartbeatService;
import my.id.roytamba.nextmes.module.monitor.service.PingHeartbeatService.PingSession;

public class PingChartModal {

    public static void show(String deviceName, String ip) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Ping Monitor - " + deviceName + " (" + ip + ")");

        PingSession session = PingHeartbeatService.getInstance().getSession(ip);
        if (session == null) {
            // Should not happen if registered correctly
            PingHeartbeatService.getInstance().registerIp(ip);
            session = PingHeartbeatService.getInstance().getSession(ip);
        }

        // Header
        Label lblTitle = new Label("Heartbeat: " + deviceName);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        
        Label lblIp = new Label(ip);
        lblIp.setFont(Font.font("Segoe UI", 14));
        lblIp.setTextFill(Color.GRAY);
        
        VBox header = new VBox(5, lblTitle, lblIp);
        header.setPadding(new Insets(10, 20, 10, 20));

        // Stats Cards
        HBox statsBox = new HBox(15);
        statsBox.setPadding(new Insets(10, 20, 10, 20));

        Label lblMin = createStatCard("Min", session.minRtt.asString().concat(" ms"));
        Label lblMax = createStatCard("Max", session.maxRtt.asString().concat(" ms"));
        Label lblAvg = createStatCard("Avg", session.avgRtt.asString().concat(" ms"));
        Label lblRto = createStatCard("RTO", session.rtoCount.asString());
        lblRto.setTextFill(Color.RED);
        
        statsBox.getChildren().addAll(lblMin, lblMax, lblAvg, lblRto);

        // Chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Waktu");
        xAxis.setAnimated(false);
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Response Time (ms)");
        yAxis.setAnimated(false);
        
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Real-time Ping Response");
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(true);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("RTT (ms)");
        
        // Bind data
        series.setData(session.history);
        lineChart.getData().add(series);

        VBox.setVgrow(lineChart, Priority.ALWAYS);

        // Log Area
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setFont(Font.font("Consolas", 12));
        logArea.textProperty().bind(session.log);
        // Auto scroll to bottom trick
        session.log.addListener((obs, oldVal, newVal) -> {
            logArea.selectPositionCaret(logArea.getLength());
            logArea.deselect();
        });

        VBox layout = new VBox(header, statsBox, lineChart, logArea);
        layout.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(layout, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    private static Label createStatCard(String title, javafx.beans.binding.StringExpression bindVal) {
        Label lbl = new Label();
        lbl.textProperty().bind(javafx.beans.binding.Bindings.concat(title + ": ", bindVal));
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lbl.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 10; -fx-background-radius: 5;");
        return lbl;
    }
}
