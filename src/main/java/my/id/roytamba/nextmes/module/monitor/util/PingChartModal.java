package my.id.roytamba.nextmes.module.monitor.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
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

public class PingChartModal {

    public static void show(String deviceName, String ip) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Ping Monitor - " + deviceName + " (" + ip + ")");

        // Header
        Label lblTitle = new Label("Heartbeat: " + deviceName);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        
        Label lblIp = new Label(ip);
        lblIp.setFont(Font.font("Segoe UI", 14));
        lblIp.setTextFill(Color.GRAY);
        
        VBox header = new VBox(5, lblTitle, lblIp);
        header.setPadding(new Insets(10, 20, 10, 20));

        // Stats Cards UI
        Label lblMin = createStatCard("Min", "0 ms");
        Label lblMax = createStatCard("Max", "0 ms");
        Label lblAvg = createStatCard("Avg", "0 ms");
        Label lblRto = createStatCard("RTO", "0");
        lblRto.setTextFill(Color.RED);
        
        HBox statsBox = new HBox(15);
        statsBox.setPadding(new Insets(10, 20, 10, 20));
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
        lineChart.getData().add(series);
        VBox.setVgrow(lineChart, Priority.ALWAYS);

        // Log Area
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setFont(Font.font("Consolas", 12));

        VBox layout = new VBox(header, statsBox, lineChart, logArea);
        layout.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(layout, 800, 600);
        stage.setScene(scene);
        
        // Modal State
        ModalState state = new ModalState();
        
        // Live Ping Thread untuk Modal ini
        Thread livePingThread = new Thread(() -> {
            Pattern timePattern = Pattern.compile("time[=|<](\\d+)ms");
            while (state.isRunning) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("ping", "-n", "1", "-w", "1000", ip);
                    Process process = pb.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    boolean replied = false;
                    int rtt = 0;

                    String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    StringBuilder tempLog = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        
                        tempLog.append("[").append(timeStr).append("] ").append(line).append("\n");

                        if (line.contains("Reply from") || line.contains("bytes=")) {
                            replied = true;
                            Matcher m = timePattern.matcher(line);
                            if (m.find()) {
                                rtt = Integer.parseInt(m.group(1));
                            }
                        } else if (line.contains("Request timed out") || line.contains("Destination host unreachable")) {
                            replied = false;
                        }
                    }
                    process.waitFor();

                    final boolean finalReplied = replied;
                    final int finalRtt = rtt;
                    final String finalLog = tempLog.toString();

                    Platform.runLater(() -> {
                        // Update Log
                        logArea.appendText(finalLog);
                        
                        // Batasi panjang log agar tidak membebani memori
                        if (logArea.getText().length() > 5000) {
                            logArea.setText(logArea.getText().substring(logArea.getText().length() - 2500));
                        }
                        
                        logArea.selectPositionCaret(logArea.getLength());
                        logArea.deselect();

                        // Update Chart & Stats
                        if (finalReplied) {
                            state.pingCount++;
                            state.sumRtt += finalRtt;
                            
                            if (state.pingCount == 1) {
                                state.minRtt = finalRtt;
                                state.maxRtt = finalRtt;
                            } else {
                                if (finalRtt < state.minRtt) state.minRtt = finalRtt;
                                if (finalRtt > state.maxRtt) state.maxRtt = finalRtt;
                            }
                            
                            int avgRtt = state.sumRtt / state.pingCount;
                            
                            lblMin.setText("Min: " + state.minRtt + " ms");
                            lblMax.setText("Max: " + state.maxRtt + " ms");
                            lblAvg.setText("Avg: " + avgRtt + " ms");

                            series.getData().add(new XYChart.Data<>(timeStr, finalRtt));
                        } else {
                            state.rtoCount++;
                            lblRto.setText("RTO: " + state.rtoCount);
                            series.getData().add(new XYChart.Data<>(timeStr, 0));
                        }

                        if (series.getData().size() > 30) {
                            series.getData().remove(0);
                        }
                    });

                    Thread.sleep(1000); // Tunggu 1 detik sebelum ping selanjutnya
                    
                } catch (Exception e) {
                    Platform.runLater(() -> logArea.appendText("Error: " + e.getMessage() + "\n"));
                    try { Thread.sleep(2000); } catch (Exception ex) {}
                }
            }
        });
        livePingThread.setDaemon(true);
        
        stage.setOnCloseRequest(e -> {
            state.isRunning = false;
        });

        stage.show();
        livePingThread.start();
    }

    private static Label createStatCard(String title, String val) {
        Label lbl = new Label(title + ": " + val);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lbl.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 10; -fx-background-radius: 5;");
        return lbl;
    }
    
    private static class ModalState {
        volatile boolean isRunning = true;
        int minRtt = 0;
        int maxRtt = 0;
        int sumRtt = 0;
        int pingCount = 0;
        int rtoCount = 0;
    }
}
