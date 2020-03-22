package th.in.meen.natmeout.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import th.in.meen.natmeout.model.config.PublicSideTcpConfigItem;
import th.in.meen.natmeout.model.message.ConnectMessage;
import th.in.meen.natmeout.model.message.DataMessage;
import th.in.meen.natmeout.model.message.DisconnectMessage;
import th.in.meen.natmeout.model.message.TunnelMessage;
import th.in.meen.natmeout.tunneler.PublicSideTunneler;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PublicSideTcpServer {

    private static final Logger log = LoggerFactory.getLogger(PublicSideTcpServer.class);

    private BlockingQueue<TunnelMessage> txQueue;
    private BlockingQueue<TunnelMessage> rxQueue;


    Map<Short, BlockingQueue<DataMessage>> rxQueueForEachConnection;
    Map<Short, Thread> txThreadEachConnectionMap;
    Map<Short, Thread> rxThreadEachConnectionMap;

    private PublicSideTunneler publicSideTunneler;

    //Incremental connection id
    private short connectionId = 0;

    public PublicSideTcpServer(PublicSideTcpConfigItem publicSideTcpConfigItem) throws Exception {
        //Setup Tx Rx Queue
        txQueue = new LinkedBlockingQueue<>();
        rxQueue = new LinkedBlockingQueue<>();

        rxQueueForEachConnection = new HashMap<>();
        txThreadEachConnectionMap = new HashMap<>();
        rxThreadEachConnectionMap = new HashMap<>();


        //Start our Dispatcher
        startDispatcherLoop();

        //Init tunneler by class name
        log.info("Creating tunneler from " + publicSideTcpConfigItem.getTunnelProtocolClass());
        Class<?> c = Class.forName(publicSideTcpConfigItem.getTunnelProtocolClass());
        Constructor<?> cons = c.getConstructor();
        publicSideTunneler = (PublicSideTunneler) cons.newInstance();
        publicSideTunneler.initialize(publicSideTcpConfigItem.getTunnelProtocolConfig());
        startTxRxLoop();


        //Setup TCP Socket for listener for client
        ServerSocket serverSocket = new ServerSocket(publicSideTcpConfigItem.getPublicSidePort());
        while(true)
        {
            log.info("Waiting for new client connection on port " + publicSideTcpConfigItem.getPublicSidePort());

            Socket socket = serverSocket.accept();
            short currentConnectionId = connectionId++;
            log.info("Incoming connection from " + socket.getRemoteSocketAddress().toString() + " assigned connectionId = " + currentConnectionId);
            txQueue.put(new ConnectMessage(currentConnectionId));
            createTxRxProcessorForEachConnection(currentConnectionId, socket);
        }
    }

    public void createTxRxProcessorForEachConnection(short currentConnectionId, Socket connectionSocket)
    {
        rxQueueForEachConnection.put(currentConnectionId, new LinkedBlockingQueue<>());



        Thread inputStreamReaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    inputStream = connectionSocket.getInputStream();

                    while(true)
                    {
                        byte[] buffer = new byte[4096];
                        int dataLength = inputStream.read(buffer);
                        if(dataLength < 0)
                        {
                            txQueue.put(new DisconnectMessage(currentConnectionId));
                            break;
                        }
                        else
                        {
                            //Create byte array exactly match to actual content length
                            byte[] payload = new byte[dataLength];
                            System.arraycopy(buffer,0, payload,0, dataLength);
                            DataMessage dataMessage = new DataMessage(currentConnectionId, payload);
                            txQueue.put(dataMessage);
                        }
                    }
                } catch (Exception e) {
                    try {
                        txQueue.put(new DisconnectMessage(currentConnectionId));
                    } catch (InterruptedException ex) {
                        log.warn(ex.getMessage(), ex);
                    }
                    log.error(e.getMessage(), e);
                }


                cleanupConnection(currentConnectionId);
            }
        });
        txThreadEachConnectionMap.put(currentConnectionId, inputStreamReaderThread);
        inputStreamReaderThread.setName("Tx for " + currentConnectionId);
        inputStreamReaderThread.start();


        Thread outputStreamWriterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream outputStream = null;
                try {
                    outputStream = connectionSocket.getOutputStream();
                    BlockingQueue<DataMessage> rxQueue = rxQueueForEachConnection.get(currentConnectionId);
                    while(true)
                    {
                        DataMessage dataMessage = rxQueue.poll(50, TimeUnit.MILLISECONDS);
                        if(dataMessage != null)
                            outputStream.write(dataMessage.getData());
                    }
                } catch (Exception e) {
                    try {
                        txQueue.put(new DisconnectMessage(currentConnectionId));
                    } catch (InterruptedException ex) {
                        log.warn(ex.getMessage(), ex);
                    }
                    log.error(e.getMessage(), e);
                }

                cleanupConnection(currentConnectionId);
            }
        });
        rxThreadEachConnectionMap.put(currentConnectionId, outputStreamWriterThread);
        outputStreamWriterThread.setName("Rx for " + currentConnectionId);
        outputStreamWriterThread.start();
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
                        dispatchMessage(message);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        rxDispatcherThread.start();
    }

    public void dispatchMessage(TunnelMessage message) {
        if (message != null) {
            //TODO: Dispatch based on command
            switch (message.getCommand()) {
                case DATA:
                    DataMessage dataMessage = new DataMessage(message.getPayload());
                    BlockingQueue<DataMessage> blockingQueue = rxQueueForEachConnection.get(dataMessage.getConnectionId());
                    if (blockingQueue == null) {
                        log.warn("Rx queue for connection id " + dataMessage.getConnectionId() + " not found, discard msg");
                        break;
                        //blockingQueue = new LinkedBlockingQueue<>();
                        //rxQueueForEachConnection.put(dataMessage.getConnectionId(), blockingQueue);
                    }

                    try {
                        blockingQueue.put(dataMessage);
                    } catch (InterruptedException e) {

                    }
                    break;
            }
        }
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
                        TunnelMessage txMessage = txQueue.poll(50, TimeUnit.MILLISECONDS);
                        if(txMessage != null)
                            publicSideTunneler.transmitMessage(txMessage);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        txThread.start();

        //Start rx q loop
        Thread rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        TunnelMessage rxMessage = publicSideTunneler.receiveMessage();
                        if (rxMessage != null)
                            rxQueue.put(rxMessage);

                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        rxThread.start();
    }

    public void cleanupConnection(short connectionId)
    {
        try {
            Thread txThreadEachConnection = txThreadEachConnectionMap.get(connectionId);
            if (txThreadEachConnection != null)
                txThreadEachConnection.interrupt();
        }
        catch (Exception ex)
        {

        }

        try {
            Thread rxThreadEachConnection = rxThreadEachConnectionMap.get(connectionId);
            if (rxThreadEachConnection != null)
                rxThreadEachConnection.interrupt();
        }
        catch (Exception ex)
        {

        }

        rxQueueForEachConnection.put(connectionId, null);

    }
}
