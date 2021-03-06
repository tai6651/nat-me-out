package th.in.meen.natmeout.model.message;

import th.in.meen.natmeout.util.PacketUtil;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public class ConnectMessage implements TunnelMessage {
    private short connectionId;
    public ConnectMessage(short connectionId)
    {
        this.connectionId = connectionId;
    }

    public ConnectMessage(byte[] payloadFromTunnel)
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
        return COMMAND.CONNECT;
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
        return "ConnectMessage{" +
                "connectionId=" + connectionId +
                '}';
    }
}
