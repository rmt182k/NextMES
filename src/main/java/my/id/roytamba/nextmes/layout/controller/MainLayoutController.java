/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package my.id.roytamba.nextmes.layout.controller;

import my.id.roytamba.nextmes.util.AppRoute;
import my.id.roytamba.nextmes.util.Router;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

/**
 *
 * @author roymt
 */
public class MainLayoutController {

    @FXML
    private BorderPane mainPane;

    @FXML
    public void initialize() {
        // 1. Daftarkan BorderPane ini ke Router
        Router.setMainLayout(mainPane);

        // 2. Set halaman default saat aplikasi pertama kali dibuka
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

        if ("Dashboard (Segera Hadir)".equals(defaultMenu)) {
            Router.navigate(AppRoute.DASHBOARD);
        } else {
            Router.navigate(AppRoute.SETTINGS);
        }
    }
}
