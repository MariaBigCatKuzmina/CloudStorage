package ru.kuzmina.cloud.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

public class CloudClientController {
    @FXML
    public ListView serverFileList;
    @FXML
    private ListView clientFilesList;
    @FXML
    public Button delete;
    @FXML
    public Button mkdir;
    @FXML
    public Button move;
    @FXML
    public Button copy;

}
