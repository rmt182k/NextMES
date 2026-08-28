/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package my.id.roytamba.nextmes.module.settings.controller;

import my.id.roytamba.nextmes.util.ViewManager;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import my.id.roytamba.nextmes.module.settings.appearance.controller.AppearanceController;

/**
 *
 * @author roymt
 */
public class SettingsController implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Karena Settings.fxml sekarang murni sebagai pembungkus (container) TabPane
        // dan telah memanggil <fx:include source="appearance/Appearance.fxml" />,
        // seluruh logika terkait UI Tampilan sudah ditangani sepenuhnya secara mandiri 
        // oleh AppearanceController.java.
    }
}
