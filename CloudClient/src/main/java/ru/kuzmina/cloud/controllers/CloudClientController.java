package ru.kuzmina.cloud.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import lombok.extern.slf4j.Slf4j;
import ru.kuzmina.cloud.model.*;
import ru.kuzmina.cloud.network.Net;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;


@Slf4j
public class CloudClientController implements Initializable {
    @FXML
    public ListView<Path> serverFileView;
    @FXML
    public ListView<Path> clientFileView;
    @FXML
    public Button delete;
    @FXML
    public Button mkdir;
    @FXML
    public Button move;
    @FXML
    public Button copy;
    @FXML
    public Label clientRoot;
    @FXML
    public MenuItem dropMenu;
    @FXML
    public MenuItem mkdirMenu;

    private static final String LOCAL_FILE_PATH = "files";
    private Net net;

    private Path localDirectory;
    private Path currentDirectory;

    ListView<Path> selectedList;

    private List<Path> serverFilesList;

    private void read() {
        while (true) {
            try {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                AbstractMessage command = net.read();
                log.info("received: {} message", command.getMessageType().getName());
                switch (command.getMessageType()) {
                    case LIST_PATHS -> {
                        ListMessagePaths cmdList = (ListMessagePaths) command;
                        serverFilesList = cmdList.getFilesList();
                        Platform.runLater(() -> {
                            serverFileView.getItems().clear();
                            serverFileView.getItems().addAll(serverFilesList);
                            setDifferentFontsForDir(serverFileView);
                        });
                    }
                    case LIST -> {
                        ListMessage cmdList = (ListMessage) command;
                        serverFilesList = getPathsFromStrings(cmdList.getFilesList());
                        Platform.runLater(() -> {
                            serverFileView.getItems().clear();
                            serverFileView.getItems().addAll(serverFilesList);
                            setDifferentFontsForDir(serverFileView);
                        });
                    }
                    case FILE -> {
                        FileMessage cmdFile = (FileMessage) command;
                        getFile(cmdFile);
                    }
                    default -> log.error("No such command: {}", command.getMessageType().getName());
                }
            } catch (ClassNotFoundException | IOException e) {
                log.error("Error reading command");
                net.closeConnection();
                break;
            }
        }
    }

    private List<Path> getPathsFromStrings(List<String> filesList) {
        return filesList.stream().map(Path::of).toList();
    }

    private void getFile(FileMessage cmdFile) {
        try {
            Files.write(Path.of(LOCAL_FILE_PATH, cmdFile.getName()), cmdFile.getBytes());
            updateLocalFilesList(Path.of(LOCAL_FILE_PATH));
        } catch (IOException e) {
            log.error("Error copy file ...");
        }
    }

    private Path getStartDir(Path localFilePath) {
        Path startDir;
        if (localFilePath.equals(Path.of(".."))) {
            startDir = this.localDirectory;
        } else {
            startDir = localFilePath;
        }
        return startDir;
    }


    private void updateLocalFilesList(Path localFilePath) {
        List<Path> localFilesList = getClientFilesList(localFilePath);
        updateLocalFileListViewData(localFilePath, localFilesList);
    }

    private List<Path> getClientFilesList(Path localFilePath) {
        Path startDir = getStartDir(localFilePath);
        try (Stream<Path> filesWalk = Files.walk(startDir, 1)) {
            return filesWalk
                    .filter(p -> !p.equals(startDir))
                    .filter(p -> !p.toFile().isHidden())
                    .sorted((p1, p2) -> (p1.toFile().isDirectory() == p2.toFile().isDirectory()) ? 0 : (p2.toFile().isDirectory() ? 1 : -1))
                    .toList();
        } catch (IOException e) {
            log.error("Error getting local file list ...");
            e.printStackTrace();
            return null;
        }
    }

    private void updateLocalFileListViewData(Path localFilePath, List<Path> localFilesList) {
        Platform.runLater(() -> {
            clientRoot.setText("path: " + localFilePath);
            clientFileView.getItems().clear();
            if (!localFilePath.equals(this.localDirectory)) {
                clientFileView.getItems().add(0, Path.of(".."));
            }
            clientFileView.getItems().addAll(localFilesList);
        });
        setDifferentFontsForDir(clientFileView);
    }


    private void setDifferentFontsForDir(ListView<Path> updatedList) {
        updatedList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Path path, boolean b) {
                super.updateItem(path, b);
                if (b || path == null) {
                    setText(null);
                } else {
                    if (path.toFile().isDirectory()) {
                        setFont(Font.font("Courier New", FontWeight.BOLD, 16));
                    }
                    setText(path.toFile().getName());
                }
            }
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            net = Net.getINSTANCE();
            localDirectory = Path.of(LOCAL_FILE_PATH);
            updateLocalFilesList(localDirectory);

            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void moveClicked() {
        copyClicked();
        dropClicked();
    }

    @FXML
    public void copyClicked() {
        String fileName = clientFileView.getSelectionModel().getSelectedItem().toString();
        Path file = clientFileView.getSelectionModel().getSelectedItem();
        if (fileName.isBlank()) {
            return;
        }
        try {
            if (selectedList.getId().equals("serverFileView")) {
                net.write(new RequestFileMessage(fileName));
            } else {
              //  FileMessage cmdFile = new FileMessage(Path.of(fileName));
                FileMessage cmdFile = new FileMessage(file);
                net.write(cmdFile);
            }
        } catch (IOException e) {
            log.error("Copy file error ...");
            e.printStackTrace();
        }
    }

    @FXML
    public void dropClicked() {
        String fileName = selectedList.getSelectionModel().getSelectedItem().toString();
        if (fileName.isBlank()) {
            return;
        }
        try {
            if (selectedList.getId().equals("serverFileView")) {
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
        Path filePath = Path.of(fileName);
        try {
            Files.delete(filePath);
        } catch (DirectoryNotEmptyException e) {
            Platform.runLater(() -> {Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Вы пытаетесь удалить директорию, содержащую файлы или другие директории",
                    ButtonType.CLOSE);
                alert.show();
            });
        }
        updateLocalFilesList(currentDirectory);
    }

    public void onClientMouseClicked(MouseEvent action) {
        selectedList = clientFileView;
        Path selectedFile = clientFileView.getSelectionModel().getSelectedItem();
        if (action.getClickCount() == 2 && selectedFile.toFile().isDirectory()) {
            if (selectedFile.toString().equals("..")) {
                selectedFile = currentDirectory.getParent();
                currentDirectory = selectedFile;
            } else {
                currentDirectory = selectedFile;
            }
            updateLocalFilesList(selectedFile);
        }
    }

    public void onServerMouseClicked(MouseEvent mouseEvent) {
        selectedList = serverFileView;
    }
//новый код для хранилища без списка серверной части,
// с синхронизацией папок как в настоящих файловых облаках

    @FXML
    public void onMkdir(ActionEvent actionEvent) {

    }

    @FXML
    public void onDrop(ActionEvent actionEvent) {
        String fileName = clientFileView.getSelectionModel().getSelectedItem().toString();
        if (fileName.isBlank()) {
            return;
        }
        try {
//            net.write(new DropMessage(Path.of(fileName)));
            dropLocalFile(fileName);
            syncServer();
        } catch (IOException e) {
            log.error("Drop file error ...");
            e.printStackTrace();
        }

    }

    private void syncServer() {
        // net.write(new ListMessage());
        List<Path> clientFilesList = getClientFilesList(localDirectory);
        // serverFilesList.compareTo(clientFilesList);
    }


}
