package th.in.meen.natmeout.model.message;

public class AuthSuccessMessage implements TunnelMessage {

    private String message;

    public AuthSuccessMessage(byte[] payloadFromTunnel)
    {
        message = new String(payloadFromTunnel);
    }

    public AuthSuccessMessage(String message)
    {
        this.message = message;
    }

    @Override
    public COMMAND getCommand() {
        return COMMAND.AUTH_SUCCESS;
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
        return "AuthSuccessMessage{" +
                "message='" + message + '\'' +
                '}';
    }
}
