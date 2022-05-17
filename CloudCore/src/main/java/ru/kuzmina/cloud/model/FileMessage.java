package ru.kuzmina.cloud.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage{
    private final String name;
    private final byte[] bytes;

    public FileMessage(Path filePath) throws IOException {
        this.name = filePath.getFileName().toString();
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
}
