package Connection;

import Message.Message;

import java.io.*;
import java.net.*;

public class TelemetryStreamServer implements Runnable {
    private int port;

    public TelemetryStreamServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Telemetry TCP Server running on port " + port);

            while (true) {
                Socket roverSocket = serverSocket.accept();
                new Thread(() -> handleRover(roverSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRover(Socket socket) {
        try(DataInputStream in = new DataInputStream(socket.getInputStream())) {
            while (true) {
                int length = in.readInt();
                if (length > 0) {
                    byte[] messageBytes = new byte[length];

                    in.readFully(messageBytes);

                    Message receivedMsg = Message.convertBytesToMessage(messageBytes);
                    System.out.println("[TS] Received telemetry message: " + receivedMsg.toString());
                }
            }
        } catch (IOException e) {
            System.out.println("[TS] Rover disconnected.");
        }
    }
}

