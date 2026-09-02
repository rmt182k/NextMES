package my.id.roytamba.nextmes.module.alarm.service;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.media.AudioClip;
import java.awt.Toolkit;
import java.net.URL;

public class NotificationService {
    
    private static AudioClip alarmSound;
    private static boolean soundLoaded = false;
    private static boolean isPlaying = false;

    static {
        try {
            // Attempt to load the audio file
            URL resource = NotificationService.class.getResource("/audio/alarm.mp3");
            if (resource == null) {
                resource = NotificationService.class.getResource("/audio/alarm.wav");
            }
            if (resource != null) {
                alarmSound = new AudioClip(resource.toExternalForm());
                soundLoaded = true;
            }
        } catch (Exception e) {
            System.err.println("Failed to load alarm audio: " + e.getMessage());
        }
    }

    public static void playAlarm() {
        if (!isPlaying) {
            isPlaying = true;
            if (soundLoaded && alarmSound != null) {
                alarmSound.setCycleCount(AudioClip.INDEFINITE);
                alarmSound.play();
            } else {
                // Fallback to system beep if audio file not found
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    public static void stopAlarm() {
        if (isPlaying) {
            isPlaying = false;
            if (soundLoaded && alarmSound != null) {
                alarmSound.stop();
            }
        }
    }

    public static void showPopup(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText("Monitoring Alert");
            alert.setContentText(message);
            // Optionally stop alarm when user acknowledges the alert
            alert.setOnHidden(evt -> stopAlarm());
            
            // If the alert is not already showing, show it
            alert.show();
        });
    }
}
