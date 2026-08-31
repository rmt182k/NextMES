package my.id.roytamba.nextmes.module.utility.fileencoder.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Base64;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;

/**
 * Controller for File to Base64 Encoder
 * @author roymt
 */
public class FileEncoderController {

    @FXML
    private Label lblFileInfo;
    @FXML
    private ImageView imgPreview;
    @FXML
    private TextArea txtOutput;

    private File selectedFile;

    @FXML
    public void initialize() {
        // Initialization if needed
    }

    @FXML
    public void handleChooseFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih File (Gambar/Lainnya)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        selectedFile = fileChooser.showOpenDialog(txtOutput.getScene().getWindow());

        if (selectedFile != null) {
            String fileName = selectedFile.getName();
            long fileSizeInBytes = selectedFile.length();
            long fileSizeInKB = fileSizeInBytes / 1024;
            lblFileInfo.setText(fileName + " (" + fileSizeInKB + " KB)");

            // Try to load as image preview if it's an image
            try {
                String lowerName = fileName.toLowerCase();
                if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") 
                        || lowerName.endsWith(".gif") || lowerName.endsWith(".bmp")) {
                    Image image = new Image(selectedFile.toURI().toString());
                    imgPreview.setImage(image);
                } else {
                    imgPreview.setImage(null);
                }
            } catch (Exception e) {
                imgPreview.setImage(null);
            }

            processFileToBase64(selectedFile);
        }
    }

    private void processFileToBase64(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String base64String = Base64.getEncoder().encodeToString(fileContent);
            txtOutput.setText(base64String);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membaca/mengkonversi file:\n" + e.getMessage());
        }
    }

    @FXML
    public void handleCopyToClipboard(ActionEvent event) {
        String base64Text = txtOutput.getText();
        if (base64Text != null && !base64Text.isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(base64Text);
            clipboard.setContent(content);
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Teks Base64 berhasil disalin ke clipboard!");
        } else {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Tidak ada hasil konversi untuk disalin.");
        }
    }

    @FXML
    public void handleClear(ActionEvent event) {
        selectedFile = null;
        lblFileInfo.setText("Tidak ada file yang dipilih");
        imgPreview.setImage(null);
        txtOutput.clear();
    }

    @FXML
    public void handleDecodeToFile(ActionEvent event) {
        String base64Text = txtOutput.getText().trim();
        if (base64Text.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Data Base64 kosong! Paste data Base64 terlebih dahulu.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan File Hasil Decode");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Image", "*.jpg"),
                new FileChooser.ExtensionFilter("PDF Document", "*.pdf"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File fileToSave = fileChooser.showSaveDialog(txtOutput.getScene().getWindow());

        if (fileToSave != null) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(base64Text);
                try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                    fos.write(decodedBytes);
                }
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "File berhasil disimpan di:\n" + fileToSave.getAbsolutePath());
                
                // Coba update preview jika itu gambar
                String lowerName = fileToSave.getName().toLowerCase();
                if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") 
                        || lowerName.endsWith(".gif") || lowerName.endsWith(".bmp")) {
                    Image image = new Image(fileToSave.toURI().toString());
                    imgPreview.setImage(image);
                    lblFileInfo.setText("Decode: " + fileToSave.getName());
                }
                
            } catch (IllegalArgumentException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Teks bukan format Base64 yang valid!");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan file:\n" + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
