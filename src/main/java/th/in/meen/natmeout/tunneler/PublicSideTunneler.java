package th.in.meen.natmeout.tunneler;

import th.in.meen.natmeout.model.exception.TunnelerException;
import th.in.meen.natmeout.model.message.TunnelMessage;

import java.util.Map;

/***
 * Interface for Tunneler - This interface is for Public side
 * It should act as Server to wait for new connection from NAT side.
 *
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public interface PublicSideTunneler {

    /***
     * Initialize tunneler
     * @param configuration Tunneler specific configuration, It came as Key-Value pair
     * @throws TunnelerException - If initialization failed, it will throw TunnelerException
     */
    void initialize(Map<String, Object> configuration) throws TunnelerException;

    /***
     * Actual method to transmit message to NAT side
     * This method will be called when there is new message to send to NAT side
     * @param tunnelMessage Message to be transmit
     */
    void transmitMessage(TunnelMessage tunnelMessage);

    /***
     * Actual method to receive message from NAT side
     * Implementor must return TunnelMessage that received from NAT side
     * Please note that this method will be called under while(true) loop
     * @return TunnelMessage from NAT side, null mean no new message
     */
    TunnelMessage receiveMessage();
}
