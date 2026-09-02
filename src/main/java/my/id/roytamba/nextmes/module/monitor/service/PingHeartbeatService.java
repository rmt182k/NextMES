package my.id.roytamba.nextmes.module.monitor.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

public class PingHeartbeatService {

    private static PingHeartbeatService instance;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private final Map<String, PingSession> sessions = new ConcurrentHashMap<>();

    public static class PingSession {
        public final ObservableList<XYChart.Data<String, Number>> history = FXCollections.observableArrayList();
        public final StringProperty log = new SimpleStringProperty("");
        public final IntegerProperty minRtt = new SimpleIntegerProperty(0);
        public final IntegerProperty maxRtt = new SimpleIntegerProperty(0);
        public final IntegerProperty avgRtt = new SimpleIntegerProperty(0);
        public final IntegerProperty rtoCount = new SimpleIntegerProperty(0);

        private int sumRtt = 0;
        private int pingCount = 0;
        
        public void appendLog(String text) {
            Platform.runLater(() -> {
                String current = log.get();
                // Keep log size manageable
                if (current.length() > 5000) {
                    current = current.substring(current.length() - 2500);
                }
                log.set(current + text + "\n");
            });
        }
    }

    private PingHeartbeatService() {}

    public static synchronized PingHeartbeatService getInstance() {
        if (instance == null) {
            instance = new PingHeartbeatService();
        }
        return instance;
    }

    public PingSession getSession(String ip) {
        return sessions.get(ip);
    }

    public void registerIp(String ip) {
        if (!sessions.containsKey(ip)) {
            PingSession session = new PingSession();
            sessions.put(ip, session);
            scheduler.scheduleAtFixedRate(() -> executePing(ip, session), 0, 2, TimeUnit.SECONDS);
        }
    }

    private void executePing(String ip, PingSession session) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ping", "-n", "1", "-w", "1000", ip);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean replied = false;
            int rtt = 0;

            Pattern timePattern = Pattern.compile("time[=|<](\\d+)ms");

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                final String logLine = "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + line;
                session.appendLog(logLine);

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
            final String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            Platform.runLater(() -> {
                if (finalReplied) {
                    session.pingCount++;
                    session.sumRtt += finalRtt;
                    
                    if (session.pingCount == 1) {
                        session.minRtt.set(finalRtt);
                        session.maxRtt.set(finalRtt);
                    } else {
                        if (finalRtt < session.minRtt.get()) session.minRtt.set(finalRtt);
                        if (finalRtt > session.maxRtt.get()) session.maxRtt.set(finalRtt);
                    }
                    session.avgRtt.set(session.sumRtt / session.pingCount);

                    session.history.add(new XYChart.Data<>(timeStr, finalRtt));
                } else {
                    session.rtoCount.set(session.rtoCount.get() + 1);
                    session.history.add(new XYChart.Data<>(timeStr, 0));
                }

                // Keep only last 30 points in chart
                if (session.history.size() > 30) {
                    session.history.remove(0);
                }
            });

        } catch (Exception e) {
            session.appendLog("Error executing ping: " + e.getMessage());
        }
    }
}
