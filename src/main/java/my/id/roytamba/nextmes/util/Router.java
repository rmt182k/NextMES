/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package my.id.roytamba.nextmes.util;

import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

/**
 *
 * @author roymt
 */
public class Router {

    // Menyimpan referensi ke layout utama aplikasi
    private static BorderPane mainLayout;

    public static void setMainLayout(BorderPane layout) {
        mainLayout = layout;
    }

    // Method ini HANYA mengganti bagian TENGAH dari BorderPane
    public static void navigate(AppRoute route) {
        if (mainLayout == null) {
            System.err.println("Error: MainLayout belum diinisialisasi di Router!");
            return;
        }

        try {
            URL fxmlLocation = Router.class.getResource(route.getFxmlPath());
            if (fxmlLocation == null) {
                throw new RuntimeException("File FXML tidak ditemukan: " + route.getFxmlPath());
            }

            Parent content = FXMLLoader.load(fxmlLocation);

            // Menyisipkan halaman baru ke tengah Sidebar Layout
            mainLayout.setCenter(content);

        } catch (Exception e) {
            System.err.println("Gagal memuat rute: " + route.name());
            e.printStackTrace();
        }
    }
}
