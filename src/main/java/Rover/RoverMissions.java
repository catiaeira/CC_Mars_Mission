package Rover;

import Message.UpdateMission;
import Mission.Mission;
import Utils.Point3D;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RoverMissions {
    private final Rover rover;
    private final PriorityBlockingQueue<Mission> missionsToDo;
    private volatile Mission currentMission = null;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private long missionStartTime = 0;

    public RoverMissions(Rover rover) {
        this.rover = rover;
        this.missionsToDo = new PriorityBlockingQueue<>();
    }
    public void addMission(Mission mission) {
        this.missionsToDo.put(mission);
    }
    /*
    TODO
        rover needs to send update missions messages
        error case
        mothership has to create missions automatically
        yet to test:
            priority queue
            start another mission
     */
    final long   LOOP_INTERVAL = 1000; // update every second
    final double CHARGE_RATE = 5; // per second
    final double SPEED = 1; // per second
    final double CONSUMPTION_WALKING = 0.1; // per sec
    final double CONSUMPTION_COLLECTING_ROCKS = 1;
    final double CONSUMPTION_EXPLORE = 2;
    final double CONSUMPTION_GETTING_SAMPLES = 0.5;

    public void run () {
        new Thread(() -> {
            try {
                long busyUntil = 0;
                while (true) {
                    switch (rover.getState()) {
                        case IDLE -> {
                            busyUntil = idle();
                        }
                        case CHARGING -> {
                            charge(busyUntil);
                            busyUntil = 0;
                        }
                        case ON_THE_WAY -> {
                            busyUntil = onTheWay(busyUntil);
                        }
                        case IN_MISSION -> {
                            busyUntil = doMission(busyUntil);
                        }
                        // missing error case -> when to apply it?
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public long idle () throws InterruptedException { // should always be at base
        System.out.println("in idle");
        if (!rover.getPosition().equals(rover.getBase().getPosition())) System.out.println("Idle but not at base!");

        if (currentMission == null || currentMission.isCompleted()) currentMission = missionsToDo.take(); // blocks here
        System.out.println("has mission");
        // decision to charge
        if (!willBatterySurvive(currentMission)) {
            this.rover.setState(Rover.MissionState.CHARGING);
            System.out.println("going to charge!");
            return System.currentTimeMillis() + Math.round (Math.ceil (timeToFullyCharge()));
        }
        // free inventory
        List<String> inventory = rover.getInventory();
        if (!inventory.isEmpty()) {
            System.out.println("cleaning inventory");
            inventory.forEach(inventoryItem -> {rover.getBase().addItem(inventoryItem);});
            this.rover.clearInventory();
        }

        System.out.println("leaving idle, becoming on the way");
        rover.setState(Rover.MissionState.ON_THE_WAY);
        return (long) (System.currentTimeMillis() + timeBetweenPlaces(rover.getPosition(),currentMission.getAreaCoordinates()) * 1000L);
    }

    public long doMission (long busyUntil) throws InterruptedException {
        System.out.println("in mission");
        long currentTime = System.currentTimeMillis();
        Random random = new Random();
        while (currentTime <= busyUntil) {
            long timeElapsed = Math.min(busyUntil - currentTime, LOOP_INTERVAL);
            decrementBattery (timeElapsed);

            // roam in mission area?

            // inventory management
            Mission.MissionType missionType = currentMission.getMissionType();
            if     (random.nextInt(100) < 5 &&                              // 5% chance every second
                    missionCollectsItems(missionType) &&                           // mission involves items
                    rover.getMaxInventorySpace() > rover.getInventory().size()){   // must have inventory space
                System.out.println("[MISSION] New item added to inventory!");
                if      (missionType == Mission.MissionType.COLLECT_ROCKS)   rover.addToInventory("ROCK");
                else if (missionType == Mission.MissionType.TEST_ATMOSPHERE) rover.addToInventory("SAMPLE");
            }

            Thread.sleep(LOOP_INTERVAL);
            currentTime = System.currentTimeMillis();
        }
        this.currentMission.setCompleted();
        if (canDoNextMission()) {
            currentMission = missionsToDo.take();
            rover.setState(Rover.MissionState.ON_THE_WAY);
            System.out.print("[MISSION -> ON THE WAY TO NEW MISSION] Finished!");    // decision to on the way to new mission
            return System.currentTimeMillis() + currentMission.getMissionTime() * 1000L;
        }
        else {
            rover.setState(Rover.MissionState.ON_THE_WAY);
            System.out.println("[MISSION -> ON THE WAY] Finished!");        // decision to on the way to base
            return (long) (System.currentTimeMillis() + timeBetweenPlaces(rover.getPosition(), rover.getBase().getPosition()) * 1000);
        }
    }

    public void charge (long busyUntil) throws InterruptedException {
        System.out.println("charging");
        long currentTime = System.currentTimeMillis();
        while (currentTime <= busyUntil) {
            // only update battery if the time elapsed is within the remaining busyUntil time
            long timeElapsed = Math.min(busyUntil - currentTime, LOOP_INTERVAL);

            double batteryIncrement = timeElapsed * CHARGE_RATE / 1000;
            double newBatteryLevel = rover.getBatteryLevel() + batteryIncrement;

            rover.setBatteryLevel((int) Math.round(Math.min(100, newBatteryLevel))); // cap at 100
            System.out.printf("[CHARGING] Battery Telemetry: +%.2f%%. Current Level: %.2f\n",
                    batteryIncrement, rover.getBatteryLevel());

            Thread.sleep(LOOP_INTERVAL);
            currentTime = System.currentTimeMillis();
        }
        rover.setBatteryLevel(100);
        rover.setState(Rover.MissionState.IDLE);        // decision to idle
        System.out.printf("[CHARGING -> IDLE] CHARGE COMPLETE! Battery now %.2f.\n", rover.getBatteryLevel());
    }

    private long onTheWay(long busyUntil) throws InterruptedException {
        System.out.println("on the way");
        long currentTime = System.currentTimeMillis();
        Point3D objective;
        if (currentMission.isCompleted()) objective = rover.getBase().getPosition();
        else objective = currentMission.getAreaCoordinates();

        while (currentTime <= busyUntil) {
            long timeElapsed = Math.min(busyUntil - currentTime, LOOP_INTERVAL);
            decrementBattery (timeElapsed);

            double distanceIncrement = timeElapsed * SPEED / 1000;
            Point3D newPosition = Point3D.findMiddlePoint(rover.getPosition(), objective, distanceIncrement);

            rover.setPosition(newPosition);
            if (!currentMission.isCompleted())
                System.out.printf("[ON THE WAY -> MISSION] Currently at " + newPosition + " \n");
            else System.out.printf("[ON THE WAY -> BASE] Currently at " + newPosition + " \n");

            Thread.sleep(LOOP_INTERVAL);
            currentTime = System.currentTimeMillis();
        }

        if (!currentMission.isCompleted()) { // has just arrived at the mission site
            // decision to become in mission
            rover.setPosition(currentMission.getAreaCoordinates());
            rover.setState(Rover.MissionState.IN_MISSION);
            System.out.println("[ON THE WAY -> MISSION] Has arrived!");
            startNewMission (currentMission);
            return System.currentTimeMillis() + currentMission.getMissionTime() * 1000L;
        }
        // pick another mission if there's enough battery
        else if (canDoNextMission()) {
            currentMission = missionsToDo.take();
            rover.setState(Rover.MissionState.ON_THE_WAY);
            System.out.println("[ON THE WAY -> NEW MISSION] Has arrived!");
            return System.currentTimeMillis() + currentMission.getMissionTime() * 1000L;

        } else { // arrived to base
            // decision to become idle
            rover.setPosition(rover.getBase().getPosition());
            rover.setState(Rover.MissionState.IDLE);

            System.out.println("[ON THE WAY -> BASE] Has arrived!");
            return 0;
        }
    }

    private void decrementBattery (double timeElapsed) throws IllegalArgumentException {
        if (timeElapsed < 0) throw new IllegalArgumentException();

        double batteryDecrement;
        if (rover.getState() == Rover.MissionState.ON_THE_WAY) batteryDecrement = timeElapsed * CONSUMPTION_WALKING / 1000 ;
        else batteryDecrement = timeElapsed * getBatteryRate(currentMission.getMissionType()) / 1000 ;

        double newBatteryLevel = rover.getBatteryLevel() - batteryDecrement;
        if (newBatteryLevel < 0 || newBatteryLevel > 100) throw new IllegalArgumentException();

        rover.setBatteryLevel(newBatteryLevel);
        //System.out.printf("[BATTERY USAGE] Battery Telemetry: -%.2f%%. Current Level: %.2f\n", batteryDecrement, rover.getBatteryLevel());
    }

    private boolean willBatterySurvive (Mission m) {
        double consumptionPerMission;
        switch (m.getMissionType()){
            case EXPLORE         -> consumptionPerMission = CONSUMPTION_EXPLORE;
            case COLLECT_ROCKS   -> consumptionPerMission = CONSUMPTION_COLLECTING_ROCKS;
            case TEST_ATMOSPHERE -> consumptionPerMission = CONSUMPTION_GETTING_SAMPLES;
            default ->  consumptionPerMission = CONSUMPTION_WALKING;
        }

        double consumption = m.getMissionTime() * consumptionPerMission
                + timeBetweenPlaces(rover.getPosition(), m.getAreaCoordinates()) * CONSUMPTION_WALKING * 2;

        return (rover.getBatteryLevel() > consumption);
    }

    private double timeBetweenPlaces (Point3D a, Point3D b) {
        double dist = Math.sqrt(Math.pow((b.x-a.x),2) + Math.pow((b.y-a.y),2) + Math.pow((b.z-a.z),2));

        return dist/SPEED;
    }

    private boolean canDoNextMission() {
        if (missionsToDo.isEmpty()) return false;
        Mission next = missionsToDo.peek();
        Mission.MissionType missionType = next.getMissionType();
        return (!missionCollectsItems(missionType)   // if the mission involves inventory, needs at least one free space
                || this.rover.getInventory().size() < this.rover.getMaxInventorySpace())
            && willBatterySurvive(next); // needs battery
    }

    private double timeToFullyCharge () {
        return (100-this.rover.getBatteryLevel())/CHARGE_RATE; // 200 secs to fully charge
    }

    private double getBatteryRate(Mission.MissionType missionType) {
        if (missionType == null) return CONSUMPTION_WALKING;
        else return switch (missionType) {
            case EXPLORE -> CONSUMPTION_EXPLORE;
            case TEST_ATMOSPHERE -> CONSUMPTION_GETTING_SAMPLES;
            case COLLECT_ROCKS -> CONSUMPTION_COLLECTING_ROCKS;
        };
    }

    private boolean missionCollectsItems(Mission.MissionType missionType) {
        return missionType == Mission.MissionType.COLLECT_ROCKS || missionType == Mission.MissionType.TEST_ATMOSPHERE;
    }

    // Sending mission updates vvv

    public void startNewMission(Mission newMission) {
        this.currentMission = newMission;
        this.missionStartTime = System.currentTimeMillis();

        scheduler.scheduleAtFixedRate(this::sendUpdate,
                0, // initial delay
                newMission.getUpdateTime(),
                TimeUnit.SECONDS);
    }

    private void sendUpdate() {
        if (currentMission == null) return;

        Mission currM = this.currentMission;
        long currentTime = System.currentTimeMillis();

        long totalDuration = currM.getMissionTime() * 1000L;
        long timeElapsed = currentTime - missionStartTime;

        int progressPercent = 0;
        if (totalDuration > 0) progressPercent = (int) Math.min(100, Math.round((double) timeElapsed / totalDuration * 100));


        UpdateMission updateMission = new UpdateMission(
                currM.getMissionId(),
                rover.getId(),
                progressPercent
        );

        try {
            rover.sendUpdateMission(updateMission);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (progressPercent >= 100) {
            scheduler.close();
        }
    }
}
