module ru.kuzmina.cloudclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    opens ru.kuzmina.cloud to javafx.fxml;
    exports ru.kuzmina.cloud;
    exports ru.kuzmina.cloud.controllers;
    opens ru.kuzmina.cloud.controllers to javafx.fxml;
}