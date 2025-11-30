package Mothership;

import java.net.InetAddress;
import Message.RoverTelemetryMessage;
import Message.Message;

public class RoverInfo {
    private final int roverId;
    private InetAddress roverIpAddress;
    private int missionLinkUdpPort;
    private RoverTelemetryMessage lastTelemetryMessage;
    private long lastActiveTimestamp;

    // Variáveis de Controlo
    private int lastProcessedSequenceNumber = -1;

    // --- VARIÁVEL QUE FALTAVA ---
    private Message lastSentMessage = null;

    // --- VARIÁVEL PARA CONTAR O ENVIO ---
    private int outputSequenceNumber = 0;

    public RoverInfo (int roverId, InetAddress roverIpAddress, int missionLinkUdpPort) {
        this.roverId = roverId;
        this.roverIpAddress = roverIpAddress;
        this.missionLinkUdpPort = missionLinkUdpPort;
        this.lastTelemetryMessage = null;
        this.lastActiveTimestamp = System.currentTimeMillis();
    }

    // --- MÉTODOS DE RESET (Novo) ---
    public void resetConnection(InetAddress newIp, int newPort) {
        this.roverIpAddress = newIp;
        this.missionLinkUdpPort = newPort;
        this.lastProcessedSequenceNumber = -1;
        this.outputSequenceNumber = 0;
        this.lastSentMessage = null;
        System.out.println("[RoverInfo] Connection reset for Rover " + roverId);
    }

    // --- SEQUÊNCIA DE SAÍDA (Novo) ---
    public int getAndIncrementOutputSequenceNumber() {
        return this.outputSequenceNumber++;
    }

    // --- CACHE (O que te faltava) ---
    public Message getLastSentMessage() {
        return lastSentMessage;
    }

    public void setLastSentMessage(Message msg) {
        this.lastSentMessage = msg;
    }

    // --- Getters e Setters Normais ---
    public int getLastProcessedSequenceNumber() {
        return lastProcessedSequenceNumber;
    }

    public void setLastProcessedSequenceNumber(int seq) {
        this.lastProcessedSequenceNumber = seq;
    }

    public void updateLastActiveTimestamp(long lastActiveTimestamp) {
        this.lastActiveTimestamp = lastActiveTimestamp;
    }

    public void updateLastTelemetryMessage(RoverTelemetryMessage lastTelemetryMessage) {
        this.lastTelemetryMessage = lastTelemetryMessage;
    }

    public void setRoverConnection (InetAddress ip, int port) {
        this.roverIpAddress = ip;
        this.missionLinkUdpPort = port;
    }

    public int getRoverId() {
        return roverId;
    }

    public InetAddress getRoverIpAddress(){
        return this.roverIpAddress;
    }

    public int getMissionLinkUdpPort() { // Adicionei este getter útil
        return this.missionLinkUdpPort;
    }

    public RoverTelemetryMessage getLastTelemetryMessage(){
        return this.lastTelemetryMessage;
    }

    public String toStringForAPI() {
        final int WIDTH = 80;
        String SEPARATOR_LINE = "+" + "-".repeat(WIDTH - 2) + "+";
        String rover = String.format("| Rover %d:%-" + (WIDTH - 11) + "s|", this.roverId, "");
        String ipAddress = String.format("| IP address -> %-" + (WIDTH - 18) + "s |", this.roverIpAddress);

        String status = "UNKNOWN";
        String battery = "N/A";
        if (this.lastTelemetryMessage != null) {
            status = this.lastTelemetryMessage.getMissionState().toString();
            battery = this.lastTelemetryMessage.getBatteryLevel() + "%";
        }

        String statusLine = String.format("| Status -> %-" + (WIDTH - 14) + "s |", status);
        String batteryLine = String.format("| Battery -> %-" + (WIDTH - 15) + "s |", battery);

        return SEPARATOR_LINE + "\n" +
                rover + "\n" +
                SEPARATOR_LINE + "\n" +
                ipAddress + "\n" +
                statusLine + "\n" +
                batteryLine + "\n" +
                SEPARATOR_LINE + "\n";
    }
}