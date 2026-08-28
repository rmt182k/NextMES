/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package my.id.roytamba.nextmes.settings.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 *
 * @author roymt
 */
public class SettingsController {

    @FXML
    private TextField txtAppWidth;
    @FXML
    private TextField txtAppHeight;

    @FXML
    private void handleSaveAppearance() {
        try {
            double newWidth = Double.parseDouble(txtAppWidth.getText());
            double newHeight = Double.parseDouble(txtAppHeight.getText());

            // Mengambil Stage aktif dan menerapkan ukuran baru secara dinamis
            Stage stage = (Stage) txtAppWidth.getScene().getWindow();
            stage.setWidth(newWidth);
            stage.setHeight(newHeight);

            // Opsional: Simpan nilai ini ke file properties (misal: config.properties)
            // agar saat aplikasi direstart, ukurannya tetap menggunakan preferensi terbaru.
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Validasi");
            alert.setHeaderText(null);
            alert.setContentText("Harap masukkan angka yang valid untuk dimensi layar!");
            alert.showAndWait();
        }
    }
}
