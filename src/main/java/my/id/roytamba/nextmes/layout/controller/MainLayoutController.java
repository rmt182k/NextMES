/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package my.id.roytamba.nextmes.layout.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/**
 *
 * @author roymt
 */
public class MainLayoutController {

    @FXML
    private VBox sidebarContainer;
    private boolean isSidebarVisible = true;

    @FXML
    private void toggleSidebar() {
        isSidebarVisible = !isSidebarVisible;
        sidebarContainer.setVisible(isSidebarVisible);
        sidebarContainer.setManaged(isSidebarVisible);
    }
}
