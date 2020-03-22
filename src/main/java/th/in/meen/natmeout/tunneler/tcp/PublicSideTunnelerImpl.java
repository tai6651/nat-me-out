package th.in.meen.natmeout.tunneler.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import th.in.meen.natmeout.model.message.DataMessage;
import th.in.meen.natmeout.model.message.TunnelMessage;
import th.in.meen.natmeout.tunneler.PublicSideTunneler;
import th.in.meen.natmeout.util.PacketUtil;

import javax.xml.bind.DatatypeConverter;
import java.util.Map;

public class PublicSideTunnelerImpl implements PublicSideTunneler {

    private static final Logger log = LoggerFactory.getLogger(PublicSideTunnelerImpl.class);

    @Override
    public void initialize(Map<String, Object> configuration) {

    }

    @Override
    public void transmitMessage(TunnelMessage tunnelMessage) {

        //Message structure is
        // MSG_LENGTH | COMMAND_MODE | PAYLOAD
        // MSG_LENGTH = LENGTH(COMMAND_MODE | PAYLOAD)
        // LENGTH(COMMAND_MODE) = 1
        // LENGTH(PAYLOAD) = payload.length
        // LENGTH(MSG_LENGTH) = 2
        byte[] payload = tunnelMessage.getPayload();
        short messageLength = (short) (payload.length+1);
        byte[] messageLengthByes = PacketUtil.convertFromShortToBytes(messageLength);

        byte[] dataToSend = new byte[payload.length+3];
        System.arraycopy(messageLengthByes, 0, dataToSend, 0, 2);
        dataToSend[2] = PacketUtil.convertFromCommandToByte(tunnelMessage.getCommand());
        System.arraycopy(payload, 0, dataToSend, 3, payload.length);

        log.debug("Transmitting message type " + tunnelMessage.getCommand() + " - " + DatatypeConverter.printHexBinary(dataToSend));

        //TODO: Send to NAT Side
    }

    @Override
    public TunnelMessage receiveMessage() {
        //TODO: Receive from NAT side
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new DataMessage((short)0, "HELLO FROM Another side".getBytes());
    }
}
