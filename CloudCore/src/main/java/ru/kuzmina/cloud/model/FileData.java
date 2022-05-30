package ru.kuzmina.cloud.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class FileData implements Serializable {
    private String fileName;
    private String parent;
    private String fullName;
    private long changeDate;
    private long creationDate;
    private final boolean isDirectory;

    public String getFileName() {
        return fileName;
    }

    public String getParent() {
        return parent;
    }

    public long getChangeDate() {
        return changeDate;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getFullName() {
        return fullName;
    }

    public FileData(Path file) {
        this.fileName = file.getFileName().toString();
        if (!file.getName(0).equals(file.getParent()) && file.getParent() != null) {
            this.parent = file.getParent().toString().substring(file.getName(0).toString().length() + 1);
        } else {
            parent = "";
        }
        this.changeDate = file.toFile().lastModified();
        try {
            BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
            creationDate = fileAttributes.creationTime().to(TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.isDirectory = file.toFile().isDirectory();
        this.fullName = file.toString();
    }

    @Override
    public String toString() {
        if (!parent.equals("")) {
            return this.parent + File.separatorChar + fileName;
        } else {
            return fileName;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileData fileData = (FileData) o;
        if (isDirectory && fileData.isDirectory){
            return fileName.equals(fileData.fileName) &&
                    parent.equals(fileData.parent) &&
                    fullName.equals(fileData.fullName);
        }
        return changeDate == fileData.changeDate &&
                creationDate == fileData.creationDate &&
                isDirectory == fileData.isDirectory &&
                fileName.equals(fileData.fileName) &&
                parent.equals(fileData.parent) &&
                fullName.equals(fileData.fullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, parent, fullName, changeDate, creationDate, isDirectory);
    }
}
