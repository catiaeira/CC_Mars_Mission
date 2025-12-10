package Connection;

import Message.Message;

import Message.MessageUDP;
import Utils.UDPPrint;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

import java.util.concurrent.ConcurrentHashMap;

public class MissionLinkReceiver implements Runnable {
    private final DatagramSocket socket;
    private final MissionLinkGeneric ML;
    private boolean running = true;
    private final MissionLinkSender sender;
    // Buffer de Remontagem
    private final Map<Integer, List<MessageUDP>> reassemblyBuffer = new ConcurrentHashMap<>();

    public MissionLinkReceiver(DatagramSocket socket,  MissionLinkGeneric ML, MissionLinkSender sender) {
        this.socket = socket;
        this.ML = ML;
        this.sender = sender;
    }
    public void stop() {running = false;}

    @Override
    public void run() {
        // Buffer seguro para receber pacotes (fragmentos de 1500 + headers)
        byte[] buffer = new byte[2048];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String address = packet.getAddress().getHostAddress() + ":" + packet.getPort();

                // 1. Limpar buffer (ler apenas os bytes recebidos)
                byte[] msgBytes = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, msgBytes, 0, packet.getLength());

                // 2. Converter
                MessageUDP receivedMsg = MessageUDP.convertBytesToMessageUDP(msgBytes);


                // --- AQUI ESTÁ A CORREÇÃO CRÍTICA ---
                // Se for um pacote EXCLUSIVO de ACK, validamos e ignoramos o resto.
                if (receivedMsg.getMessageDataType() == Message.MessageDataTypes.ACK) {

                    // O número validado costuma vir no Header (AckNumber) ou no Payload
                    int ackToValidate = receivedMsg.getAckNumber();

                    // Fallback: Se o header for -1, tentamos ler do payload (ACKMessage)
                    if (ackToValidate == -1 && receivedMsg.getMessageData() != null) {
                        // Assumindo que tens cast para ACKMessage
                        // ackToValidate = ((ACKMessage) receivedMsg.getMessageData()).getConfirmingSequenceNumber();
                        // Se não tiveres acesso fácil, usa o SequenceNumber da mensagem como fallback
                        ackToValidate = receivedMsg.getSequenceNumber();
                    }

                    if (this.sender != null && ackToValidate != -1) {
                        System.out.println("[Receiver] ACK recebido (" + ackToValidate + ").");
                        this.sender.confirmAck(ackToValidate, address);
                    }

                    // O PULO DO GATO: Volta ao início do loop. Não processa mais nada.
                    continue;
                }


                // 3. Confirmar ACKs ao Sender (se a mensagem trouxer um ACK para nós)
                if (receivedMsg.getAckNumber() != -1 && this.sender != null) {
                    this.sender.confirmAck(receivedMsg.getAckNumber(), address);
                }

                MessageUDP finalMessage = null;

                // --- LÓGICA DE RECONSTRUÇÃO ---
                if (!receivedMsg.isFragmented()) {
                    finalMessage = receivedMsg;
                } else {
                    int fragID = receivedMsg.getFragmentID();

                    reassemblyBuffer.putIfAbsent(fragID, new ArrayList<>());
                    List<MessageUDP> parts = reassemblyBuffer.get(fragID);

                    // --- CORREÇÃO AQUI: Verificar ANTES de adicionar ---
                    boolean isDuplicate = false;
                    synchronized (parts) { // Sincronizar acesso à lista
                        for (MessageUDP existing : parts) {
                            if (existing.getFragmentIndex() == receivedMsg.getFragmentIndex()) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        if (!isDuplicate) {
                            parts.add(receivedMsg); // Adicionar só se não existir

                            // Verificar se já temos todas as peças necessárias
                            if (parts.size() == receivedMsg.getTotalFragments()) {
                                finalMessage = FragManager.reassembleMessage(parts);
                                reassemblyBuffer.remove(fragID);
                            }
                        }
                    }
                    // ---------------------------------------------------
                }

                // --- PROCESSAMENTO E RESPOSTA ---
                if (finalMessage != null) {
                    // Entregar à lógica de negócio
                    ML.processMessageContent(finalMessage, packet);

                    // Calcular número de ACK esperado (Seq + Tamanho)
                    int payloadSize = 0;
                    if (finalMessage.getMessageData() != null) {
                        payloadSize = finalMessage.getMessageData().convertMessageDataToBytes().length;
                    }
                    int ackNum = finalMessage.getSequenceNumber() + (payloadSize > 0 ? payloadSize : 1);

                    // Gerar Resposta (Geralmente um ACK ou uma Missão)
                    MessageUDP reply = (MessageUDP) ML.generateReply(finalMessage, ackNum);

                    if (reply != null && sender != null) {
                        // Se não for um ACK simples, guardamos para retransmissões futuras (Lógica Server)
                        if (reply.getMessageDataType() != Message.MessageDataTypes.ACK) {
                            sender.setLastSentReply(address, reply);
                        }

                        // ENVIAR USANDO O SENDER
                        // O Sender trata da fragmentação e coloca na fila de saída
                        sender.sendMessage(reply, packet.getAddress().getHostAddress(), packet.getPort());
                    }
                }

            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }
        System.out.println("Closing ML receiver!");
    }
}