package ru.kuzmina.cloud.model;

import java.nio.file.Path;

public class DropMessage extends AbstractMessage{

    private final String file;

    public DropMessage(Path filePath) {
        this.file = filePath.getFileName().toString();
    }

    public String getFile() {
        return file;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.DROP;
    }
}
