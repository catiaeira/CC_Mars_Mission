package Mothership;

import Message.Message;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import Message.*;
import Message.MissionMessage;
import Mission.Mission;
import Utils.Point3D;

public class Mothership { // controller
    List<RoverInfo> rovers = new ArrayList<RoverInfo>();
    // queue with missions?

    public void updateRoverInfoWithTelemetry (Message msg) {
        if (msg.getMessageDataType()!= Message.MessageDataTypes.ROVER_TELEMETRY) {
            System.out.println("Message that isn't telemetry received in TS");
            return; // should never happen
        }
        RoverTelemetryMessage telemetry = (RoverTelemetryMessage) msg.getMessageData();
        RoverInfo roverInfo = this.rovers.get(telemetry.id-1);
        if (roverInfo == null) {
            System.out.println("[TS] No RoverInfo found with id " + telemetry.id);
            return;  // shouldn't happen?
        }
        roverInfo.updateLastTelemetryMessage(telemetry);
        roverInfo.updateLastActiveTimestamp(System.currentTimeMillis());
    }

    public static void main(String[] args) {
        Mothership mothership = new Mothership();
        MothershipConnection connection = new MothershipConnection(mothership);
        connection.startServer();
    }

    public Message generateReply(Message receivedMsg) {
        int replyId = receivedMsg.getMessageId();
        Message reply = null;

        switch (receivedMsg.getMessageDataType()) {
            case ROVER_INIT:
                int newID = rovers.size();
                reply = new Message(
                        receivedMsg.getSequenceNumber()+1,
                        receivedMsg.getMessageId(),
                        Message.MessageDataTypes.ROVER_INIT,
                        new RoverInitMessage(newID)
                );
                break;
            case REQUEST_MISSION:
                System.out.println("sending mission after request");
                reply = new Message(receivedMsg.getSequenceNumber()+1,
                        receivedMsg.getMessageId(),
                        Message.MessageDataTypes.MISSION,
                        new MissionMessage(1, 1, Mission.MissionType.COLLECT_ROCKS,
                                new Point3D(30,10,5), 10, 60, 20,
                                false, false));
                break;
            default:
                break;
        }

        if (reply == null) { // no reply needed, send an ACK only
            reply = new Message(receivedMsg.getSequenceNumber()+1,
                    receivedMsg.getMessageId(),
                    Message.MessageDataTypes.ACK,
                    new ACKMessage(receivedMsg.getSequenceNumber())
            );
        }
        return reply;
    }

    public void assignRoverID (Message msg, InetAddress address, int port) {
        int nextId = rovers.size()+1;
        RoverInfo roverInfo = new RoverInfo(nextId, address, port);
        rovers.add(roverInfo);
    }
}