package my.id.roytamba.nextmes.module.utility.fileencoder.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import my.id.roytamba.nextmes.module.utility.fileencoder.util.EncodingHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
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
    private Label lblDocPreview;
    @FXML
    private ComboBox<String> cmbFormat;
    @FXML
    private TextArea txtOutput;

    private File selectedFile;

    @FXML
    public void initialize() {
        cmbFormat.getItems().addAll("Base64", "Hexadecimal (Base16)", "Base32", "Base58", "Ascii85 (Base85)");
        cmbFormat.setValue("Base64");
        
        cmbFormat.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedFile != null) {
                processFileToString(selectedFile);
            }
        });
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
                    lblDocPreview.setVisible(false);
                } else {
                    imgPreview.setImage(null);
                    lblDocPreview.setVisible(true);
                }
            } catch (Exception e) {
                imgPreview.setImage(null);
                lblDocPreview.setVisible(true);
            }

            processFileToString(selectedFile);
        }
    }

    private void processFileToString(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String format = cmbFormat.getValue();
            String encodedString = "";
            
            switch (format) {
                case "Hexadecimal (Base16)":
                    encodedString = EncodingHelper.encodeHex(fileContent);
                    break;
                case "Base32":
                    encodedString = EncodingHelper.encodeBase32(fileContent);
                    break;
                case "Base58":
                    encodedString = EncodingHelper.encodeBase58(fileContent);
                    break;
                case "Ascii85 (Base85)":
                    encodedString = EncodingHelper.encodeAscii85(fileContent);
                    break;
                case "Base64":
                default:
                    encodedString = EncodingHelper.encodeBase64(fileContent);
                    break;
            }
            
            txtOutput.setText(encodedString);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membaca/mengkonversi file:\n" + e.getMessage());
        }
    }

    @FXML
    public void handleCopyToClipboard(ActionEvent event) {
        String text = txtOutput.getText();
        if (text != null && !text.isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Teks berhasil disalin ke clipboard!");
        } else {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Tidak ada hasil konversi untuk disalin.");
        }
    }

    @FXML
    public void handleClear(ActionEvent event) {
        selectedFile = null;
        lblFileInfo.setText("Tidak ada file yang dipilih");
        imgPreview.setImage(null);
        lblDocPreview.setVisible(false);
        txtOutput.clear();
    }

    @FXML
    public void handleDecodeToFile(ActionEvent event) {
        String textToDecode = txtOutput.getText().trim();
        if (textToDecode.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Data string kosong! Paste data teks terlebih dahulu.");
            return;
        }

        // Coba deteksi magic number jika memungkinkan untuk menyarankan format
        String suggestedExt = "bin";
        String format = cmbFormat.getValue();
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan File Hasil Decode");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Semua File (*.*)", "*.*"),
                new FileChooser.ExtensionFilter("PNG Image", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Image", "*.jpg"),
                new FileChooser.ExtensionFilter("PDF Document", "*.pdf"),
                new FileChooser.ExtensionFilter("Word Document", "*.docx"),
                new FileChooser.ExtensionFilter("Excel Document", "*.xlsx")
        );

        File fileToSave = fileChooser.showSaveDialog(txtOutput.getScene().getWindow());

        if (fileToSave != null) {
            try {
                byte[] decodedBytes = null;
                switch (format) {
                    case "Hexadecimal (Base16)":
                        decodedBytes = EncodingHelper.decodeHex(textToDecode);
                        break;
                    case "Base32":
                        decodedBytes = EncodingHelper.decodeBase32(textToDecode);
                        break;
                    case "Base58":
                        decodedBytes = EncodingHelper.decodeBase58(textToDecode);
                        break;
                    case "Ascii85 (Base85)":
                        decodedBytes = EncodingHelper.decodeAscii85(textToDecode);
                        break;
                    case "Base64":
                    default:
                        decodedBytes = EncodingHelper.decodeBase64(textToDecode);
                        break;
                }
                
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
                    lblDocPreview.setVisible(false);
                    lblFileInfo.setText("Decode: " + fileToSave.getName());
                } else {
                    imgPreview.setImage(null);
                    lblDocPreview.setVisible(true);
                    lblFileInfo.setText("Decode: " + fileToSave.getName());
                }
                
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal men-decode atau menyimpan file:\n" + e.getMessage());
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
