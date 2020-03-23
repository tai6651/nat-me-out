package th.in.meen.natmeout.tunneler;

import th.in.meen.natmeout.model.exception.TunnelerException;
import th.in.meen.natmeout.model.message.ConnectMessage;
import th.in.meen.natmeout.model.message.DataMessage;
import th.in.meen.natmeout.model.message.DisconnectMessage;
import th.in.meen.natmeout.model.message.TunnelMessage;

import java.util.Map;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public interface NatSideTunneler {
    void initialize(String natSideDestinationHost, Integer natSideDestinationPort, Map<String, Object> configuration) throws TunnelerException;
    void transmitMessage(TunnelMessage tunnelMessage);
    TunnelMessage receiveMessage();

    TunnelMessage pollMessageFromTxQueue();

    void handleConnectMessage(ConnectMessage connectMessage);
    void handleDisconnectMessage(DisconnectMessage connectMessage);
    void handleDataMessage(DataMessage connectMessage);
}
