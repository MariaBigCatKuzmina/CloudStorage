package ru.kuzmina.cloud.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import ru.kuzmina.cloud.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class FileHandler extends SimpleChannelInboundHandler<AbstractMessage> {
    private final Path serverRoot;

    public FileHandler(String serverRoot) {
        this.serverRoot = Path.of(serverRoot);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
   //     ctx.writeAndFlush(new ListMessage(serverRoot));
        ctx.writeAndFlush(new ListMessage(serverRoot));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) throws Exception {
        log.info("received: {} message", msg.getMessageType().getName());
        switch (msg.getMessageType()) {
            case FILE -> {
                FileMessage fileMessage = (FileMessage) msg;
                writeFile(fileMessage.getFilePath(), fileMessage.getName(), fileMessage.getBytes());
                ctx.writeAndFlush(new ListMessage(serverRoot));
            }
            case DROP -> {
                DropMessage dropMessage = (DropMessage) msg;
                Path filePath = serverRoot.resolve(dropMessage.getFile());
                if (Files.exists(filePath)) {
 //                   Path copyTo = serverRoot.resolve("trash");
 //                   copyTo = copyTo.resolve(dropMessage.getFile());
 //                   Files.copy(filePath, copyTo);
                    Files.delete(filePath);
                    ctx.writeAndFlush(new ListMessage(serverRoot));
                }
            }
            case REQUEST_FILE -> {
                RequestFileMessage cmdRequest = (RequestFileMessage) msg;
                Path filePath = serverRoot.resolve(cmdRequest.getFile());
                if (Files.exists(filePath)) {
                    FileMessage fileMessage = new FileMessage(new FileData(filePath));
                    ctx.writeAndFlush(fileMessage);
                }
            }
            default -> log.error("Couldn't resolve {} command", msg.getMessageType().getName());
        }
    }

    private void writeFile(String writePath, String fileName, byte[] fileBytes) throws IOException {
        Path copyRoot = serverRoot.resolve(writePath);
        if (!Files.exists(copyRoot)) {
            Files.createDirectory(copyRoot);
        }
        Files.write(copyRoot.resolve(fileName), fileBytes);
    }
}
