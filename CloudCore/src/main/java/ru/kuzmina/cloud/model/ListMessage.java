package ru.kuzmina.cloud.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ListMessage extends AbstractMessage{
    private final List<FileData> filesList = new ArrayList<>();

    public ListMessage(Path rootPath) throws IOException {
        try (Stream<Path> filesWalk = Files.walk(rootPath)) {
            List<Path> pathsList = filesWalk
                    .filter(p -> !p.equals(rootPath))
                    .filter(p -> !p.toFile().isHidden())
                    .sorted((p1, p2) -> (p1.toFile().isDirectory() == p2.toFile().isDirectory()) ? 0 : (p2.toFile().isDirectory() ? 1 : -1))
                    .toList();
            for (Path path: pathsList) {
                this.filesList.add(new FileData(path));
            }
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.LIST;
    }

    public List<FileData> getFilesList() {
        return filesList;
    }
}
