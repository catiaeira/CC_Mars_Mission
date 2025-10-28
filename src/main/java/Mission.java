
public class Mission {
    private int id;
    private MissionType missionType;
    private String area;
    private int duration; // minutes
    // updates: the mission must define how and how often the rover reports back to the mothership

    enum MissionType {
        EXPLORE,
        COLLECT_ROCKS,
        TEST_ATMOSPHERE,
        BUILD_BASE
    }
}
