package th.in.meen.natmeout.model.message;

public class HelloMessage implements TunnelMessage {

    private byte[] payload;

    @Override
    public COMMAND getCommand() {
        return COMMAND.HELLO;
    }

    @Override
    public byte[] getPayload() {
        return payload;
    }

    public void setAuthenticationSecret(String secret) {
        this.payload = secret.getBytes();
    }
}
