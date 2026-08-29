package my.id.roytamba.nextmes.module.settings.pc.model;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PcModel {

    private final StringProperty id;
    private final StringProperty category;
    private final StringProperty name;
    private final StringProperty ip;

    // Menampung seluruh kolom dari CSV
    private final Map<String, String> details;

    public PcModel(String id, String category, String name, String ip, Map<String, String> details) {
        this.id = new SimpleStringProperty(id);
        this.category = new SimpleStringProperty(category);
        this.name = new SimpleStringProperty(name);
        this.ip = new SimpleStringProperty(ip);
        this.details = details != null ? details : new HashMap<>();
    }

    public String getId() {
        return id.get();
    }

    public StringProperty idProperty() {
        return id;
    }

    public String getCategory() {
        return category.get();
    }

    public StringProperty categoryProperty() {
        return category;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getIp() {
        return ip.get();
    }

    public StringProperty ipProperty() {
        return ip;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
