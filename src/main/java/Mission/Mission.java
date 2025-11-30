package Mission;

import Utils.Point3D;

import java.util.concurrent.locks.ReentrantLock;

public class Mission implements Comparable<Mission> {
    private int missionId;
    private int roverId;
    private final MissionType missionType;
    private final Point3D areaCoordinates;
    private final int areaRadius;
    private final int missionTime;
    private final int updateTime;
    private final boolean isUrgent;
    private boolean isCompleted = false;
    private final ReentrantLock lock = new ReentrantLock();

    // updates: the mission must define how and how often the rover reports back to the mothership

    public enum MissionType {
        EXPLORE,
        COLLECT_ROCKS,
        TEST_ATMOSPHERE,
    }

    private static int counter = 1;

    public Mission(int roverId, MissionType missionType, Point3D areaCoordinates, int areaRadius, int missionTime, int updateTime, boolean isUrgent) {
        lock.lock();
        this.missionId = counter;
        counter++;
        lock.unlock();
        this.roverId = roverId;
        this.missionType = missionType;
        this.areaCoordinates = areaCoordinates;
        this.areaRadius = areaRadius;
        this.missionTime = missionTime;
        this.updateTime = updateTime;
        this.isUrgent = isUrgent;
    }


    public Mission(int missionId, int roverId, MissionType missionType, Point3D areaCoordinates, int areaRadius, int missionTime, int updateTime, boolean isUrgent, boolean isCompleted) {
        this.missionId = missionId;
        this.roverId = roverId;
        this.missionType = missionType;
        this.areaCoordinates = areaCoordinates;
        this.areaRadius = areaRadius;
        this.missionTime = missionTime;
        this.updateTime = updateTime;
        this.isUrgent = isUrgent;
        this.isCompleted = isCompleted;
    }





    public int getMissionId() {
        return missionId;
    }
    public int getRoverId() {
        return roverId;
    }
    public MissionType getMissionType() {
        return missionType;
    }
    public Point3D getAreaCoordinates() {
        return areaCoordinates;
    }
    public int getAreaRadius() {
        return areaRadius;
    }
    public int getMissionTime() {
        return missionTime;
    }
    public int getUpdateTime() {
        return updateTime;
    }
    public boolean isUrgent() {
        return isUrgent;
    }
    public boolean isCompleted() {
        return this.isCompleted;
    }

    public void setCompleted() {
        this.isCompleted = true;
    }
    public void setRoverId(int roverId) {this.roverId = roverId;}
    @Override
    public int compareTo(Mission mission) {
        if (this.isUrgent == mission.isUrgent) return 0;
        if (this.isUrgent) return -1;
        return 1;
    }
}
