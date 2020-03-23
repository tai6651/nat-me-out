package th.in.meen.natmeout.model.message;

import th.in.meen.natmeout.util.PacketUtil;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public class DisconnectMessage implements TunnelMessage {

    private short connectionId;

    public DisconnectMessage(short connectionId)
    {
        this.connectionId = connectionId;
    }

    public DisconnectMessage(byte[] payloadFromTunnel)
    {
        //Parse raw binary data into connectionId and data
        byte[] connectionIdByte = new byte[2];

        //Copy data to destination array
        System.arraycopy(payloadFromTunnel, 0, connectionIdByte, 0, 2);
        //Covert from byte to short
        this.connectionId = PacketUtil.convertFromBytesToShort(connectionIdByte);
    }

    @Override
    public COMMAND getCommand() {
        return COMMAND.DISCONNECT;
    }

    @Override
    public byte[] getPayload() {
        return PacketUtil.convertFromShortToBytes(connectionId);
    }

    public short getConnectionId() {
        return connectionId;
    }

    @Override
    public String toString() {
        return "DisconnectMessage{" +
                "connectionId=" + connectionId +
                '}';
    }
}
