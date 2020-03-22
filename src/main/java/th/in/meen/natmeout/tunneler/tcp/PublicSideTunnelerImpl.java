package th.in.meen.natmeout.tunneler.tcp;

import th.in.meen.natmeout.model.message.TunnelMessage;
import th.in.meen.natmeout.tunneler.PublicSideTunneler;

import java.util.Map;

public class PublicSideTunnelerImpl implements PublicSideTunneler {
    @Override
    public void initialize(Map<String, Object> configuration) {

    }

    @Override
    public void transmitMessage(TunnelMessage tunnelMessage) {

    }

    @Override
    public TunnelMessage receiveMessage() {
        return null;
    }
}
