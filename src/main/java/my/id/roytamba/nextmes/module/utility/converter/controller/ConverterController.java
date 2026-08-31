package my.id.roytamba.nextmes.module.utility.converter.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

/**
 * Controller for Decoder/Converter Utility
 * @author roymt
 */
public class ConverterController {

    @FXML
    private TextArea txtInput;
    @FXML
    private ComboBox<String> cmbAlgorithm;
    @FXML
    private TextArea txtOutput;
    @FXML
    private Label lblDetectedFormat;

    @FXML
    public void initialize() {
        cmbAlgorithm.getItems().addAll(
                "Auto-Detect (Smart)",
                "Base64 -> GZIP", 
                "Base64 -> ZLIB", 
                "Plain Base64",
                "Hex -> GZIP",
                "Hex -> ZLIB",
                "Plain Hex"
        );
        cmbAlgorithm.setValue("Auto-Detect (Smart)");
    }

    @FXML
    public void handleProcess(ActionEvent event) {
        String input = txtInput.getText();
        if (input == null || input.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Input data tidak boleh kosong!");
            return;
        }
        
        String cleanInput = input.trim();
        boolean shouldDecode = false;
        
        // 1. Cek apakah ini Hex (hanya karakter hex) atau Base64 yang valid, 
        // dan tidak mengandung karakter plain text umum (seperti spasi di tengah, kurawal, dll)
        if (cleanInput.startsWith("0x") || cleanInput.matches("^[0-9a-fA-F]{16,}$")) {
            shouldDecode = true; // Jelas ini deretan Hex panjang
        } else if (cleanInput.startsWith("H4sI") || cleanInput.startsWith("eJw") || cleanInput.startsWith("eJy") || cleanInput.startsWith("eJz")) {
            shouldDecode = true; // Jelas ini Base64 GZIP / ZLIB
        } else if (cleanInput.contains("{") || cleanInput.contains("<") || cleanInput.contains("\"") || cleanInput.contains(" ")) {
            shouldDecode = false; // Ada karakter plain text JSON/XML/Spasi, pasti ini untuk di-encode
        } else if (cleanInput.matches("^[A-Za-z0-9+/]+={0,2}$") && cleanInput.length() > 20) {
            shouldDecode = true; // Looks like a long plain Base64 string
        } else {
            // Default fallback: coba decode, kalau gagal artinya plain text untuk di-encode
            shouldDecode = true;
        }

        if (shouldDecode) {
            boolean success = doDecode(cleanInput);
            if (!success) {
                // Jika gagal decode, berarti tebakan salah, mari kita coba encode saja
                doEncode(input);
            }
        } else {
            doEncode(input);
        }
    }

    private void doEncode(String input) {

        String algorithm = cmbAlgorithm.getValue();
        if (algorithm.equals("Auto-Detect (Smart)")) {
            algorithm = "Base64 -> GZIP"; // Default fallback for Encoding
            lblDetectedFormat.setText("Operasi: ENCODE (Teks -> Base64 GZIP)");
            lblDetectedFormat.setVisible(true);
        } else {
            lblDetectedFormat.setText("Operasi: ENCODE (" + algorithm + ")");
            lblDetectedFormat.setVisible(true);
        }

        try {
            byte[] inputBytes = input.getBytes("UTF-8");
            byte[] compressedBytes = null;
            
            switch (algorithm) {
                case "Base64 -> GZIP":
                case "Hex -> GZIP":
                    compressedBytes = compressGzip(inputBytes);
                    break;
                case "Base64 -> ZLIB":
                case "Hex -> ZLIB":
                    compressedBytes = compressZlib(inputBytes);
                    break;
                default:
                    compressedBytes = inputBytes;
                    break;
            }

            if (algorithm.startsWith("Hex")) {
                txtOutput.setText(bytesToHex(compressedBytes));
            } else {
                txtOutput.setText(Base64.getEncoder().encodeToString(compressedBytes));
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Encode", "Gagal melakukan encode/kompresi data:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean doDecode(String input) {
        String algorithm = cmbAlgorithm.getValue();

        try {
            if ("Auto-Detect (Smart)".equals(algorithm)) {
                algorithm = detectFormat(input);
                lblDetectedFormat.setText("Operasi: DECODE (" + algorithm + ")");
                lblDetectedFormat.setVisible(true);
            } else {
                lblDetectedFormat.setText("Operasi: DECODE (" + algorithm + ")");
                lblDetectedFormat.setVisible(true);
            }

            byte[] decodedBytes;
            if (algorithm.startsWith("Hex")) {
                decodedBytes = hexStringToByteArray(input);
            } else {
                decodedBytes = Base64.getDecoder().decode(input);
            }

            String resultText = "";

            if (algorithm.endsWith("GZIP")) {
                resultText = decompressGzip(decodedBytes);
            } else if (algorithm.endsWith("ZLIB")) {
                resultText = decompressZlib(decodedBytes);
            } else {
                resultText = new String(decodedBytes, "UTF-8");
            }

            txtOutput.setText(resultText);
            return true;

        } catch (Exception e) {
            // Silently fail if called from smart detect, we will try encoding instead
            return false;
        }
    }

    @FXML
    public void handleClear(ActionEvent event) {
        txtInput.clear();
        txtOutput.clear();
        lblDetectedFormat.setVisible(false);
    }

    private String detectFormat(String input) {
        // Cek apakah murni Hex
        String hexClean = input.replaceAll("\\s+", "").replaceAll("^0x", "");
        if (hexClean.matches("^[0-9a-fA-F]+$")) {
            if (hexClean.toUpperCase().startsWith("1F8B")) {
                return "Hex -> GZIP";
            }
            if (hexClean.toUpperCase().startsWith("78")) {
                return "Hex -> ZLIB";
            }
            return "Plain Hex";
        }
        
        // Asumsikan Base64
        if (input.startsWith("H4sI")) {
            return "Base64 -> GZIP";
        }
        if (input.startsWith("eJw") || input.startsWith("eJy") || input.startsWith("eJz")) {
            return "Base64 -> ZLIB";
        }
        return "Plain Base64";
    }

    private byte[] hexStringToByteArray(String s) {
        String cleanString = s.replaceAll("\\s+", "").replaceAll("^0x", "");
        int len = cleanString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(cleanString.charAt(i), 16) << 4)
                                 + Character.digit(cleanString.charAt(i+1), 16));
        }
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private byte[] compressGzip(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
        }
        return baos.toByteArray();
    }

    private byte[] compressZlib(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOut = new DeflaterOutputStream(baos, new Deflater())) {
            deflaterOut.write(data);
        }
        return baos.toByteArray();
    }

    private String decompressGzip(byte[] compressedBytes) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toString("UTF-8");
        }
    }

    private String decompressZlib(byte[] compressedBytes) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
             InflaterInputStream inflaterIn = new InflaterInputStream(bais, new Inflater());
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inflaterIn.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toString("UTF-8");
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
