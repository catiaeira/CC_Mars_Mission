package Connection;

import Message.Message;

import java.io.*;
import java.net.*;
import Mothership.Mothership;

public class TelemetryStreamServer implements Runnable {
    private int port;
    private Mothership mothership;

    public TelemetryStreamServer(int port, Mothership mothership) {
        this.port = port;
        this.mothership = mothership;
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
                    mothership.updateRoverInfoWithTelemetry(receivedMsg);
                }
            }
        } catch (IOException e) {
            System.out.println("[TS] Rover disconnected.");
        }
    }
}

