/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package my.id.roytamba.nextmes.module.settings.appearance.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

/**
 *
 * @author roymt
 */
public class AppearanceController {

    private final String CONFIG_FILE_PATH = "src/main/resources/config.properties";

    @javafx.fxml.FXML
    private TextField txtAppWidth;
    @javafx.fxml.FXML
    private TextField txtAppHeight;
    @javafx.fxml.FXML
    private ComboBox<String> cmbAppLang;

    @javafx.fxml.FXML
    public void initialize() {
        setupLanguageListener(cmbAppLang);
        loadAppearanceSettings();
    }

    public void setupLanguageListener(ComboBox<String> cmbAppLang) {
        cmbAppLang.getItems().addAll("Bahasa Indonesia", "English");
        cmbAppLang.setValue("Bahasa Indonesia"); // Default
    }

    private void loadAppearanceSettings() {
        File configFile = new File(CONFIG_FILE_PATH);
        if (!configFile.exists()) {
            return;
        }

        try (FileInputStream in = new FileInputStream(configFile)) {
            Properties props = new Properties();
            props.load(in);

            txtAppWidth.setText(props.getProperty("ui.width", "1000"));
            txtAppHeight.setText(props.getProperty("ui.height", "650"));

            String lang = props.getProperty("ui.language");
            if (lang != null) {
                cmbAppLang.setValue(lang);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @javafx.fxml.FXML
    public void handleSaveAppearance(javafx.event.ActionEvent event) {
        saveAppearanceSettings();
    }

    private void saveAppearanceSettings() {
        File configFile = new File(CONFIG_FILE_PATH);
        Properties props = new Properties();

        // 1. Muat data lama agar konfigurasi database tidak hilang terhapus
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 2. Timpa nilai tampilan dengan yang baru
        props.setProperty("ui.width", txtAppWidth.getText().trim());
        props.setProperty("ui.height", txtAppHeight.getText().trim());
        props.setProperty("ui.language", cmbAppLang.getValue());

        // 3. Simpan kembali seluruh konfigurasi
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Konfigurasi Aplikasi");
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Pengaturan Tampilan berhasil disimpan. Silakan restart aplikasi untuk melihat perubahan.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
