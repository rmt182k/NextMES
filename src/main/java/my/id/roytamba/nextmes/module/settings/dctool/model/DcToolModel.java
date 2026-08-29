package my.id.roytamba.nextmes.module.settings.dctool.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DcToolModel {
    private final StringProperty id;
    private final StringProperty category;
    private final StringProperty name;
    private final StringProperty ip;

    public DcToolModel(String id, String category, String name, String ip) {
        this.id = new SimpleStringProperty(id);
        this.category = new SimpleStringProperty(category);
        this.name = new SimpleStringProperty(name);
        this.ip = new SimpleStringProperty(ip);
    }

    public String getId() { return id.get(); }
    public StringProperty idProperty() { return id; }

    public String getCategory() { return category.get(); }
    public StringProperty categoryProperty() { return category; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getIp() { return ip.get(); }
    public StringProperty ipProperty() { return ip; }
}
