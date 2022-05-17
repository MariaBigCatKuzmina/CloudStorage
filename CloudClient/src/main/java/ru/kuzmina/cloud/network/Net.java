package ru.kuzmina.cloud.network;

import java.io.*;
import java.net.Socket;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import lombok.extern.slf4j.Slf4j;
import ru.kuzmina.cloud.model.AbstractMessage;

@Slf4j
public class Net {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8189;

    private static Net INSTANCE;

    private final Socket socket;
    private final ObjectDecoderInputStream inputStream;
    private final ObjectEncoderOutputStream outputStream;

    private Net(String host, int port) throws IOException {
        socket = new Socket(host, port);
        outputStream = new ObjectEncoderOutputStream(socket.getOutputStream());
        inputStream = new ObjectDecoderInputStream(socket.getInputStream());
    }

    public static Net getINSTANCE() throws IOException {
        if (INSTANCE == null) {
            INSTANCE = new Net(SERVER_HOST, SERVER_PORT);
        }
        return INSTANCE;
    }

    public AbstractMessage read() throws IOException, ClassNotFoundException {
        return (AbstractMessage) inputStream.readObject();
    }

    public void write(AbstractMessage msg) throws IOException {
        outputStream.writeObject(msg);
    }

    public void closeConnection() {
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            log.error("Error on close connection");
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return socket.isClosed();
    }
}
