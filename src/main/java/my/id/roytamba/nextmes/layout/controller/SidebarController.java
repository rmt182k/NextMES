/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package my.id.roytamba.nextmes.layout.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author roymt
 */
public class SidebarController {

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnProduction;
    @FXML
    private Button btnSettings;

    @FXML
    private void goToDashboard(ActionEvent event) {
        // Asumsi kamu sudah punya file Dashboard.fxml
        loadView("/view/layout/Dashboard.fxml", event);
    }

    @FXML
    private void goToSettings(ActionEvent event) {
        // Memuat file Settings.fxml yang tadi kita buat
        loadView("/view/pages/Settings.fxml", event);
    }

    /**
     * Method reusable untuk memuat FXML dan memasukkannya ke bagian tengah
     * (Center) MainLayout
     */
    private void loadView(String fxmlPath, ActionEvent event) {
        try {
            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                System.err.println("File FXML tidak ditemukan: " + fxmlPath);
                return;
            }

            // Load file FXML tujuan
            Parent newView = FXMLLoader.load(fxmlLocation);

            // Mendapatkan komponen node yang memicu event (Tombol Sidebar)
            Node sourceNode = (Node) event.getSource();

            // Mengambil root layout (MainLayout.fxml) yang merupakan BorderPane
            BorderPane mainLayout = (BorderPane) sourceNode.getScene().getRoot();

            // Mengganti konten yang ada di posisi <center> dengan view baru
            mainLayout.setCenter(newView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
