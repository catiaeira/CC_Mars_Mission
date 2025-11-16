package Mothership;

import Rover.Rover;

import java.io.DataOutputStream;
import java.net.InetAddress;
import Message.RoverTelemetryMessage;

public class RoverInfo {
    private final int roverId;
    private InetAddress roverIpAddress;
    private int missionLinkUdpPort;
    private DataOutputStream tcpOut;
    private RoverTelemetryMessage lastTelemetryMessage;
    private long lastActiveTimestamp;

    public RoverInfo (int roverId,  InetAddress roverIpAddress, int missionLinkUdpPort,  DataOutputStream tcpOut) {
        this.roverId = roverId;
        this.roverIpAddress = roverIpAddress;
        this.missionLinkUdpPort = missionLinkUdpPort;
        this.tcpOut = tcpOut;
        this.lastTelemetryMessage = null;
        this.lastActiveTimestamp = System.currentTimeMillis();
    }
    public void updateLastActiveTimestamp(long lastActiveTimestamp) {
        this.lastActiveTimestamp = lastActiveTimestamp;
    }
    public void updateLastTelemetryMessage(RoverTelemetryMessage lastTelemetryMessage) {
        this.lastTelemetryMessage = lastTelemetryMessage;
    }
}
