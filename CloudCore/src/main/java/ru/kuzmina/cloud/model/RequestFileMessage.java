package ru.kuzmina.cloud.model;

public class RequestFileMessage extends AbstractMessage{
    private final String file;

    public RequestFileMessage(String file) {
        this.file = file;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.REQUEST_FILE;
    }

    public String getFile() {
        return file;
    }
}
