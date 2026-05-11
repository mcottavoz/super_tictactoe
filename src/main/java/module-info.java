module com.supermorpion {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    opens com.supermorpion to javafx.fxml;
    opens com.supermorpion.view to javafx.fxml;

    exports com.supermorpion;
    exports com.supermorpion.model;
    exports com.supermorpion.ai;
    exports com.supermorpion.online;
}
