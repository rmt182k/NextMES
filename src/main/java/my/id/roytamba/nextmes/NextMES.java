/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package my.id.roytamba.nextmes;

import java.net.URL;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author roymt
 */
public class NextMES extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            URL fxmlLocation = getClass().getResource("/view/layout/MainLayout.fxml");
            Parent root = FXMLLoader.load(fxmlLocation);

            // Load size from config.properties
            double width = 1000;
            double height = 650;
            java.io.File configFile = new java.io.File("src/main/resources/config.properties");
            if (configFile.exists()) {
                try (java.io.FileInputStream in = new java.io.FileInputStream(configFile)) {
                    java.util.Properties props = new java.util.Properties();
                    props.load(in);
                    width = Double.parseDouble(props.getProperty("ui.width", "1000"));
                    height = Double.parseDouble(props.getProperty("ui.height", "650"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Scene scene = new Scene(root, width, height);

            primaryStage.setTitle("NextMES");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
