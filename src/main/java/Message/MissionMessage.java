package Message;

import Mission.Mission;
import Utils.Point3D;

public class MissionMessage implements MessageData{
    private final int missionId;
    private final int roverId;
    private final Mission.MissionType missionType;
    private final Point3D areaCoordinates;
    private final int areaRadius;
    private final int missionTime;
    private final int updateTime;
    private final boolean isUrgent;
    private final boolean isCompleted;

    public MissionMessage(int missionId, int roverId, Mission.MissionType missionType,
                          Point3D areaCoordinates, int areaRadius, int missionTime,
                          int updateTime, boolean isUrgent, boolean isCompleted) {
        this.missionId = missionId;
        this.roverId = roverId;
        this.missionType = missionType;
        this.areaCoordinates = areaCoordinates;
        this.areaRadius = areaRadius;
        this.missionTime = missionTime;
        this.updateTime = updateTime;
        this.isUrgent =  isUrgent;
        this.isCompleted = isCompleted;
    }
    public MissionMessage (Mission mission) {
        this.missionId = mission.getMissionId();
        this.roverId = mission.getRoverId();
        this.missionType = mission.getMissionType();
        this.areaCoordinates= mission.getAreaCoordinates();
        this.areaRadius= mission.getAreaRadius();
        this.missionTime = mission.getMissionTime();
        this.updateTime = mission.getUpdateTime();
        this.isUrgent = mission.isUrgent();
        this.isCompleted = mission.isCompleted();
    }

    public Mission getMission() {
        return new Mission (missionId,roverId, missionType, areaCoordinates, areaRadius, missionTime, updateTime, isUrgent, isCompleted);
    }
    public int getMissionId() {
        return missionId;
    }
    public int  getRoverId() {
        return roverId;
    }
    public Mission.MissionType getMissionType() {
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

    @Override
    public byte[] convertMessageDataToBytes() {
        int num_vars = 11;
        byte[] bytes  = new byte[num_vars+1];
        bytes[0] = (byte) num_vars;
        bytes[1] = (byte) missionId;
        bytes[2] = (byte) roverId;
        bytes[3] = (byte) this.missionType.ordinal();
        bytes[4] = (byte) this.areaCoordinates.x;
        bytes[5] = (byte) this.areaCoordinates.y;
        bytes[6] = (byte) this.areaCoordinates.z;
        bytes[7] = (byte) this.areaRadius;
        bytes[8] = (byte) this.missionTime;
        bytes[9] = (byte) this.updateTime;
        bytes[10] = (byte) (this.isUrgent ? 1 : 0);
        bytes[11] = (byte) (this.isCompleted ? 1 : 0);

        return bytes;
    }

    public static MissionMessage convertBytesToMessageData(byte[] bytes) {
        int missionId = bytes[1];
        int roverId = bytes[2];
        Mission.MissionType missionType = Mission.MissionType.values()[bytes[3]];
        int x = bytes[4];
        int y = bytes[5];
        int z = bytes[6];
        Point3D coordinates = new Point3D(x,y,z);
        int areaRadius = bytes[7];
        int missionTime = bytes[8];
        int updateTime = bytes[9];
        boolean isUrgent = bytes[10] == 1;
        boolean isCompleted = bytes[11] == 1;

        return new MissionMessage(missionId, roverId, missionType, coordinates, areaRadius, missionTime, updateTime, isUrgent, isCompleted);
    }

    @Override
    public String toString() {
        return "MissionMessage { " +
                "missionId = " + missionId +
                ", roverId = " + roverId +
                ", missionType = " + missionType +
                ", areaCoordinates = " + areaCoordinates +
                ", areaRadius = " + areaRadius +
                ", missionTime = " + missionTime +
                ", updateTime = " + updateTime +
                ", isUrgent = " + isUrgent +
                ", isCompleted = " + isCompleted +
                '}';
    }
}
