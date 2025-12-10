package Connection;

import Message.MessageUDP;
import Message.RoverInitMessage;
import Message.Package;
import Message.ACKMessage;
import Rover.Rover;
import Utils.UDPPrint;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MissionLinkClient implements Runnable, MissionLinkGeneric {
    private final String serverIP;
    private final int serverPort;
    private final BlockingQueue<Package> outgoingQueue = new LinkedBlockingQueue<>();
    private final Rover rover;
    private MissionLinkReceiver receiver;
    private MissionLinkSender sender;
    private int lastProcessedSeq = -1;

    public MissionLinkClient(String serverIP, int serverPort, Rover rover) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.rover = rover;
    }

    public void enqueueMessage(MessageUDP message) {
        try {
            Package p = new Package(serverIP, serverPort, message);
            outgoingQueue.put(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket();

            this.sender = new MissionLinkSender(socket, this.outgoingQueue);
            this.receiver = new MissionLinkReceiver(socket, this, sender);

            Thread senderThread = new Thread(this.sender);
            Thread receiverThread = new Thread(this.receiver);

            senderThread.start();
            receiverThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        this.receiver.stop();
        this.sender.stop();
        System.out.println("[MissionLinkClient] Closing!");
    }

    @Override
    public void processMessageContent(MessageUDP msg, DatagramPacket packet) {
        // --- FIX: VERIFICAÇÃO DE DUPLICADOS ---
        // Se o número de sequência for igual ou menor ao último que processámos,
        // é uma retransmissão. Ignoramos a lógica, MAS deixamos o método acabar
        // para que o Receiver envie o ACK novamente (linha 94 do Receiver).
        if (lastProcessedSeq != -1 && msg.getSequenceNumber() <= lastProcessedSeq) {
            UDPPrint.log("ML", msg, "Duplicado detetado (Seq " + msg.getSequenceNumber() + "). Reenviando ACK e ignorando conteúdo.", false);

            // 1. OBRIGATÓRIO: Gerar e Enviar o ACK novamente!
            // Se não fizermos isto, o Servidor nunca vai saber que já recebemos.
            MessageUDP ackMsg = new MessageUDP(
                    0, // Seq do ACK não interessa muito aqui
                    0,
                    0, 0, 1,
                    MessageUDP.MessageDataTypes.ACK,
                    new ACKMessage(msg.getSequenceNumber()) // Confirma o ID que acabámos de receber
            );

            // Envia para o IP/Porto de onde veio a mensagem duplicada
            this.sender.sendMessage(ackMsg, packet.getAddress().getHostAddress(), packet.getPort());

            // 2. Agora sim, podemos sair
            return;
        }
        this.lastProcessedSeq = msg.getSequenceNumber();
        // --------------------------------------

        System.out.println("[ML] Received: " + msg.toString());
        switch (msg.getMessageDataType()) {
            case ROVER_INIT:
                RoverInitMessage message = (RoverInitMessage) msg.getMessageData();
                UDPPrint.logSuccess("RCV", msg, "ID Atribuído: " + message.getId());

                break;

            case MISSION:
                UDPPrint.logSuccess("RCV", msg, "NOVA MISSÃO ACEITE E GUARDADA!");

                break;

            default:
                // Outras mensagens (como ACKs puros) ficam em log normal ou silêncio
                // WiresharkLogger.log("RCV", msg, "ACK/Outro recebido", false);
                break;
        }
        rover.processMessage(msg.getMessageDataType(), msg.getMessageData());
    }

    @Override
    public MessageUDP generateReply(MessageUDP msg, int ackNum) {
        return this.rover.generateReply(msg, ackNum);
    }
}