package ru.kuzmina.cloud.handlers;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import lombok.extern.slf4j.Slf4j;
import ru.kuzmina.cloud.model.*;

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
        ctx.writeAndFlush(new ListMessage(serverRoot));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) throws Exception {
        log.info("received: {} message", msg.getMessageType().getName());
        switch (msg.getMessageType()) {
            case FILE -> {
                FileMessage fileMessage = (FileMessage) msg;
                Files.write(serverRoot.resolve(fileMessage.getName()), fileMessage.getBytes());
                ctx.writeAndFlush(new ListMessage(serverRoot));
            }
            case DROP -> {
                DropMessage dropMessage = (DropMessage) msg;
                Path filePath = serverRoot.resolve(dropMessage.getFile());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    ctx.writeAndFlush(new ListMessage(serverRoot));
                }
            }
            case REQUEST_FILE -> {
                RequestFileMessage cmdRequest = (RequestFileMessage) msg;
                Path filePath = serverRoot.resolve(cmdRequest.getFile());
                if (Files.exists(filePath)) {
                    FileMessage fileMessage = new FileMessage(filePath);
                    ctx.writeAndFlush(fileMessage);
                }
            }
            default -> log.error("Couldn't resolve {} command", msg.getMessageType().getName());
        }
    }
}
