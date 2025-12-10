package Connection;

import Message.Message;
import Message.UpdateMission;
import Message.Package;
import Message.MessageUDP;
import Mothership.Mothership;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MissionLinkServer implements Runnable, MissionLinkGeneric {
    private int port;
    private Mothership mothership;
    private final BlockingQueue<Package> outgoingQueue = new LinkedBlockingQueue<>();

    // Variável para controlar o Sender
    private MissionLinkSender sender;

    public MissionLinkServer(int port, Mothership mothership) {
        this.port = port;
        this.mothership = mothership;
    }

    public void enqueueMessage(MessageUDP message, InetAddress roverIp, int roverPort) {
        try {
            Package p = new Package(roverIp.getHostAddress(), roverPort, message);
            outgoingQueue.put(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(port);
            System.out.println("MissionLink UDP Server running on port " + port);

            // Inicializar o Sender e guardar a referência
            this.sender = new MissionLinkSender(socket, this.outgoingQueue);

            Thread senderThread = new Thread(this.sender);
            Thread receiverThread = new Thread(new MissionLinkReceiver(socket, this, sender));

            receiverThread.start();
            senderThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processMessageContent(MessageUDP msg, DatagramPacket packet) {
        // 1. IGNORAR ACKS (Para não responder a respostas)
        if (msg.getMessageDataType() == MessageUDP.MessageDataTypes.ACK) {
            return;
        }

        // --- FIX CRÍTICO: DESENTUPIR O SENDER ---
        // Se o Rover está a pedir Missão, significa que JÁ RECEBEU o Init.
        // Temos de mandar o Sender PARAR de retransmitir o Init imediatamente.
        if (msg.getMessageDataType() == MessageUDP.MessageDataTypes.REQUEST_MISSION) {

        }
        // ----------------------------------------

        System.out.println("[ML Server] Recebido: " + msg.getMessageDataType() + " Seq=" + msg.getSequenceNumber());

        switch (msg.getMessageDataType()) {
            case ROVER_INIT:
                mothership.storeRoverInfoConnection(msg, packet.getAddress(), packet.getPort());
                break;
            case REQUEST_MISSION:
                // Se a fila estiver vazia, o generateReply da Mothership agora cria uma missão nova.
                // Aqui apenas chamamos o método para garantir que a fila não está bloqueada.
                mothership.mothershipMissions.createRandomMissionIfEmpty();
                break;
            case MISSION_UPDATE:
                UpdateMission updateMissionMsg = (UpdateMission) msg.getMessageData();
                mothership.mothershipMissions.processMissionUpdate(updateMissionMsg);
            default:
                break;
        }
    }

    @Override
    public MessageUDP generateReply(MessageUDP msg, int ackNum) {
        return mothership.generateReply(msg, ackNum);
    }
}