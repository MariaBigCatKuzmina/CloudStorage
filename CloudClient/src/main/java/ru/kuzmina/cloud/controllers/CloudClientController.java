package ru.kuzmina.cloud.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import lombok.extern.slf4j.Slf4j;
import ru.kuzmina.cloud.model.*;
import ru.kuzmina.cloud.network.Net;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;


@Slf4j
public class CloudClientController implements Initializable {
    @FXML
    public ListView<FileData> serverFileView;
    @FXML
    public ListView<FileData> clientFileView;
    @FXML
    public Label clientRoot;
    @FXML
    public MenuItem dropMenu;
    @FXML
    public MenuItem mkdirMenu;
    @FXML
    public Button expandServerList;
    @FXML
    public Label syncLabel;
    @FXML
    public AnchorPane serverListPane;
    @FXML
    public HBox clientListPane;

    private static final String LOCAL_FILE_PATH = "files";
    private Net net;

    private Path localDirectory; // директория локальной папки облака
    private Path activeDirectory; // поддиректория отображаемая в ListView

    private List<FileData> serverFilesList;
    private List<FileData> clientFilesList;

    private Path recipientDir;
    private boolean toDropFilesOnServer;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            this.net = Net.getINSTANCE();

            this.localDirectory = Path.of(LOCAL_FILE_PATH);
            if (!Files.exists(localDirectory)) {
                Files.createDirectories(localDirectory);
            }
            this.activeDirectory = this.localDirectory;
            updateLocalFileListViewData(this.localDirectory);

            this.clientFilesList = getClientFilesList(this.localDirectory, Integer.MAX_VALUE);
            this.toDropFilesOnServer = true;

            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();

