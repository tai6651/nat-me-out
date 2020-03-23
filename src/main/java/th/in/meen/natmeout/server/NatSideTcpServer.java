package th.in.meen.natmeout.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import th.in.meen.natmeout.model.config.NatSideTcpConfigItem;
import th.in.meen.natmeout.model.message.ConnectMessage;
import th.in.meen.natmeout.model.message.DataMessage;
import th.in.meen.natmeout.model.message.DisconnectMessage;
import th.in.meen.natmeout.model.message.TunnelMessage;
import th.in.meen.natmeout.tunneler.NatSideTunneler;

import java.lang.reflect.Constructor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public class NatSideTcpServer {

    private static final Logger log = LoggerFactory.getLogger(NatSideTcpServer.class);

    //Rx queue (as Tx queue is in Tunneler Implementation)
    private BlockingQueue<TunnelMessage> rxQueue;

    //Our Tunneler object
    private NatSideTunneler natSideTunneler;

    /***
     * NAT side TCP Client
     * This is main class for creating connection to Public side and also to service behind NAT
     * This mean that this NAT side work as client only
     * It will create actual tunneller based on configured class name
     * @param natSideTcpConfigItem Configuration information
     * @throws Exception - Only if initialization failed (ex. invalid config)
     */
    public NatSideTcpServer(NatSideTcpConfigItem natSideTcpConfigItem) throws Exception {
        //Setup Rx Queue (Tx Queue is at Tunneler impl)
        rxQueue = new LinkedBlockingQueue<>();

        //Start our Dispatcher
        startDispatcherLoop();

        //Init tunneler by class name
        log.info("Creating tunneler from " + natSideTcpConfigItem.getTunnelProtocolClass());
        Class<?> c = Class.forName(natSideTcpConfigItem.getTunnelProtocolClass());
        Constructor<?> cons = c.getConstructor();
        natSideTunneler = (NatSideTunneler) cons.newInstance();
        natSideTunneler.initialize(natSideTcpConfigItem.getNatSideDestinationHost(), natSideTcpConfigItem.getNatSideDestinationPort(), natSideTcpConfigItem.getTunnelProtocolConfig());

        //Start Tx and Rx Loop
        startTxRxLoop();

    }

    /***
     * Create Dispatcher loop
     */
    public void startDispatcherLoop()
    {
        //Start rx dispatcher loop
        Thread rxDispatcherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true)
                {
                    try {
                        TunnelMessage message = rxQueue.poll(50, TimeUnit.MILLISECONDS);
                        if(message != null) {
                            if(message instanceof ConnectMessage)
                                natSideTunneler.handleConnectMessage((ConnectMessage) message);
                            else if(message instanceof DisconnectMessage)
                                natSideTunneler.handleDisconnectMessage((DisconnectMessage) message);
                            else if(message instanceof DataMessage)
                                natSideTunneler.handleDataMessage((DataMessage) message);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        rxDispatcherThread.setName("NatSideRx-Dispatcher");
        rxDispatcherThread.start();
    }


    /***
     * Start Tx message poller loop then transmit that message to public side
     * This is because each connection made to service behind NAT have to place Tx message to shared Tx queue
     * in NatSideTunneeler implementation
     * Message will be transmit to Public side by calling transmitMessage method in NatSideTunneler's interface
     * It is Tunneler's responsibility to transmit this message to Public side
     *
     * Start Rx message poller loop and put to Rx queue for dispatcher
     * Message will be receive by calling receiveMessage method in NatSideTunneler's interface
     * It is Tunneler's responsibility to implement the logic and return the tunnel message
     */
    public void startTxRxLoop()
    {
        //Start tx q loop
        Thread txThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true)
                {
                    try {
                        TunnelMessage txMessage = natSideTunneler.pollMessageFromTxQueue();
                        if(txMessage != null)
                            natSideTunneler.transmitMessage(txMessage);
                    } catch (Exception e) {
                        break;
                    }
                }
                log.error("Main Tx loop exited, Future message will not get processed, consider restart entire server");
            }
        });
        txThread.setName("NatSideTx-Main");
        txThread.start();

        //Start rx q loop
        Thread rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        TunnelMessage rxMessage = natSideTunneler.receiveMessage();
                        if (rxMessage != null)
                            rxQueue.put(rxMessage);

                    } catch (InterruptedException e) {
                        break;
                    }
                }
                log.error("Main Rx loop exited, Future message will not get processed, consider restart entire server");

            }
        });
        rxThread.setName("NatSideRx-Main");
        rxThread.start();
    }
}
