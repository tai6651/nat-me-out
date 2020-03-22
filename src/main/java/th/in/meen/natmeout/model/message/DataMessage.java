package th.in.meen.natmeout.model.message;

public class DataMessage implements TunnelMessage  {

    private short connectionId;
    private byte[] data;

    public DataMessage(byte[] payloadFromTunnel)
    {
        //TODO: Parse raw binary data into connectionId and data
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
        //TODO: Append connection id
        return data;
    }

    public short getConnectionId() {
        return connectionId;
    }

    public byte[] getData() {
        return data;
    }

}
