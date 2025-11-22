package Rover;

import Utils.Point3D;

import Message.*;
import Message.Message.MessageDataTypes;


import java.util.ArrayList;
import java.util.List;

public class Rover {
    private int id;
    private Point3D position;
    private MissionState state = MissionState.IDLE;
    private double batteryLevel = 100;
    private Base base;
    private List<String> inventory = new ArrayList<>();
    private final int maxInventorySpace;
    private List <PhysicalState> physicalStates;
    private RoverMissions roverMissions;

    public enum MissionState {
        IN_MISSION,
        IDLE,
        CHARGING,
        ERROR,
        ON_THE_WAY
    }

    public Rover(Point3D position, List<PhysicalState> physicalStates, int inventorySpace) {
        this.id = -1;
        this.position = position;
        this.base = new Base(position);
        this.physicalStates = new ArrayList<>();
        this.physicalStates.addAll(physicalStates);
        this.roverMissions = new RoverMissions(this);
        this.maxInventorySpace = inventorySpace;
    }

    public int getId() {
        return id;
    }
    public Point3D getPosition() {return position;}
    public MissionState getState() {return state;}
    public double getBatteryLevel() {return batteryLevel;}
    public Base getBase() {return base;}
    public List <String> getInventory() {
        return new ArrayList<>(inventory);
    }
    public List <PhysicalState> getPhysicalStates() {
        return new ArrayList<>(physicalStates);
    }
    public int getMaxInventorySpace() {
        return this.maxInventorySpace;
    }

    public void setId(int id) {
        this.id = id;
    }
    public void setPosition(Point3D position) {this.position = position;}
    public void setState(MissionState state) {this.state = state;}
    public void setBatteryLevel(double batteryLevel) {this.batteryLevel = batteryLevel;}
    public void addToInventory(String item) {
        if (maxInventorySpace > inventory.size()) inventory.add(item);
    }
    public void clearInventory () {this.inventory.clear();}

    public static void main(String[] args) throws InterruptedException {
        ArrayList<PhysicalState> physicalStates = new ArrayList<>();
        physicalStates.add(new PhysicalState("wheels", 100));
        physicalStates.add(new PhysicalState("camera", 80));

        Rover rover = new Rover( new Point3D(0,0,0), physicalStates, 5);
        RoverConnection connection = new RoverConnection(rover);
        connection.connectServer();
        connection.sendInit();
        Thread.sleep(1000); // TEMPORARY

        rover.roverMissions.run();
        connection.sendTelemetry();
        if (rover.state == MissionState.IDLE)  {
            connection.requestMission();
        }
    }

    public void processMessage(MessageDataTypes type, MessageData msg) {
        switch (type) {
            case ROVER_INIT:
                RoverInitMessage roverMsg = (RoverInitMessage) msg;
                setId(roverMsg.id);
                break;
            case MISSION:
                System.out.println("received mission");
                MissionMessage missionMsg = (MissionMessage) msg;
                this.roverMissions.addMission(missionMsg.getMission());
                break;
            default:
                break;
        }
    }

    public Message generateReply(Message receivedMsg) {
        Message reply = null;

        switch (receivedMsg.getMessageDataType()) {
            case MISSION:
                // reply = ....
                break;
            case ACK:
                return null; // ACKs dont need any reply
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
}