package th.in.meen.natmeout.model.message;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
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
