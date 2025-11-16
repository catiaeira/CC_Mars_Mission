package Connection;

import Message.Message;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MissionLinkServer implements Runnable { //UDP
    private int port;

    public MissionLinkServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("MissionLink UDP Server running on port " + port);
            byte[] buffer = new byte[10];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] msg = packet.getData();
                Message message = Message.convertBytesToMessage(msg);
                System.out.println("[ML] Received: " + message.toString()); //kinda wacky here but it's gonna change anyway so idrc rn

                // for response
                String ack = "ACK: " + message.toString();
                byte[] ackBytes = ack.getBytes();
                DatagramPacket response = new DatagramPacket(
                        ackBytes, ackBytes.length, packet.getAddress(), packet.getPort());
                socket.send(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
