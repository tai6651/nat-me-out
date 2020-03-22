package th.in.meen.natmeout.tunneler;

import th.in.meen.natmeout.model.message.TunnelMessage;

import java.util.Map;

public interface PublicSideTunneler {
    void initialize(Map<String, Object> configuration);
    void transmitMessage(TunnelMessage tunnelMessage);
    TunnelMessage receiveMessage();
}
