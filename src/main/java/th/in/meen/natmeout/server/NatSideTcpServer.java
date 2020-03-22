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

public class NatSideTcpServer {

    private static final Logger log = LoggerFactory.getLogger(NatSideTcpServer.class);

    private BlockingQueue<TunnelMessage> rxQueue;

    private NatSideTunneler natSideTunneler;

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
        startTxRxLoop();

    }

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
        rxDispatcherThread.setName("NatSideRxDispatcher");
        rxDispatcherThread.start();
    }


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
            }
        });
        txThread.setName("NatSideTxThread");
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
            }
        });
        rxThread.setName("NatSideRxThread");
        rxThread.start();
    }
}
