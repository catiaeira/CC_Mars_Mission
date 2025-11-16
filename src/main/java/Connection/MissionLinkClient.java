package Connection;

import Message.Message;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MissionLinkClient implements Runnable {
    private final String serverIP;
    private final int serverPort;
    private final BlockingQueue<Message> outgoingQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public MissionLinkClient(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void enqueueMessage(Message message) {
        try {
            outgoingQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddr = InetAddress.getByName(serverIP);

            while (running) {
                try {
                    Message messageToSend = outgoingQueue.take();

                    byte[] msg = messageToSend.convertMessageToBytes();
                    DatagramPacket requestPacket = new DatagramPacket(msg, msg.length, serverAddr, serverPort);

                    socket.send(requestPacket);
                    System.out.println("[ML] Message sent to Mothership.");

                    // receive reply
                    byte[] buffer = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(responsePacket);

                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    System.out.println("[ML] Received from Mothership: " + response);

                } catch (InterruptedException e) {
                    System.out.println("[ML] Connection thread interrupted.");
                    Thread.currentThread().interrupt();
                    running = false;
                } catch (IOException e) {
                    if (running) {
                        System.out.println("[ML] IO Error: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("[ML] Client connection terminated.");
        }
    }
}