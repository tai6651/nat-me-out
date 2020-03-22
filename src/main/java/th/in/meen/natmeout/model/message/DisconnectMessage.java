package th.in.meen.natmeout.model.message;

import th.in.meen.natmeout.util.PacketUtil;

public class DisconnectMessage implements TunnelMessage {

    private short connectionId;

    public DisconnectMessage(short connectionId)
    {
        this.connectionId = connectionId;
    }

    @Override
    public COMMAND getCommand() {
        return COMMAND.DISCONNECT;
    }

    @Override
    public byte[] getPayload() {
        return PacketUtil.convertFromShortToBytes(connectionId);
    }
}
