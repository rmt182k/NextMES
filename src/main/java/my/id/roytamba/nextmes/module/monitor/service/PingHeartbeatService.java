package my.id.roytamba.nextmes.module.monitor.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingHeartbeatService {

    private static PingHeartbeatService instance;
    
    // Gunakan thread pool yang jauh lebih besar agar tidak bottleneck
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(50, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private final Map<String, Boolean> registeredIps = new ConcurrentHashMap<>();
    private final String LOG_DIR = "logs/ping_monitor";

    private PingHeartbeatService() {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static synchronized PingHeartbeatService getInstance() {
        if (instance == null) {
            instance = new PingHeartbeatService();
        }
        return instance;
    }

    public void registerIp(String ip) {
        if (!registeredIps.containsKey(ip)) {
            registeredIps.put(ip, true);
            // Ping dilakukan setiap 5 detik di background, 
            // khusus untuk menulis riwayat ke file log
            scheduler.scheduleAtFixedRate(() -> executePingAndLog(ip), 0, 5, TimeUnit.SECONDS);
        }
    }

    private void executePingAndLog(String ip) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ping", "-n", "1", "-w", "2000", ip);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            
            StringBuilder logOutput = new StringBuilder();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logOutput.append("[").append(timestamp).append("] ");

            boolean success = false;
            int rtt = -1;
            Pattern timePattern = Pattern.compile("time[=|<](\\d+)ms");

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (line.contains("Reply from") || line.contains("bytes=")) {
                    success = true;
                    Matcher m = timePattern.matcher(line);
                    if (m.find()) {
                        rtt = Integer.parseInt(m.group(1));
                    }
                    logOutput.append(line.trim());
                    break;
                } else if (line.contains("Request timed out") || line.contains("Destination host unreachable")) {
                    logOutput.append(line.trim());
                    break;
                }
            }
            process.waitFor();
            
            if (logOutput.length() < 25) {
                logOutput.append("Unknown error or timeout.");
            }

            // Tulis ke file log per IP
            writeToLogFile(ip, logOutput.toString());

        } catch (Exception e) {
            writeToLogFile(ip, "[" + LocalDateTime.now().toString() + "] Error: " + e.getMessage());
        }
    }

    private void writeToLogFile(String ip, String text) {
        try {
            File logFile = new File(LOG_DIR, "ping_" + ip.replace(".", "_") + ".log");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(text);
                writer.newLine();
            }
        } catch (Exception e) {
            System.err.println("Gagal menulis log untuk " + ip + ": " + e.getMessage());
        }
    }
}
