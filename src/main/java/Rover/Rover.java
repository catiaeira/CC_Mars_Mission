package Rover;
import Utils.Point3D;

import java.util.ArrayList;
import java.util.List;

public class Rover {
    private int id;
    private Point3D position;
    private MissionState state = MissionState.IDLE;
    private int batteryLevel = 100;
    private List<String> inventory = new ArrayList<>();
    private List <PhysicalState> physicalStates;

    public enum MissionState {
        IN_MISSION,
        IDLE,
        CHARGING,
        ERROR,
        ON_THE_WAY
    }

    public Rover (int id){
        this.id = id;
        this.position = new Point3D(0,0,0);
        this.physicalStates = new ArrayList<>();
    }

    public Rover(int id, Point3D position, List<PhysicalState> physicalStates) {
        this.id = id;
        this.position = position;
        this.physicalStates = new ArrayList<>();
        this.physicalStates.addAll(physicalStates);
    }

    public int getId() {
        return id;
    }
    public Point3D getPosition() {return position;}
    public MissionState getState() {return state;}
    public int getBatteryLevel() {return batteryLevel;}
    public List <String> getInventory() {
        return new ArrayList<>(inventory);
    }
    public List <PhysicalState> getPhysicalStates() {
        return new ArrayList<>(physicalStates);
    }

    public void setId(int id) {
        this.id = id;
    }
    public void setPosition(Point3D position) {this.position = position;}
    public void setState(MissionState state) {this.state = state;}
    public void setBatteryLevel(int batteryLevel) {this.batteryLevel = batteryLevel;}

    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        ArrayList<PhysicalState> physicalStates = new ArrayList<>();
        physicalStates.add(new PhysicalState("wheels", 100));
        physicalStates.add(new PhysicalState("camera", 80));

        Rover rover = new Rover(id, new Point3D(0,0,0), physicalStates);
        RoverConnection connection = new RoverConnection(rover);
        connection.connectServer();
        connection.sendTelemetry();
        if (rover.state == MissionState.IDLE)  {
            connection.requestMission();
        }
    }
}