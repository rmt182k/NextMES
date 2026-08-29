package my.id.roytamba.nextmes.module.settings.database.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for Database Settings
 * @author roymt
 */
public class DatabaseController {

    private final String CONFIG_FILE_PATH = "src/main/resources/config.properties";

    @FXML
    private ComboBox<String> cmbDbType;
    @FXML
    private TextField txtHost;
    @FXML
    private TextField txtPort;
    @FXML
    private TextField txtDb;
    @FXML
    private TextField txtUser;
    @FXML
    private PasswordField txtPass;

    @FXML
    public void initialize() {
        setupDatabaseTypeListener();
        loadDatabaseSettings();
    }

    private void setupDatabaseTypeListener() {
        cmbDbType.getItems().addAll("MySQL", "PostgreSQL", "MariaDB", "MSSQL", "H2", "SQLite");
        cmbDbType.setValue("MySQL");
        txtHost.setText("localhost");
        txtPort.setText("3306");

        cmbDbType.setOnAction(e -> {
            String selectedDb = cmbDbType.getValue();
            if (selectedDb == null) {
                return;
            }

            txtHost.setDisable(false);
            txtPort.setDisable(false);

            switch (selectedDb) {
                case "MySQL":
                case "MariaDB":
                    txtPort.setText("3306");
                    break;
                case "PostgreSQL":
                    txtPort.setText("5432");
                    break;
                case "MSSQL":
                    txtPort.setText("1433");
                    break;
                case "H2":
                    txtPort.setText("9092");
                    break;
                case "SQLite":
                    txtPort.setText("");
                    txtHost.setText("");
                    txtHost.setDisable(true);
                    txtPort.setDisable(true);
                    break;
            }
        });
    }

    private void loadDatabaseSettings() {
        File configFile = new File(CONFIG_FILE_PATH);

        if (!configFile.exists()) {
            return;
        }

        try (FileInputStream in = new FileInputStream(configFile)) {
            Properties props = new Properties();
            props.load(in);

            if (props.getProperty("db.type") != null) {
                cmbDbType.setValue(props.getProperty("db.type"));
                txtHost.setText(props.getProperty("db.host", ""));
                txtPort.setText(props.getProperty("db.port", ""));
                txtDb.setText(props.getProperty("db.name", ""));
                txtUser.setText(props.getProperty("db.username", ""));
                txtPass.setText(props.getProperty("db.password", ""));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void handleTestConnection(ActionEvent event) {
        String dbType = cmbDbType.getValue();
        String host = txtHost.getText().trim();
        String port = txtPort.getText().trim();
        String dbName = txtDb.getText().trim();
        String user = txtUser.getText().trim();
        String pass = txtPass.getText();

        if (dbType == null || dbName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validasi", "Tipe dan Nama Database wajib diisi!");
            return;
        }

        try {
            String jdbcUrl = getJdbcUrl(dbType, host, port, dbName);
            Class.forName(getDriverClass(dbType));

            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
                if (conn != null) {
                    String finalHost = (host.isEmpty()) ? "localhost" : host;
                    showAlert(Alert.AlertType.INFORMATION, "Sukses", "Koneksi berhasil ke " + finalHost);
                }
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Koneksi Gagal", ex.getMessage());
        }
    }

    @FXML
    public void handleSaveDatabase(ActionEvent event) {
        String dbType = cmbDbType.getValue();
        String host = txtHost.getText().trim();
        if (host.isEmpty() && !"SQLite".equalsIgnoreCase(dbType)) {
            host = "localhost";
        }
        String port = txtPort.getText().trim();
        String dbName = txtDb.getText().trim();
        String user = txtUser.getText().trim();
        String pass = txtPass.getText();

        if (dbType == null || dbName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validasi Gagal", "Tipe Database dan Nama Database tidak boleh kosong!");
            return;
        }

        saveConfig(dbType, host, port, dbName, user, pass);
    }

    private void saveConfig(String dbType, String host, String port, String dbName, String user, String pass) {
        File configFile = new File(CONFIG_FILE_PATH);
        Properties props = new Properties();

        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        props.setProperty("db.type", dbType);
        props.setProperty("db.host", host);
        props.setProperty("db.port", port);
        props.setProperty("db.name", dbName);
        props.setProperty("db.username", user);
        props.setProperty("db.password", pass);

        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Konfigurasi Aplikasi");
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Konfigurasi Database berhasil disimpan.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan konfigurasi: " + ex.getMessage());
        }
    }

    private String getJdbcUrl(String dbType, String host, String port, String dbName) {
        String finalHost = (host == null || host.trim().isEmpty()) ? "localhost" : host.trim();
        switch (dbType) {
            case "MySQL":
                return "jdbc:mysql://" + finalHost + ":" + port + "/" + dbName;
            case "PostgreSQL":
                return "jdbc:postgresql://" + finalHost + ":" + port + "/" + dbName;
            case "MariaDB":
                return "jdbc:mariadb://" + finalHost + ":" + port + "/" + dbName;
            case "MSSQL":
                return "jdbc:sqlserver://" + finalHost + ":" + port + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true;";
            case "H2":
                return "jdbc:h2:./" + dbName;
            case "SQLite":
                return "jdbc:sqlite:" + dbName + ".db";
            default:
                return "";
        }
    }

    private String getDriverClass(String dbType) {
        switch (dbType) {
            case "MySQL":
                return "com.mysql.cj.jdbc.Driver";
            case "PostgreSQL":
                return "org.postgresql.Driver";
            case "MariaDB":
                return "org.mariadb.jdbc.Driver";
            case "MSSQL":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "H2":
                return "org.h2.Driver";
            case "SQLite":
                return "org.sqlite.JDBC";
            default:
                return "";
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
