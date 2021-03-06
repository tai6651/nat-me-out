package th.in.meen.natmeout.model.message;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public class HelloMessage implements TunnelMessage {

    private String secret;

    public HelloMessage(byte[] payloadFromTunnel)
    {
        secret = new String(payloadFromTunnel);
    }

    public HelloMessage(String secret)
    {
        this.secret = secret;
    }

    @Override
    public COMMAND getCommand() {
        return COMMAND.HELLO;
    }

    @Override
    public byte[] getPayload() {
        return secret.getBytes();
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public String toString() {
        return "HelloMessage{" +
                "secret=" + secret +
                '}';
    }
}
