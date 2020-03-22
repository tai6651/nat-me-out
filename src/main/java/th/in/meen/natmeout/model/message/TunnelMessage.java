package th.in.meen.natmeout.model.message;

public interface TunnelMessage {

    public enum COMMAND
    {
        HELLO,
        CONNECT,
        DATA,
        DISCONNECT,
        PING,
        PONG
    }

    public COMMAND getCommand();
    public byte[] getPayload();

}
