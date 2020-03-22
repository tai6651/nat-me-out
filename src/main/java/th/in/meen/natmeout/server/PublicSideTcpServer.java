package th.in.meen.natmeout.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.awt.image.ImageWatched;
import th.in.meen.natmeout.model.config.PublicSideTcpConfigItem;
import th.in.meen.natmeout.model.message.DataMessage;
import th.in.meen.natmeout.model.message.TunnelMessage;
import th.in.meen.natmeout.tunneler.PublicSideTunneler;
import th.in.meen.natmeout.tunneler.tcp.PublicSideTunnelerImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PublicSideTcpServer {

    private static final Logger log = LoggerFactory.getLogger(PublicSideTcpServer.class);

    private BlockingQueue<TunnelMessage> txQueue;
    private BlockingQueue<TunnelMessage> rxQueue;


    Map<Short, BlockingQueue<DataMessage>> rxQueueForEachConnection;

    private PublicSideTunneler publicSideTunneler;

    //Incremental connection id
    private short connectionId = 0;

    public PublicSideTcpServer(PublicSideTcpConfigItem publicSideTcpConfigItem) throws IOException {
        //Setup Tx Rx Queue
        txQueue = new LinkedBlockingQueue<>();
        rxQueue = new LinkedBlockingQueue<>();


        //TODO: Init tunneler by config
        publicSideTunneler = new PublicSideTunnelerImpl();
        publicSideTunneler.initialize(publicSideTcpConfigItem.getTunnelProtocolConfig());
        startTxRxLoop();

        //Start our Dispatcher
        startRxDispatcherLoop();

        //Setup TCP Socket for listener for client
        ServerSocket serverSocket = new ServerSocket(publicSideTcpConfigItem.getPublicSidePort());
        while(true)
        {
            log.info("Waiting for new client connection");

            Socket socket = serverSocket.accept();
            short currentConnectionId = connectionId++;
            log.info("Incoming connection from " + socket.getRemoteSocketAddress().toString() + " assigned connectionId = " + currentConnectionId);
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
                        if(dataLength == 0)
                        {
                            //TODO: Send disconnect
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
                } catch (IOException | InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
        inputStreamReaderThread.start();


        Thread outputStreamWriterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream outputStream = null;
                try {
                    outputStream = connectionSocket.getOutputStream();
                    BlockingQueue<DataMessage> rxQueue = rxQueueForEachConnection.get(connectionId);
                    while(true)
                    {
                        DataMessage dataMessage = rxQueue.poll(50, TimeUnit.MILLISECONDS);
                        if(dataMessage != null)
                            outputStream.write(dataMessage.getData());
                    }
                } catch (IOException | InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
        outputStreamWriterThread.start();
    }

    public void startRxDispatcherLoop()
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
                        log.warn("Rx queue for connection id " + dataMessage.getConnectionId() + " not found, create new!");
                        blockingQueue = new LinkedBlockingQueue<>();
                        rxQueueForEachConnection.put(dataMessage.getConnectionId(), blockingQueue);
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
}
