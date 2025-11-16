package Mothership;

import Connection.MissionLinkServer;
import Connection.NetworkConfig;
import Connection.TelemetryStreamServer;

public class MothershipConnection {
    private MissionLinkServer missionLinkServer;
    private TelemetryStreamServer telemetryStreamServer;
    private Mothership mothership;

    public MothershipConnection(Mothership mothership) {
        this.mothership = mothership;
    }

    public void startServer() {
        NetworkConfig networkConfig = new NetworkConfig();

        String ml_port = networkConfig.getIp(NetworkConfig.ID.MISSION_LINK_PORT);
        String ts_port = networkConfig.getIp(NetworkConfig.ID.TELEMETRY_STREAM_PORT);
        try {
            missionLinkServer = new MissionLinkServer(Integer.parseInt(ml_port), mothership);
            Thread udpServer = new Thread(missionLinkServer);

            telemetryStreamServer = new TelemetryStreamServer(Integer.parseInt(ts_port), mothership);
            Thread tcpServer = new Thread(telemetryStreamServer);

            udpServer.start();
            tcpServer.start();

            System.out.println("Mothership is running MissionLink (UDP) on port " + ml_port + " and TelemetryStream (TCP) on port " + ts_port + ".");
        } catch (Exception e) {
            System.out.println("Failed to establish connections: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
