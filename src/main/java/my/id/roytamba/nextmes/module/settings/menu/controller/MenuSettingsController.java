package my.id.roytamba.nextmes.module.settings.menu.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;

public class MenuSettingsController {

    private final String CONFIG_FILE_PATH = "src/main/resources/config.properties";

    @FXML
    private ComboBox<String> cmbDefaultMenu;

    @FXML
    public void initialize() {
        // Setup item
        cmbDefaultMenu.getItems().addAll("Pengaturan", "Dashboard (Segera Hadir)");
        cmbDefaultMenu.setValue("Pengaturan");
        
        loadMenuSettings();
    }

    private void loadMenuSettings() {
        File configFile = new File(CONFIG_FILE_PATH);
        if (!configFile.exists()) {
            return;
        }

        try (FileInputStream in = new FileInputStream(configFile)) {
            Properties props = new Properties();
            props.load(in);

            String defMenu = props.getProperty("ui.default_menu");
            if (defMenu != null) {
                cmbDefaultMenu.setValue(defMenu);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void handleSaveMenuSettings(ActionEvent event) {
        File configFile = new File(CONFIG_FILE_PATH);
        Properties props = new Properties();

        // 1. Muat data lama
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 2. Timpa
        props.setProperty("ui.default_menu", cmbDefaultMenu.getValue());

        // 3. Simpan
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Konfigurasi Aplikasi");
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Pengaturan Manajemen Menu berhasil disimpan.");
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
