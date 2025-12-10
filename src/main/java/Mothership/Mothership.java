package Mothership;

import Message.*;
import Mission.Mission;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap; // <--- IMPORTANTE

public class Mothership { // controller
    // 1. MUDANÇA: Usar ConcurrentHashMap para suportar vários Rovers ao mesmo tempo
    private final Map<Integer, RoverInfo> rovers = new ConcurrentHashMap<>();

    public MothershipMissions mothershipMissions;
    public MothershipConnection connection;

    public static void main(String[] args) {
        Mothership mothership = new Mothership();
        mothership.connection = new MothershipConnection(mothership);
        mothership.connection.startServer();
        mothership.mothershipMissions = new MothershipMissions(mothership);
    }

    // --- LÓGICA DE TELEMETRIA ---
    public void updateRoverInfoWithTelemetry(Message msg) {
        if (msg.getMessageDataType() != Message.MessageDataTypes.ROVER_TELEMETRY) return;

        RoverTelemetryMessage telemetry = (RoverTelemetryMessage) msg.getMessageData();
        int roverId = telemetry.getId();
        RoverInfo roverInfo = this.rovers.get(roverId);

        if (roverInfo == null) return;

        roverInfo.updateLastTelemetryMessage(telemetry);
        roverInfo.updateLastActiveTimestamp(System.currentTimeMillis());
    }

    // Método sincronizado para garantir que não há conflitos ao criar novos rovers
    public synchronized void storeRoverInfoConnection (Message msg, InetAddress ip, int port) {
        if (msg.getMessageDataType() != Message.MessageDataTypes.ROVER_INIT) return;
        RoverInitMessage roverInit = (RoverInitMessage) msg.getMessageData();
        int roverId = roverInit.getId();

        // Verifica se já existe
        RoverInfo roverInfo = this.rovers.get(roverId);
        if (roverInfo != null) {
            // Se já existe, não fazemos nada (o generateReply trata dos duplicados)
            // System.out.println("[Mothership] Rover " + roverId + " already exists in map.");
            return;
        }

        System.out.println("[Mothership] New rover initiating: ID " + roverId);
        roverInfo = new RoverInfo(roverId, ip, port);
        this.rovers.put(roverId, roverInfo);
        roverInfo.updateLastActiveTimestamp(System.currentTimeMillis());
    }

    public void sendMission(MissionMessage msg) {
        connection.sendMission(msg);
    }

