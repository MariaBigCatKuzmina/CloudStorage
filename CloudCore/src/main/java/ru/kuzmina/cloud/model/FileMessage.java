package ru.kuzmina.cloud.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage{
    private final String name;
    private final byte[] bytes;
    private final String filePath;

    public FileMessage(Path filePath) throws IOException {
        this.name = filePath.getFileName().toString();

        if (!filePath.getParent().equals(filePath.getName(0))) {
            this.filePath = filePath.getParent().toString()
                    .substring(filePath.getName(0).toString().length() + 1);
        } else {
            this.filePath = null;
        }
        bytes = Files.readAllBytes(filePath);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.FILE;
    }

    public String getName() {
        return name;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getFilePath() {
        return filePath;
    }
}
