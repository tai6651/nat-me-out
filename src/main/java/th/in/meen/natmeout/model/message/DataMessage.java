package th.in.meen.natmeout.model.message;

import th.in.meen.natmeout.util.PacketUtil;

import javax.xml.bind.DatatypeConverter;
import java.util.Arrays;

public class DataMessage implements TunnelMessage  {

    private short connectionId;
    private byte[] data;

    public DataMessage(byte[] payloadFromTunnel)
    {
        //Parse raw binary data into connectionId and data
        byte[] connectionIdByte = new byte[2];
        this.data = new byte[payloadFromTunnel.length-2];

        //Copy data to destination array
        System.arraycopy(payloadFromTunnel, 0, connectionIdByte, 0, 2);
        System.arraycopy(payloadFromTunnel, 2, this.data, 0, this.data.length);

        //Covert from byte to short
        this.connectionId = PacketUtil.convertFromBytesToShort(connectionIdByte);
    }

    public DataMessage(short connectionId, byte[] data){
        this.connectionId = connectionId;
        this.data = data;
    }

    @Override
    public COMMAND getCommand() {
        return COMMAND.DATA;
    }

    @Override
    public byte[] getPayload() {
        byte[] connectionIdBytes = PacketUtil.convertFromShortToBytes(connectionId);
        byte[] payload = new byte[data.length + 2];
        System.arraycopy(connectionIdBytes, 0, payload, 0, 2);
        System.arraycopy(data, 0, payload, 2, data.length);

        return payload;
    }

    public short getConnectionId() {
        return connectionId;
    }

    public byte[] getData() {
        return data;
    }


    @Override
    public String toString() {
        return "DataMessage{" +
                "connectionId=" + connectionId +
                ", data=" + DatatypeConverter.printHexBinary(data) +
                '}';
    }
}
