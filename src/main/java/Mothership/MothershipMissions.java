package Mothership;

import Mission.Mission;
import Utils.Point3D;

import java.util.PriorityQueue;
import java.util.Random;

public class MothershipMissions {
    private final PriorityQueue<Mission> missionsToDo = new PriorityQueue<>();

    public Mission getMission () {
        return missionsToDo.poll();
    }
    public void createRandomMissionIfEmpty () {
        if (missionsToDo.isEmpty()) missionsToDo.add(createRandomMission());
    }

    public Mission createRandomMissionToRover (int idRover) {
        Random rand = new Random();
        Mission.MissionType type = Mission.MissionType.values()[rand.nextInt(Mission.MissionType.values().length)];
        Point3D coords = new Point3D(rand.nextInt(50), rand.nextInt(50), rand.nextInt(50));
        int area =  rand.nextInt(1,50);
        int time = rand.nextInt(10,120);            // 2 mins max
        int updateTime = rand.nextInt(1,time/3);    // 3 updates per mission min
        boolean isUrgent = rand.nextInt(100) < 10;        // 10% chance of being urgent

        return new Mission (idRover, type, coords, area, time, updateTime, isUrgent);
    }

    private Mission createRandomMission () {
        return createRandomMissionToRover(-1);
    }

    public MothershipMissions () {
        Random rand = new Random();
        new Thread (() -> {
            try {
            while (true) {
                missionsToDo.add(createRandomMission());
                int sleepFor = rand.nextInt(30,60); // new missions every 30 secs minimum, 60 seconds max
                Thread.sleep(sleepFor * 1000L);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    /*
    - create a mission specifically for a rover -> todo
        a follow-up of another mission?
        send it immediately to the rover.
    */
}
