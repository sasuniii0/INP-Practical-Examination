module INP.Examination {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens lk.ijse.gdse.client to javafx.fxml;
    opens lk.ijse.gdse.server to javafx.fxml;

    exports lk.ijse.gdse;
}