            Thread syncThread = new Thread(this::sync);
            syncThread.setDaemon(true);
            syncThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read() {
        while (true) {
            try {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                AbstractMessage command = net.read();
                log.info("received: {} message", command.getMessageType().getName());
                switch (command.getMessageType()) {
                    case LIST -> {
                        ListMessage cmdList = (ListMessage) command;
                        serverFilesList = cmdList.getFilesList();
                        Platform.runLater(() -> {
                            serverFileView.getItems().clear();
                            serverFileView.getItems().addAll(serverFilesList);
                            setDifferentFontsForListView(serverFileView);
                        });
                    }
                    case FILE -> {
                        FileMessage cmdFile = (FileMessage) command;
                        getFileFromCommand(cmdFile);
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

    private void getFileFromCommand(FileMessage cmdFile) {
        try {
            Files.write(localDirectory.resolve(cmdFile.getName()), cmdFile.getBytes());
            updateLocalFileListViewData(activeDirectory);
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

    private void updateLocalFileListViewData(Path localFilePath) {
        List<FileData> localFilesList = getClientFilesList(localFilePath, 1);
        updateLocalFileListView(localFilePath, localFilesList);
    }

    private List<FileData> getClientFilesList(Path localFilePath, int maxDepth) {
        Path startDir = getStartDir(localFilePath);
        List<FileData> fileList = new ArrayList<>();
        try (Stream<Path> filesWalk = Files.walk(startDir, maxDepth)) {
            List<Path> files = filesWalk
                    .filter(p -> !p.equals(startDir))
                    .filter(p -> !p.toFile().isHidden())
                    .sorted((p1, p2) -> (p1.toFile().isDirectory() == p2.toFile().isDirectory()) ? 0 : (p2.toFile().isDirectory() ? 1 : -1))
                    .toList();
            for (Path path : files) {
                fileList.add(new FileData(path));
            }
        } catch (IOException e) {
            log.error("Error getting local file list ...");
            e.printStackTrace();
        }
        return fileList;
    }

    private void updateLocalFileListView(Path localFilePath, List<FileData> localFilesList) {
        Platform.runLater(() -> {
            clientRoot.setText(" Client: " + localFilePath);
            clientFileView.getItems().clear();
            if (!localFilePath.toString().equals(this.localDirectory.toString())) {
                clientFileView.getItems().add(0, new FileData(Path.of("..")));
            }
            clientFileView.getItems().addAll(localFilesList);
        });

        setDifferentFontsForListView(clientFileView);
    }

    private void setDifferentFontsForListView(ListView<FileData> filesListView) {
        filesListView.setCellFactory(param -> {
            ListCell<FileData> cell = new ListCell<>() {
                @Override
                protected void updateItem(FileData file, boolean b) {
                    super.updateItem(file, b);
                    if (b || file == null) {
                        setText(null);
                    } else {
                        if (file.isDirectory()) {
                            setFont(Font.font("Courier New", FontWeight.BOLD, 16));
                        }
                        setText(file.getFileName());
                    }
                }
            };
            cell.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                if (isNowHovered && !cell.isEmpty()) {
                    recipientDir = Path.of(cell.getItem().getFullName());
                } else {
                    recipientDir = activeDirectory;
                }
            });
            return cell;
        });
    }

    private void dropLocalFile(FileData file) throws IOException {
        Path filePath = Path.of(file.getFullName());
        try {
            Files.delete(filePath);
        } catch (DirectoryNotEmptyException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Вы пытаетесь удалить директорию, содержащую файлы или другие директории",
                        ButtonType.CLOSE);
                alert.show();
            });
        }
        updateLocalFileListViewData(activeDirectory);
    }

    @FXML
    public void onClientMouseClicked(MouseEvent action) {
        FileData selectedFile = clientFileView.getSelectionModel().getSelectedItem();
        if (action.getClickCount() == 2 && selectedFile.isDirectory()) {
            if (selectedFile.getFullName().equals("..")) {
                selectedFile = new FileData(activeDirectory.getParent());
            }
            activeDirectory = Path.of(selectedFile.getFullName());
            updateLocalFileListViewData(Path.of(selectedFile.getFullName()));
        }
    }

//новый код для хранилища без списка серверной части,
// с синхронизацией папок как в настоящих файловых облаках

    @FXML
    public void onMkdir(ActionEvent actionEvent) {

    }

    @FXML
    public void onDrop(ActionEvent actionEvent) {
        FileData file = clientFileView.getSelectionModel().getSelectedItem();
        if (file == null) {
            return;
        }
        try {
            dropLocalFile(file);
            if (toDropFilesOnServer) {
                net.write(new DropMessage(file));
            }
        } catch (IOException e) {
            log.error("Drop file error ...");
            e.printStackTrace();
        }
    }

    private void checkAndUpdateChanges() throws IOException {
        List<FileData> clientFilesListNew = getClientFilesList(localDirectory, Integer.MAX_VALUE);
        if (!clientFilesListNew.equals(clientFilesList)) {
            Platform.runLater(() -> syncLabel.setText("Синхронизация файлов"));
            for (FileData newFile : clientFilesListNew) {
                if (!clientFilesList.contains(newFile)) {
                    net.write(new FileMessage(newFile));
                    clientFilesList.add(newFile);
                }
            }
            if (!clientFilesListNew.equals(clientFilesList)) {
                for (FileData file : clientFilesList) {
                    if (!clientFilesListNew.contains(file)) {
                        net.write(new DropMessage(file));
                    }
                }
            }
            clientFilesList = clientFilesListNew;
            updateLocalFileListViewData(activeDirectory);
            Platform.runLater(() -> syncLabel.setText("Файлы синхронизированы"));

        }
    }

    private void sync() {
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            try {
                checkAndUpdateChanges();
                Thread.sleep(5000);
            } catch (IOException e) {
                log.error("Error updating files ...");
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void onDragDropped(DragEvent dragEvent) {
        final Dragboard dragboard = dragEvent.getDragboard();
        boolean isSuccessful = false;
        if (dragboard.hasFiles()) {
            List<File> draggedFiles = dragboard.getFiles();
            for (File file : draggedFiles) {
                Path copyTo = recipientDir.resolve(file.getName());
                try {
                    copyFileTo(file.toPath(), copyTo);
                    net.write(new FileMessage(new FileData(file.toPath())));
                    clientFilesList = getClientFilesList(localDirectory, Integer.MAX_VALUE);
                } catch (IOException e) {
                    log.error("Error copy file {} ...", file.getName());
                    e.printStackTrace();
                }
            }
            isSuccessful = true;
        }
        dragEvent.setDropCompleted(isSuccessful);
        dragEvent.consume();
        updateLocalFileListViewData(activeDirectory);

    }

    private void copyFileTo(Path source, Path target) throws IOException {
        if (!Files.notExists(target)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    String.format("Файл %s существует. Заменить?", source.getFileName()),
                    ButtonType.YES, ButtonType.NO);
            if (alert.showAndWait().isPresent() && alert.getResult() == ButtonType.NO) {
                return;
            }
        }
        byte[] bytes = Files.readAllBytes(source);
        Files.write(target, bytes);
    }

    @FXML
    public void onDragOver(DragEvent dragEvent) {
        if (dragEvent.getDragboard().hasFiles()) {
            clientFileView.setStyle("-fx-background-color: #CBE6EF");
            dragEvent.acceptTransferModes(TransferMode.ANY);
        } else {
            dragEvent.consume();
        }
    }

    @FXML
    public void onDragExited(MouseDragEvent mouseDragEvent) {
        clientFileView.setStyle("-fx-background-color: #F0F2F3");
    }

    @FXML
    public void onCollapseExpandPress(ActionEvent actionEvent) {
        FileData file = serverFileView.getSelectionModel().getSelectedItem();
        try {
            net.write(new RequestFileMessage(file));
        } catch (IOException e) {
            log.error("Error requesting file ...");
            e.printStackTrace();
        }
    }
}
