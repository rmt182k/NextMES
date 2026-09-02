package my.id.roytamba.nextmes.module.alarm.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ReminderModel {
    private final StringProperty id;
    private final StringProperty title;
    private final StringProperty time; // Format HH:mm
    private final BooleanProperty active;

    public ReminderModel(String id, String title, String time, boolean active) {
        this.id = new SimpleStringProperty(id);
        this.title = new SimpleStringProperty(title);
        this.time = new SimpleStringProperty(time);
        this.active = new SimpleBooleanProperty(active);
    }

    public String getId() { return id.get(); }
    public StringProperty idProperty() { return id; }

    public String getTitle() { return title.get(); }
    public StringProperty titleProperty() { return title; }

    public String getTime() { return time.get(); }
    public StringProperty timeProperty() { return time; }

    public boolean isActive() { return active.get(); }
    public BooleanProperty activeProperty() { return active; }
    public void setActive(boolean isActive) { this.active.set(isActive); }
}
