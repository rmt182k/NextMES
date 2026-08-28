/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package my.id.roytamba.nextmes.util;

import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

/**
 *
 * @author roymt
 */
public class ViewManager {

    // Menyimpan referensi ke jendela utama
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    // Method untuk mengganti tampilan
    public static void changeScene(String fxmlPath) {
        try {
            URL fxmlLocation = ViewManager.class.getResource(fxmlPath);
            if (fxmlLocation == null) {
                throw new RuntimeException("File FXML tidak ditemukan: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(fxmlLocation);

            // Mengganti isi scene saat ini dengan root dari FXML baru
            primaryStage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Gagal memuat tampilan: " + fxmlPath);
        }
    }
}
