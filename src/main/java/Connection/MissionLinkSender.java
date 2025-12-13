package Connection;

import Message.MessageUDP;
import Message.Package;
import Utils.UDPPrint;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MissionLinkSender implements Runnable {
    private final DatagramSocket socket;
    private final BlockingQueue<Package> outgoingQueue;
    private final ScheduledExecutorService retransmissionScheduler; // New scheduler

    // shared control variables
    private static final int MAX_TIMEOUTS = 5;
    private static final int TIMEOUT_MS = 5000;
    private static final int RETRANSMISSION_CHECK_INTERVAL_MS = 1000; // check pending ACKs every second

    private final ConcurrentHashMap<String, MessageUDP> lastSentReply = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PendingMessage> pendingAcks = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private Thread sendingThread = null;

    private static class PendingMessage {
        final MessageUDP message;
        final String ip;
        final int port;
        final int expectedAckNumber;
        final List<MessageUDP> fragments;
        volatile AtomicLong lastSentTime = new AtomicLong(System.currentTimeMillis());
        volatile int attempts = 0;
        volatile boolean ackReceived = false;

        PendingMessage(MessageUDP msg, String ip, int port, int expectedAck) {
            this.message = msg;
            this.ip = ip;
            this.port = port;
            this.expectedAckNumber = expectedAck;
            this.fragments = FragManager.fragmentMessage(msg);
        }
    }

    public MissionLinkSender(DatagramSocket socket, BlockingQueue<Package> outgoingQueue) {
        this.socket = socket;
        this.outgoingQueue = outgoingQueue;

        // initialize the scheduler for retransmissions
        this.retransmissionScheduler = Executors.newSingleThreadScheduledExecutor();
        this.retransmissionScheduler.scheduleAtFixedRate(
                this::retransmissionTask,
                RETRANSMISSION_CHECK_INTERVAL_MS,
                RETRANSMISSION_CHECK_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }
    public void sendMessage(MessageUDP msg, String ip, int port) {
        try {
            Package pck = new Package(ip, port, msg);
            outgoingQueue.put(pck);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void confirmAck(int ackNumber, String address) {
        PendingMessage p = pendingAcks.get(address);
        if (p == null) return;

        synchronized (p) {
            if (ackNumber >= p.expectedAckNumber) {
                p.ackReceived = true;
                System.out.println("RECEIVED ACK : " + ackNumber + " AND WAS WAITING FOR " + p.expectedAckNumber);
            }
        }
    }

    public void stop() {
        running = false;
        this.sendingThread.interrupt();
        retransmissionScheduler.shutdownNow();
    }

    @Override
    public void run() {
        this.sendingThread = Thread.currentThread();
        while (running) {
            try {
                Package pkg = outgoingQueue.take();
                MessageUDP msg = pkg.getMessage();

                InetAddress ipAddress = InetAddress.getByName(pkg.getToIp());
                int port = pkg.getToPort();

                // doesn't wait for confirmation if it's a pure ACK
                if (msg.getMessageDataType() == MessageUDP.MessageDataTypes.ACK) {
                    byte[] data = msg.convertMessageToBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
                    socket.send(packet);
                    continue;
                }

                List<MessageUDP> fragments = FragManager.fragmentMessage(msg);
                for (MessageUDP frag : fragments) {
                    byte[] data = frag.convertMessageToBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
                    socket.send(packet);
                }

                int payloadSize = (msg.getMessageData() != null) ? msg.getMessageData().convertMessageDataToBytes().length : 0;
                int expectedAck = msg.getSequenceNumber() + (payloadSize > 0 ? payloadSize : 1);

                PendingMessage pMsg = new PendingMessage(msg, pkg.getToIp(), port, expectedAck);

                String pendingKey = ipAddress.getHostAddress() + ":" + port;
                pendingAcks.put(pendingKey, pMsg);

                // logging
                String fragInfo = (fragments.size() > 1) ? " (" + fragments.size() + " fragments)" : "";
                UDPPrint.log("SND", msg, "To: " + pkg.getToIp() + fragInfo + " (Waiting ACK " + expectedAck + ")", false);
                System.out.println("[ML SENDER] SENDING: " + msg);

            } catch (IOException | InterruptedException e) {
                if (running) {
                    System.out.println("[ML SENDER] Connection closed or lost.");
                    e.printStackTrace();
                }
                running = false;
            }
        }
        System.out.println("Closing ML sender!");
    }

    private void retransmissionTask() {
        long currentTime = System.currentTimeMillis();

        pendingAcks.entrySet().removeIf(entry -> {
            PendingMessage p = entry.getValue();
            if (p.ackReceived) return true; // remove the ack

            if (currentTime - p.lastSentTime.get() >= TIMEOUT_MS) {
                p.attempts++;
                if (p.attempts > MAX_TIMEOUTS) {
                    UDPPrint.log("SND", p.message, "Max number of retransmissions hit, dropping message...", true);
                    return true; // drop message
                }
                try { // retransmitting
                    InetAddress ipAddress = InetAddress.getByName(p.ip);
                    int port = p.port;

                    for (MessageUDP frag : p.fragments) {
                        byte[] data = frag.convertMessageToBytes();
                        DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
                        socket.send(packet);
                    }

                    p.lastSentTime.set(currentTime);
                    String fragInfo = (p.fragments.size() > 1) ? " (" + p.fragments.size() + " fragments)" : "";
                    UDPPrint.log("SND", p.message, "Retransmission #" + p.attempts + fragInfo + " -> " + p.ip, true);

                } catch (IOException e) {
                    System.err.println("[ML SENDER] Error during retransmission: " + e.getMessage());
                    return true;
                }
            }
            return false;
        });
    }

    public MessageUDP getLastSentReply (String address) {
        return this.lastSentReply.get(address);
    }
    public void setLastSentReply (String address, MessageUDP reply) {
        this.lastSentReply.put(address, reply);
    }
}