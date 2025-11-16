package Mothership;

import Message.Message;

import java.util.HashMap;
import Message.RoverTelemetryMessage;

public class Mothership { // controller
    HashMap<Integer, RoverInfo> rovers = new HashMap<>();

    public void updateRoverInfoWithTelemetry (Message msg) {
        if (msg.getMessageDataType()!= Message.MessageDataTypes.ROVER_TELEMETRY) {
            System.out.println("Message that isn't telemetry received in TS");
            return; // should never happen
        }
        RoverTelemetryMessage telemetry = (RoverTelemetryMessage) msg.getMessageData();
        RoverInfo roverInfo = this.rovers.get(telemetry.id);
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
}