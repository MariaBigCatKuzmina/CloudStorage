package ru.kuzmina.cloud.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class ListMessagePaths extends AbstractMessage{
    private final List<Path> filesList;

    public ListMessagePaths(Path rootPath) throws IOException {
        try (Stream<Path> filesWalk = Files.walk(rootPath)) {
            this.filesList = filesWalk
                    .filter(p -> !p.equals(rootPath))
                    .filter(p -> !p.toFile().isHidden())
                    .sorted((p1, p2) -> (p1.toFile().isDirectory() == p2.toFile().isDirectory()) ? 0 : (p2.toFile().isDirectory() ? 1 : -1))
                    .toList();
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.LIST_PATHS;
    }

    public List<Path> getFilesList() {
        return filesList;
    }
}
