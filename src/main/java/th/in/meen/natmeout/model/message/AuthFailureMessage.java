package th.in.meen.natmeout.model.message;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public class AuthFailureMessage implements TunnelMessage {

    private String message;

    public AuthFailureMessage(byte[] payloadFromTunnel)
    {
        message = new String(payloadFromTunnel);
    }

    public AuthFailureMessage(String message)
    {
        this.message = message;
    }

    @Override
    public TunnelMessage.COMMAND getCommand() {
        return COMMAND.AUTH_FAILURE;
    }

    @Override
    public byte[] getPayload() {
        return message.getBytes();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "AuthFailureMessage{" +
                "message='" + message + '\'' +
                '}';
    }
}
