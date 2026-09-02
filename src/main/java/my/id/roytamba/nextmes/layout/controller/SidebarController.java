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

public class SidebarController {

    @FXML private VBox sidebar;
    @FXML private Label lblBrand;
    @FXML private Region spacer;
    @FXML private Button btnToggle;
    
    @FXML private Button btnMonitor;
    @FXML private Button btnGenerator;
    @FXML private Button btnSettings;
    @FXML private Button btnUtility;
    @FXML private Button btnAlarm;

    private final String ACTIVE_STYLE = "-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-cursor: hand;";
    private final String INACTIVE_STYLE = "-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-cursor: hand;";

    private boolean isExpanded = true;
    private final double EXPANDED_WIDTH = 220.0;
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
        } else if ("Generator".equals(defaultMenu)) {
            setActiveButton(btnGenerator);
        } else {
            setActiveButton(btnSettings);
        }
    }

    @FXML
    public void goToMonitor(ActionEvent event) {
        Router.navigate(AppRoute.MONITOR);
        setActiveButton(btnMonitor);
    }
    
    @FXML
    public void goToGenerator(ActionEvent event) {
        Router.navigate(AppRoute.GENERATOR);
        setActiveButton(btnGenerator);
    }

    @FXML
    public void goToSettings(ActionEvent event) {
        Router.navigate(AppRoute.SETTINGS);
        setActiveButton(btnSettings);
    }

    @FXML
    public void goToUtility(ActionEvent event) {
        Router.navigate(AppRoute.UTILITY);
        setActiveButton(btnUtility);
    }

    @FXML
    public void goToAlarm(ActionEvent event) {
        Router.navigate(AppRoute.ALARM);
        setActiveButton(btnAlarm);
    }

    private void setActiveButton(Button activeButton) {
        btnMonitor.setStyle(INACTIVE_STYLE);
        btnGenerator.setStyle(INACTIVE_STYLE);
        btnSettings.setStyle(INACTIVE_STYLE);
        btnUtility.setStyle(INACTIVE_STYLE);
        btnAlarm.setStyle(INACTIVE_STYLE);
        if(activeButton != null) {
            activeButton.setStyle(ACTIVE_STYLE);
        }
    }

    @FXML
    public void toggleSidebar(ActionEvent event) {
        if (isExpanded) {
            lblBrand.setVisible(false);
            lblBrand.setManaged(false);
            spacer.setManaged(false);

            btnMonitor.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            btnGenerator.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            btnSettings.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            btnUtility.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            btnAlarm.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            animateSidebar(COLLAPSED_WIDTH);
            isExpanded = false;

        } else {
            lblBrand.setVisible(true);
            lblBrand.setManaged(true);
            spacer.setManaged(true);

            btnMonitor.setContentDisplay(ContentDisplay.LEFT);
            btnGenerator.setContentDisplay(ContentDisplay.LEFT);
            btnSettings.setContentDisplay(ContentDisplay.LEFT);
            btnUtility.setContentDisplay(ContentDisplay.LEFT);
            btnAlarm.setContentDisplay(ContentDisplay.LEFT);

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
