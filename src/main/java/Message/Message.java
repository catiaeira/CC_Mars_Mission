package Message;


import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.io.Serializable;


public class Message implements Serializable {
    protected static int msgIds = 1;

    // Identificador único (Lógica de Aplicação)
    protected int messageId;
    protected MessageDataTypes messageDataType;
    protected MessageData data;

    public enum MessageDataTypes {
        MISSION,
        REQUEST_MISSION,
        MISSION_UPDATE,
        ROVER_INIT,
        ROVER_TELEMETRY,
        ACK;
    }

    // Construtor Base (Gera ID automático - 1 a 255)
    public Message(MessageDataTypes type, MessageData data) {
        this.messageId = msgIds++;
        if (msgIds > 255) msgIds = 1;
        this.messageDataType = type;
        this.data = data;
    }

    // Construtor para Deserialização (ID já existe)
    public Message(int messageId, MessageDataTypes type, MessageData data) {
        this.messageId = messageId;
        this.messageDataType = type;
        this.data = data;
    }

    // --- SERIALIZAÇÃO TCP (Leve) ---
    // [TotalLen] [ID] [Type] [Payload]
    public byte[] convertMessageToBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // HEADER SIMPLES
            out.write((byte) messageId);
            out.write((byte) messageDataType.ordinal());

            // PAYLOAD
            byte[] dataBytes = data.convertMessageDataToBytes();
            out.write(dataBytes); // Assumindo que o payload gere os seus bytes corretamente

            byte[] bytes = out.toByteArray();

            // Adicionar tamanho total no início (Protocolo 1 byte length)
            byte[] bytesWithLength = new byte[bytes.length + 1];
            bytesWithLength[0] = (byte) bytesWithLength.length;
            System.arraycopy(bytes, 0, bytesWithLength, 1, bytes.length);

            return bytesWithLength;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar Message (TCP)", e);
        }
    }

    // --- DESERIALIZAÇÃO TCP ---
    public static Message convertBytesToMessage(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int totalLength = Byte.toUnsignedInt(buffer.get()); // Consome tamanho
        int messageId = Byte.toUnsignedInt(buffer.get());
        int messageDataTypeOrdinal = Byte.toUnsignedInt(buffer.get());

        MessageDataTypes dataType = MessageDataTypes.values()[messageDataTypeOrdinal];

        // Calcular tamanho do payload
        // Header TCP = Length(1) + ID(1) + Type(1) = 3 bytes
        int headerSize = 3;
        int dataLen = totalLength - headerSize;

        byte[] dataBytes = new byte[dataLen];
        buffer.get(dataBytes, 0, dataLen);

        MessageData mData = parseMessageData(dataType, dataBytes);
        return new Message(messageId, dataType, mData);
    }

    // Método auxiliar partilhado com a filha UDP
    public static MessageData parseMessageData(MessageDataTypes type, byte[] dataBytes) {
        switch (type) {
            case MISSION: return MissionMessage.convertBytesToMessageData(dataBytes);
            case REQUEST_MISSION: return RequestMission.convertBytesToMessageData(dataBytes);
            case MISSION_UPDATE: return UpdateMission.convertBytesToMessageData(dataBytes);
            case ROVER_INIT: return RoverInitMessage.convertBytesToMessageData(dataBytes);
            case ROVER_TELEMETRY: return RoverTelemetryMessage.convertBytesToMessageData(dataBytes);
            case ACK: return ACKMessage.convertBytesToMessageData(dataBytes);
            default: return null;
        }
    }

    // GETTERS
    public int getMessageId() { return messageId; }
    public MessageDataTypes getMessageDataType() { return messageDataType; }
    public MessageData getMessageData() { return data; }
}
