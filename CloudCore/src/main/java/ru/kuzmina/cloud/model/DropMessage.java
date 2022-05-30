package ru.kuzmina.cloud.model;

public class DropMessage extends AbstractMessage{

    private final String fileName;
    private final String file;

    public DropMessage(FileData fileName) {
        this.fileName = fileName.getFileName();
        this.file = fileName.toString();
    }

    public String getFileName() {
        return fileName;
    }

    public String getFile() {
        return file;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.DROP;
    }
}
