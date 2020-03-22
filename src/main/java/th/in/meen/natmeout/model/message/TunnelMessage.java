package th.in.meen.natmeout.model.message;

public interface TunnelMessage {

    enum COMMAND
    {
        HELLO,
        CONNECT,
        DATA,
        DISCONNECT,
        AUTH_SUCCESS,
        AUTH_FAILURE,
        PING,
        PONG,
    }

    public COMMAND getCommand();
    public byte[] getPayload();

}
