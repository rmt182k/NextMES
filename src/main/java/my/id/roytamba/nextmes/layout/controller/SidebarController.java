/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package my.id.roytamba.nextmes.layout.controller;

import my.id.roytamba.nextmes.util.AppRoute;
import my.id.roytamba.nextmes.util.Router;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 *
 * @author roymt
 */
public class SidebarController {

    @FXML
    private VBox sidebar;
    @FXML
    private Label lblBrand;
    @FXML
    private Region spacer; // Mengontrol ruang pendorong di header
    @FXML
    private Button btnToggle;
    // @FXML
    // private Button btnDashboard;
    @FXML
    private Button btnMonitor;
    @FXML
    private Button btnSettings;

    private final String ACTIVE_STYLE = "-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-cursor: hand;";
    private final String INACTIVE_STYLE = "-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-cursor: hand;";

    private boolean isExpanded = true;
    private final double EXPANDED_WIDTH = 220.0;

    // 60.0 adalah angka emas (20px padding kiri + 20px icon + sisa 20px kanan = sempurna di tengah)
    private final double COLLAPSED_WIDTH = 60.0;

    @FXML
    public void initialize() {
        String defaultMenu = "Pengaturan";
        java.io.File configFile = new java.io.File("src/main/resources/config.properties");
        if (configFile.exists()) {
            try (java.io.FileInputStream in = new java.io.FileInputStream(configFile)) {
                java.util.Properties props = new java.util.Properties();
                props.load(in);
                defaultMenu = props.getProperty("ui.default_menu", "Pengaturan");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if ("Monitor".equals(defaultMenu)) {
            setActiveButton(btnMonitor);
        } else {
            setActiveButton(btnSettings);
        }
    }

    // @FXML
    // public void goToDashboard(ActionEvent event) {
    //     Router.navigate(AppRoute.DASHBOARD);
    //     setActiveButton(btnDashboard);
    // }

    @FXML
    public void goToMonitor(ActionEvent event) {
        Router.navigate(AppRoute.MONITOR);
        setActiveButton(btnMonitor);
    }

    @FXML
    public void goToSettings(ActionEvent event) {
        Router.navigate(AppRoute.SETTINGS);
        setActiveButton(btnSettings);
    }

    private void setActiveButton(Button activeButton) {
        // btnDashboard.setStyle(INACTIVE_STYLE);
        btnMonitor.setStyle(INACTIVE_STYLE);
        btnSettings.setStyle(INACTIVE_STYLE);
        if(activeButton != null) {
            activeButton.setStyle(ACTIVE_STYLE);
        }
    }

    @FXML
    public void toggleSidebar(ActionEvent event) {
        if (isExpanded) {
            // --- PROSES COLLAPSE (TUTUP) ---
            lblBrand.setVisible(false);
            lblBrand.setManaged(false);
            spacer.setManaged(false); // Matikan ruang kosong agar tombol toggle menempel ke padding kiri

            // Perintahkan JavaFX untuk hanya menampilkan icon, sembunyikan teksnya
            // btnDashboard.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            btnMonitor.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            btnSettings.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            animateSidebar(COLLAPSED_WIDTH);
            isExpanded = false;

        } else {
            // --- PROSES EXPAND (BUKA) ---
            lblBrand.setVisible(true);
            lblBrand.setManaged(true);
            spacer.setManaged(true);

            // Perintahkan JavaFX untuk kembali menampilkan Icon di sebelah Kiri teks
            // btnDashboard.setContentDisplay(ContentDisplay.LEFT);
            btnMonitor.setContentDisplay(ContentDisplay.LEFT);
            btnSettings.setContentDisplay(ContentDisplay.LEFT);

            animateSidebar(EXPANDED_WIDTH);
            isExpanded = true;
        }
    }

    private void animateSidebar(double targetWidth) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(sidebar.prefWidthProperty(), targetWidth)
                )
        );
        timeline.play();
    }
}
