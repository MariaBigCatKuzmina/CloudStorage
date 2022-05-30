package ru.kuzmina.cloud.model;

public class RequestFileMessage extends AbstractMessage{
    private final String file;

    public RequestFileMessage(FileData file) {
        this.file = file.toString();
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.REQUEST_FILE;
    }

    public String getFile() {
        return file;
    }
}
