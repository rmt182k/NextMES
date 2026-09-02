package my.id.roytamba.nextmes.module.alarm.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import my.id.roytamba.nextmes.module.alarm.model.ReminderModel;

public class TaskReminderService {

    private ScheduledExecutorService scheduler;
    private final List<ReminderModel> reminders = new ArrayList<>();
    private final String PROPS_PATH = "src/main/resources/reminders.properties";
    private Properties props = new Properties();

    private Runnable onUpdateCallback;

    public void setOnUpdateCallback(Runnable callback) {
        this.onUpdateCallback = callback;
    }

    public List<ReminderModel> getReminders() {
        return reminders;
    }

    public void startService() {
        loadReminders();
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            });
            // Check every 30 seconds
            scheduler.scheduleAtFixedRate(this::checkReminders, 5, 30, TimeUnit.SECONDS);
        }
    }

    public void stopService() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    public void reloadReminders() {
        loadReminders();
        if (onUpdateCallback != null) {
            Platform.runLater(onUpdateCallback);
        }
    }

    private void loadReminders() {
        reminders.clear();
        File propFile = new File(PROPS_PATH);
        if (!propFile.exists()) return;

        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
            
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("reminder.") && key.endsWith(".title")) {
                    String id = key.substring(9, key.length() - 6);
                    String title = props.getProperty(key, "");
                    String time = props.getProperty("reminder." + id + ".time", "");
                    boolean active = Boolean.parseBoolean(props.getProperty("reminder." + id + ".active", "false"));
                    
                    reminders.add(new ReminderModel(id, title, time, active));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load reminders: " + e.getMessage());
        }
    }

    public void saveReminder(ReminderModel model) {
        props.setProperty("reminder." + model.getId() + ".title", model.getTitle());
        props.setProperty("reminder." + model.getId() + ".time", model.getTime());
        props.setProperty("reminder." + model.getId() + ".active", String.valueOf(model.isActive()));
        savePropertiesToFile();
        reloadReminders();
    }

    public void deleteReminder(String id) {
        props.remove("reminder." + id + ".title");
        props.remove("reminder." + id + ".time");
        props.remove("reminder." + id + ".active");
        savePropertiesToFile();
        reloadReminders();
    }

    private void savePropertiesToFile() {
        File propFile = new File(PROPS_PATH);
        try {
            if(!propFile.exists()) {
                propFile.getParentFile().mkdirs();
                propFile.createNewFile();
            }
            try (FileOutputStream fos = new FileOutputStream(propFile)) {
                props.store(fos, "Task Reminders Data");
            }
        } catch (Exception e) {
            System.err.println("Failed to save reminders: " + e.getMessage());
        }
    }

    private void checkReminders() {
        LocalTime now = LocalTime.now();
        String currentTimeString = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        boolean changed = false;

        for (ReminderModel reminder : reminders) {
            if (reminder.isActive() && reminder.getTime().equals(currentTimeString)) {
                
                // Trigger Alarm
                NotificationService.playAlarm();
                
                Platform.runLater(() -> {
                    NotificationService.showPopup("Task Reminder", reminder.getTitle());
                });
                
                // Set to inactive so it doesn't trigger again immediately
                reminder.setActive(false);
                props.setProperty("reminder." + reminder.getId() + ".active", "false");
                changed = true;
            }
        }

        if (changed) {
            savePropertiesToFile();
            if (onUpdateCallback != null) {
                Platform.runLater(onUpdateCallback);
            }
        }
    }
}
