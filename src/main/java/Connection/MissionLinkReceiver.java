package Connection;

import Message.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class MissionLinkReceiver implements Runnable {
    private final DatagramSocket socket;
    private final MissionLinkGeneric ML;
    private boolean running = true;

    public MissionLinkReceiver(DatagramSocket socket,  MissionLinkGeneric ML) {
        this.socket = socket;
        this.ML = ML;
    }
    public void stop() {running = false;}

    @Override
    public void run() {
        byte[] buffer = new byte[1500];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // blocks here until a new packet is received

                byte[] msg = packet.getData();
                Message receivedMsg = Message.convertBytesToMessage(msg);

                ML.processMessageContent(receivedMsg, packet);

                Message reply = ML.generateReply(receivedMsg);
                if (reply != null) ML.sendResponse(socket, packet, reply);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Closing ML receiver!");
    }
}