package ru.kuzmina.cloud.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
//import lombok.extern.slf4j.Slf4j;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;
import ru.kuzmina.cloud.model.*;
import ru.kuzmina.cloud.network.Net;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

@Slf4j
public class CloudClientController implements Initializable {
    @FXML
    public ListView<String> serverFileList;
    @FXML
    public ListView<String> clientFilesList;
    @FXML
    public Button delete;
    @FXML
    public Button mkdir;
    @FXML
    public Button move;
    @FXML
    public Button copy;

    private static final String LOCAL_FILE_PATH = "files";
    private Net net;

    private ListView<String> selectedFileList;

    private void read() {
        while (true) {
            try {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                AbstractMessage command = net.read();
                switch (command.getMessageType()) {
                    case LIST -> {
                        ListMessage cmdList = (ListMessage) command;
                        Platform.runLater(() -> {
                            serverFileList.getItems().clear();
                            serverFileList.getItems().addAll(cmdList.getFilesList());
                        });
                    }
                    case FILE -> {
                        FileMessage cmdFile = (FileMessage) command;
                        getFile(cmdFile);
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                log.error("Error reading command");
                net.closeConnection();
                break;
            }
        }
    }

    private void getFile(FileMessage cmdFile) {
        try {
            Files.write(Path.of(LOCAL_FILE_PATH, cmdFile.getName()), cmdFile.getBytes());
            readLocalFilesList(LOCAL_FILE_PATH);
        } catch (IOException e) {
            log.error("Error copy file ...");
        }
    }

    private void readLocalFilesList(String localFilePath) {
        File dir = new File(localFilePath);
        String[] localFilesList = dir.list();

        Platform.runLater(() -> {
            clientFilesList.getItems().clear();
            clientFilesList.getItems().addAll(localFilesList);
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            net = Net.getINSTANCE();
            readLocalFilesList(LOCAL_FILE_PATH);
            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void moveClicked(ActionEvent actionEvent) {
    }

    @FXML
    public void copyClicked(ActionEvent actionEvent) {
        String fileName = selectedFileList.getSelectionModel().getSelectedItem();
        if (fileName.isBlank()) {
            return;
        }
        try {
            if (selectedFileList.getId().equals("clientFileList")) {
                FileMessage cmdFile = new FileMessage(Path.of(LOCAL_FILE_PATH, fileName));
                net.write(cmdFile);
            } else {
                RequestFileMessage cmdRequest = new RequestFileMessage(fileName);
                net.write(cmdRequest);
            }
        } catch (IOException e) {
            log.error("Drop file error ...");
            e.printStackTrace();
        }

    }

    @FXML
    public void dropClicked(ActionEvent actionEvent) {
//        System.out.println(ClientApp.instance.getClientStage().getScene().getFocusOwner().getId());
        String fileName = selectedFileList.getSelectionModel().getSelectedItem();
        if (fileName.isBlank()) {
            return;
        }
        try {
            if (selectedFileList.getId().equals("serverFileList")) {
                net.write(new DropMessage(Path.of(fileName)));
            } else {
                dropLocalFile(fileName);
            }
        } catch (IOException e) {
            log.error("Drop file error ...");
            e.printStackTrace();
        }

    }

    private void dropLocalFile(String fileName) throws IOException {
        Path filePath = Path.of(LOCAL_FILE_PATH, fileName);
        Files.delete(filePath);
        readLocalFilesList(LOCAL_FILE_PATH);
    }

    public void onClientMouseClicked(MouseEvent mouseEvent) {
        selectedFileList = clientFilesList;
    }

    public void onServerMouseClicked(MouseEvent mouseEvent) {
        selectedFileList = serverFileList;
    }
}
