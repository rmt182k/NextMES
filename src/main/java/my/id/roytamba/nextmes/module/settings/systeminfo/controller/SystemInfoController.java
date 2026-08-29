package my.id.roytamba.nextmes.module.settings.systeminfo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for System Info settings tab.
 * @author roymt
 */
public class SystemInfoController {

    @FXML
    private Label lblAppVersion;
    @FXML
    private Label lblOs;
    @FXML
    private Label lblJavaVersion;
    @FXML
    private Label lblArch;
    @FXML
    private Label lblWorkingDir;

    @FXML
    public void initialize() {
        loadSystemInformation();
    }

    private void loadSystemInformation() {
        // App version - hardcoded for now or fetched from properties
        lblAppVersion.setText("NextMES v1.0.0");
        
        // OS Info
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        lblOs.setText(osName + " (Version: " + osVersion + ")");
        
        // Java Info
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        lblJavaVersion.setText(javaVersion + " (" + javaVendor + ")");
        
        // Architecture
        lblArch.setText(System.getProperty("os.arch"));
        
        // Working Directory
        lblWorkingDir.setText(System.getProperty("user.dir"));
    }
}
