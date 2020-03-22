package th.in.meen.natmeout.model.message;

import th.in.meen.natmeout.util.PacketUtil;

public class ConnectMessage implements TunnelMessage {
    private short connectionId;
    public ConnectMessage(short connectionId)
    {
        this.connectionId = connectionId;
    }

    @Override
    public COMMAND getCommand() {
        return COMMAND.CONNECT;
    }

    @Override
    public byte[] getPayload() {
        return PacketUtil.convertFromShortToBytes(connectionId);
    }
}