    // --- LÓGICA DE RESPOSTA (AGORA COM FILTRO POR ID) ---
    public synchronized MessageUDP generateReply(MessageUDP receivedMsg, int ackNum) {
        // 1. Ignorar ACKs
        if (receivedMsg.getMessageDataType() == Message.MessageDataTypes.ACK) {
            return null;
        }

        // 2. IDENTIFICAR O ROVER (Extrair ID da mensagem)
        int roverIdTemp = -1;
        if (receivedMsg.getMessageDataType() == Message.MessageDataTypes.REQUEST_MISSION) {
            roverIdTemp = ((RequestMission) receivedMsg.getMessageData()).getIdRover();
        } else if (receivedMsg.getMessageDataType() == Message.MessageDataTypes.ROVER_INIT) {
            roverIdTemp = ((RoverInitMessage) receivedMsg.getMessageData()).getId();
        }

        // 3. OBTER INFO ESPECÍFICA DO ROVER
        // Aqui garantimos que estamos a olhar APENAS para a cache deste ID.
        // O Seq=0 do Rover 3 vai buscar o rInfo do Rover 3.
        // O Seq=0 do Rover 5 vai buscar o rInfo do Rover 5.
        RoverInfo rInfo = (roverIdTemp != -1) ? this.rovers.get(roverIdTemp) : null;
        if (rInfo == null) {
            System.out.println("[CRITICO] generateReply: rInfo é NULL para a mensagem Seq=" + receivedMsg.getSequenceNumber());
        } else {
            System.out.println("[DEBUG] generateReply: rInfo OK. LastProcessed=" + rInfo.getLastProcessedSequenceNumber() + " | Recebido=" + receivedMsg.getSequenceNumber());
        }
        // Se pediu missão e não o conhecemos, ignoramos (para obrigar Init correto)
        if (rInfo == null && receivedMsg.getMessageDataType() == Message.MessageDataTypes.REQUEST_MISSION) {
            return null;
        }

        // 4. VERIFICAÇÃO DE DUPLICADOS / CACHE (Por Rover)
        if (rInfo != null && rInfo.getLastProcessedSequenceNumber() >= receivedMsg.getSequenceNumber()) {

            // Filtro de tempo: evitar resposta dupla instantânea
            long timeDiff = System.currentTimeMillis() - rInfo.getLastReplyTime();
            if (timeDiff < 1000) {
                return null;
            }

            // Lógica de Cache
            if (rInfo.getLastSentReplyUDP() != null) {
                MessageUDP cached = rInfo.getLastSentReplyUDP();

                // Validação de Tipos
                if (receivedMsg.getMessageDataType() == Message.MessageDataTypes.REQUEST_MISSION &&
                        cached.getMessageDataType() == Message.MessageDataTypes.MISSION) {

                    System.out.println("[Mothership] Rover " + roverIdTemp + ": Retransmitindo Missão (Seq " + cached.getSequenceNumber() + ").");
                    rInfo.setLastReplyTime(System.currentTimeMillis());
                    return cached;

                } else if (receivedMsg.getMessageDataType() == Message.MessageDataTypes.ROVER_INIT) {
                    System.out.println("[Mothership] Rover " + roverIdTemp + ": Retransmitindo Init.");
                    rInfo.setLastReplyTime(System.currentTimeMillis());
                    return cached;
                }
            }
        }

        // 5. GERAÇÃO DE NOVA RESPOSTA
        MessageUDP reply = null;
        int fragID = 0, fragIdx = 0, totalFrags = 1;

        switch (receivedMsg.getMessageDataType()) {
            case ROVER_INIT:
                int givenID = ((RoverInitMessage) receivedMsg.getMessageData()).getId();

                // Se o rover já existe e tem telemetria, ignoramos inits tardios
                if (rInfo != null && rInfo.getLastTelemetryMessage() != null) {
                    System.out.println("[Mothership] Ignorando Init duplicado de Rover ativo (" + givenID + ").");
                    reply = null;
                    break;
                }

                int idParaRegistar = givenID;
                // Se já existe, mantemos o ID mas reiniciamos sequências se necessário

                int seqInit = (rInfo != null) ? rInfo.getAndIncrementOutputSequenceNumber() : 0;

                reply = new MessageUDP(
                        seqInit, ackNum, fragID, fragIdx, totalFrags,
                        Message.MessageDataTypes.ROVER_INIT,
                        new RoverInitMessage(idParaRegistar)
                );
                break;

            case REQUEST_MISSION:
                RequestMission req = (RequestMission) receivedMsg.getMessageData();

                Mission mission = this.mothershipMissions.getMission();
                if (mission == null) {
                    mission = this.mothershipMissions.createRandomMissionToRover(req.getIdRover());
                }
                mission.setRoverId(req.getIdRover());
                mothershipMissions.startMission(mission);

                int seqMission = rInfo.getAndIncrementOutputSequenceNumber();

                reply = new MessageUDP(
                        seqMission, ackNum, fragID, fragIdx, totalFrags,
                        Message.MessageDataTypes.MISSION,
                        new MissionMessage(mission));

                System.out.println("[Mothership] Rover " + req.getIdRover() + ": Nova missão atribuída.");
                break;

            case MISSION_UPDATE:

                break;
        }

        if (reply == null) {
            if (rInfo != null) {
                System.out.println("[DEBUG] Criando ACK manual para Seq=" + receivedMsg.getSequenceNumber());
                int seqAck = rInfo.getAndIncrementOutputSequenceNumber();
                reply = new MessageUDP(seqAck, ackNum, 0, 0, 1, MessageUDP.MessageDataTypes.ACK, null);
            } else {
                System.out.println("[CRITICO] Não consigo criar ACK porque rInfo é NULL!");
            }
        }
        // -----------------------------------------

        if (reply != null && rInfo != null) {
            rInfo.setLastSentReplyUDP(reply);
            rInfo.setLastProcessedSequenceNumber(receivedMsg.getSequenceNumber());
            rInfo.setLastReplyTime(System.currentTimeMillis());
            System.out.println("[DEBUG] Estado atualizado. Novo LastProcessed=" + rInfo.getLastProcessedSequenceNumber());
        }

        return reply;
    }

    public void removeRover(int roverId) {
        if (rovers.containsKey(roverId)) {
            rovers.remove(roverId);
            System.out.println("[Mothership] Rover " + roverId + " desconectado e removido da lista.");
        }
    }

    public Collection<RoverInfo> getRoverInfo() { return this.rovers.values(); }
    public Set<Integer> getRoverIDs() { return this.rovers.keySet(); }
    public Collection<Mission> getActiveMissions() { return mothershipMissions.getActiveMissions(); }
    public Collection<Mission> getPastMissions() { return mothershipMissions.getPastMissions(); }
    public Collection<Mission> getFutureMissions() { return mothershipMissions.getFutureMissions(); }
    public ArrayList<RoverTelemetryMessage> getLastTelemetry() {
        ArrayList<RoverTelemetryMessage> res = new ArrayList<RoverTelemetryMessage>();
        for (RoverInfo i : this.rovers.values()) {
            if (i.getLastTelemetryMessage() != null) res.add(i.getLastTelemetryMessage());
        }
        return res;
    }
    public RoverInfo getRoverById(int id) { return this.rovers.get(id); }
}