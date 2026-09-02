package my.id.roytamba.nextmes.module.alarm.service;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

public class DevicePingService {

    private ScheduledExecutorService scheduler;
    private final List<Device> monitoredDevices = new ArrayList<>();
    
    // Callback to update UI if needed
    private Runnable onStatusChangeCallback;

    public static class Device {
        public String id;
        public String type;
        public String name;
        public String ip;
        public boolean isOnline = true;
        public boolean isMonitored = true;

        public Device(String id, String type, String name, String ip) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.ip = ip;
        }
    }

    public void setOnStatusChangeCallback(Runnable callback) {
        this.onStatusChangeCallback = callback;
    }

    public List<Device> getDevices() {
        return monitoredDevices;
    }

    public void startService() {
        loadDevices();
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            });
            // Ping every 10 seconds
            scheduler.scheduleAtFixedRate(this::pingDevices, 2, 10, TimeUnit.SECONDS);
        }
    }

    public void stopService() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        NotificationService.stopAlarm();
    }

    public void reloadDevices() {
        loadDevices();
        if (onStatusChangeCallback != null) {
            Platform.runLater(onStatusChangeCallback);
        }
    }

    private void loadDevices() {
        monitoredDevices.clear();
        loadFromProperties("plc", "src/main/resources/plc.properties", "PLC");
        loadFromProperties("pc", "src/main/resources/pc.properties", "PC");
        loadFromProperties("printer", "src/main/resources/printer.properties", "Printer");
        loadFromProperties("dctool", "src/main/resources/dctool.properties", "DC Tool");
    }

    private void loadFromProperties(String prefix, String path, String typeName) {
        File propFile = new File(path);
        if (!propFile.exists()) return;
        
        try (FileInputStream fis = new FileInputStream(propFile)) {
            Properties props = new Properties();
            props.load(fis);
            
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith(prefix + ".") && key.endsWith(".name")) {
                    String id = key.substring(prefix.length() + 1, key.length() - 5);
                    String name = props.getProperty(key, "");
                    String ip = props.getProperty(prefix + "." + id + ".ip", "");
                    
                    if (!ip.isEmpty()) {
                        monitoredDevices.add(new Device(id, typeName, name, ip));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load " + typeName + " properties: " + e.getMessage());
        }
    }

    private void pingDevices() {
        boolean anyFailed = false;
        boolean statusChanged = false;

        for (Device dev : monitoredDevices) {
            if (!dev.isMonitored) continue;

            boolean currentOnline = false;
            try {
                InetAddress inet = InetAddress.getByName(dev.ip);
                currentOnline = inet.isReachable(2000); // 2 second timeout
            } catch (Exception e) {
                currentOnline = false;
            }

            if (dev.isOnline != currentOnline) {
                dev.isOnline = currentOnline;
                statusChanged = true;
            }

            if (!currentOnline) {
                anyFailed = true;
                final String failedIp = dev.ip;
                final String failedName = dev.name;
                final String failedType = dev.type;
                Platform.runLater(() -> {
                    NotificationService.showPopup("Device Offline", 
                        failedType + " [" + failedName + "] with IP " + failedIp + " is unreachable!");
                });
            }
        }

        if (anyFailed) {
            NotificationService.playAlarm();
        } else {
            // If all monitored devices are fine, stop the alarm
            NotificationService.stopAlarm();
        }

        if (statusChanged && onStatusChangeCallback != null) {
            Platform.runLater(onStatusChangeCallback);
        }
    }
}
