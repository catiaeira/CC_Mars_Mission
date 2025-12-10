package Connection;

import Message.MessageUDP;
import Message.Package;
import Utils.UDPPrint;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class MissionLinkSender implements Runnable {
    private final DatagramSocket socket;
    private final BlockingQueue<Package> outgoingQueue;
    private volatile boolean running = true;

    // Mapa Thread-Safe para gerir ACKs
    private final Map<String, PendingAck> pendingAcks = new ConcurrentHashMap<>();

    // Cache
    private final ConcurrentHashMap<String, MessageUDP> lastSentReply = new ConcurrentHashMap<>();

    private static final int MAX_TIMEOUTS = 10;
    private static final int TIMEOUT_MS = 4000;

    public MissionLinkSender(DatagramSocket socket, BlockingQueue<Package> outgoingQueue) {
        this.socket = socket;
        this.outgoingQueue = outgoingQueue;
    }

    public void sendMessage(MessageUDP msg, String ip, int port) {
        try {
            Package pck = new Package(ip, port, msg);
            outgoingQueue.put(pck);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        System.out.println("MissionLink Sender started (Multi-threaded).");
        while (running) {
            try {
                Package pkg = outgoingQueue.take();
                // Lança thread independente
                new Thread(() -> sendWithRetries(pkg)).start();
            } catch (InterruptedException e) {
                if (running) System.out.println("[ML SENDER] Interrupted.");
            }
        }
    }

    private void sendWithRetries(Package pkg) {
        String pendingKey = null;
        try {
            MessageUDP msg = (MessageUDP) pkg.getMessage();
            InetAddress ipAddress = InetAddress.getByName(pkg.getToIp());

            // --- FIX 1: LIMPEZA DE IP NA CRIAÇÃO ---
            // Removemos a barra para garantir que a chave fica limpa (ex: "10.0.9.20")
            String cleanIp = ipAddress.getHostAddress().replace("/", "").trim();
            // ---------------------------------------

            int port = pkg.getToPort();

            if (msg.getMessageDataType() == MessageUDP.MessageDataTypes.ACK) {
                byte[] data = msg.convertMessageToBytes();
                socket.send(new DatagramPacket(data, data.length, ipAddress, port));
                return;
            }

            int payloadSize = 0;
            try {
                if (msg.getMessageData() != null) {
                    payloadSize = msg.getMessageData().convertMessageDataToBytes().length;
                }
            } catch (Exception e) { e.printStackTrace(); }
            int expectedAck = msg.getSequenceNumber() + (payloadSize > 0 ? payloadSize : 1);

            // --- FIX 2: ZOMBI KILLER (Atomocidade) ---
            pendingKey = cleanIp + ":" + port + ":" + expectedAck;
            PendingAck pAck = new PendingAck(expectedAck);

            // Usamos APENAS putIfAbsent. É mais seguro e atómico.
            // Se retornar algo, significa que já existe uma thread -> SAIR.
            if (pendingAcks.putIfAbsent(pendingKey, pAck) != null) {
                // System.out.println("[Sender] Thread redundante evitada para: " + pendingKey);
                return;
            }
            // -----------------------------------------

            List<MessageUDP> fragments = FragManager.fragmentMessage(msg);
            boolean sentSuccessfully = false;
            int attempts = 0;

            while (!sentSuccessfully) {
                // Verifica antes de enviar
                synchronized (pAck.lock) {
                    if (pAck.ackReceived) {
                        sentSuccessfully = true;
                        break;
                    }
                }

                attempts++;
                for (MessageUDP frag : fragments) {
                    byte[] data = frag.convertMessageToBytes();
                    socket.send(new DatagramPacket(data, data.length, ipAddress, port));
                    if (fragments.size() > 1) Thread.sleep(2);
                }

                if (attempts > 1) {
                    UDPPrint.log("SND", msg, "Retransmission #" + attempts + " -> " + cleanIp, true);
                } else {
                    UDPPrint.log("SND", msg, "To: " + cleanIp + " (Waiting ACK " + expectedAck + ")", false);
                }

                synchronized (pAck.lock) {
                    if (!pAck.ackReceived) {
                        pAck.lock.wait(TIMEOUT_MS);
                    }
                    if (pAck.ackReceived) sentSuccessfully = true;
                    else if (attempts >= MAX_TIMEOUTS) sentSuccessfully = true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pendingKey != null) {
                pendingAcks.remove(pendingKey);
            }
        }
    }

    public void confirmAck(int ackNumber, String address) {
        // --- FIX 3: LIMPEZA DE IP NA VALIDAÇÃO ---
        String rawIp = address.split(":")[0];
        // Remove a barra "/" que vem do Receiver
        String remoteIp = rawIp.replace("/", "").trim();

        // Debug Opcional (se quiseres ver a funcionar)
        // System.out.println("Validando ACK " + ackNumber + " de " + remoteIp);

        for (Map.Entry<String, PendingAck> entry : pendingAcks.entrySet()) {
            String key = entry.getKey();

            PendingAck p = entry.getValue();
            boolean ipMatch = key.startsWith(remoteIp);

            if (!ipMatch && ackNumber >= p.waitingForAckNumber && !p.ackReceived) {
                System.out.println("[Sender] AVISO: IP Mismatch (" + remoteIp + " vs " + key + ") mas ACK bate certo. Aceitando.");
                ipMatch = true; // Forçamos a aceitação se o número for forte
            }

            if (ipMatch) {
                synchronized (p.lock) {
                    if (!p.ackReceived && ackNumber >= p.waitingForAckNumber) {
                        System.out.println("-> SUCESSO! Thread desbloqueada.");
                        p.ackReceived = true;
                        p.lock.notifyAll();

                    }
                }
            }
        }
    }

    public MessageUDP getLastSentReply (String address) { return this.lastSentReply.get(address); }
    public void setLastSentReply (String address, MessageUDP reply) { this.lastSentReply.put(address, reply); }
    public void stop() { running = false; }

    private static class PendingAck {
        int waitingForAckNumber;
        boolean ackReceived = false;
        final Object lock = new Object();
        public PendingAck(int expected) { this.waitingForAckNumber = expected; }
    }
}