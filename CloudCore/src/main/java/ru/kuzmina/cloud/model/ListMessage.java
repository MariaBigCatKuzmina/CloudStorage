package ru.kuzmina.cloud.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ListMessage extends AbstractMessage{

    private final List<String> filesList;

    public ListMessage(Path rootPath) throws IOException {
        filesList = Files.list(rootPath).map(Path::getFileName).map(Path::toString).toList();
       // Files.list(rootPath).map(Path::subpath)
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.LIST;
    }

    public List<String> getFilesList() {
        return filesList;
    }
}
