package ru.kuzmina.cloud.model;

public enum MessageType {
    LIST("list"),
    FILE("file"),
    DROP("drop"),
    REQUEST_FILE("request_file"),
    LIST_PATHS("list_paths");

    private final String name;

    MessageType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
