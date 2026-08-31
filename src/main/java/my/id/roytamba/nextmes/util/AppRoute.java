/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package my.id.roytamba.nextmes.util;

/**
 *
 * @author roymt
 */
public enum AppRoute {
    DASHBOARD("/view/pages/Dashboard.fxml"),
    SETTINGS("/view/module/settings/Settings.fxml"),
    MONITOR("/view/module/monitor/Monitor.fxml"),
    GENERATOR("/view/module/generator/Generator.fxml"),
    UTILITY("/view/module/utility/Utility.fxml");

    private final String fxmlPath;

    AppRoute(String fxmlPath) {
        this.fxmlPath = fxmlPath;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }
}
