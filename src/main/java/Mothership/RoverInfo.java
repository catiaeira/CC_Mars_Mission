package Mothership;

import java.net.InetAddress;
import java.io.DataOutputStream;
import Message.RoverTelemetryMessage;
import Message.Message; // Import essencial

public class RoverInfo {
    private final int roverId;
    private InetAddress roverIpAddress;
    private int missionLinkUdpPort;
    private DataOutputStream tcpOut;
    private RoverTelemetryMessage lastTelemetryMessage;
    private long lastActiveTimestamp;

    // Variáveis de Controlo
    private int lastProcessedSequenceNumber = -1;
    private Message lastSentMessage = null; // A variável que faltava

    public RoverInfo (int roverId, InetAddress roverIpAddress, int missionLinkUdpPort) {
        this.roverId = roverId;
        this.roverIpAddress = roverIpAddress;
        this.missionLinkUdpPort = missionLinkUdpPort;
        this.tcpOut = null;
        this.lastTelemetryMessage = null;
        this.lastActiveTimestamp = System.currentTimeMillis();
    }

    public RoverInfo (int roverId, InetAddress roverIpAddress, int missionLinkUdpPort, DataOutputStream tcpOut) {
        this.roverId = roverId;
        this.roverIpAddress = roverIpAddress;
        this.missionLinkUdpPort = missionLinkUdpPort;
        this.tcpOut = tcpOut;
        this.lastTelemetryMessage = null;
        this.lastActiveTimestamp = System.currentTimeMillis();
    }

    // --- Getters e Setters da Cache ---
    public Message getLastSentMessage() {
        return lastSentMessage;
    }

    public void setLastSentMessage(Message msg) {
        this.lastSentMessage = msg;
    }

    // --- Getters e Setters da Sequência ---
    public int getLastProcessedSequenceNumber() {
        return lastProcessedSequenceNumber;
    }

    public void setLastProcessedSequenceNumber(int seq) {
        this.lastProcessedSequenceNumber = seq;
    }

    // --- Outros Métodos ---
    public void updateLastActiveTimestamp(long lastActiveTimestamp) {
        this.lastActiveTimestamp = lastActiveTimestamp;
    }

    public void updateLastTelemetryMessage(RoverTelemetryMessage lastTelemetryMessage) {
        this.lastTelemetryMessage = lastTelemetryMessage;
    }

    public int getRoverId() {
        return roverId;
    }
}