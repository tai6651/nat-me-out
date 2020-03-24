package th.in.meen.natmeout.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import th.in.meen.natmeout.model.PublicSideConnection;
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
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public class PublicSideTcpServer {

    private static final Logger log = LoggerFactory.getLogger(PublicSideTcpServer.class);

    //Shared Tx queue
    private BlockingQueue<TunnelMessage> txQueue;

    //Connection map
    private Map<Short, PublicSideConnection> connectionMap;

    //Our Tunneler object
    private PublicSideTunneler publicSideTunneler;

    //Incremental connection id, This will increase every time that new connection created
    private short connectionId = 0;

    /***
     * Public side TCP Server
     * This is main class for handling TCP Connection from Public side
     * It will create the actual tunneller based on configured class name
     * @param publicSideTcpConfigItem Configuration information
     * @throws Exception - Only if initialization failed (ex. invalid config)
     */
    public PublicSideTcpServer(PublicSideTcpConfigItem publicSideTcpConfigItem) throws Exception {
        //Setup Tx Queue
        txQueue = new LinkedTransferQueue<>();

        //Setup connection map
        connectionMap = new HashMap<>();


        //Init tunneler by class name
        log.info("Creating tunneler from " + publicSideTcpConfigItem.getTunnelProtocolClass());
        Class<?> c = Class.forName(publicSideTcpConfigItem.getTunnelProtocolClass());
        Constructor<?> cons = c.getConstructor();
        publicSideTunneler = (PublicSideTunneler) cons.newInstance();
        publicSideTunneler.initialize(publicSideTcpConfigItem.getTunnelProtocolConfig());


        //Start Tx and Rx loop
        startTxRxLoop();


        //Setup TCP Socket for listener for client
        ServerSocket serverSocket = new ServerSocket(publicSideTcpConfigItem.getPublicSidePort());
        while(true)
        {
            log.info("Waiting for new client connection on port " + publicSideTcpConfigItem.getPublicSidePort());

            //Accept incoming connection; This is blocking command
            Socket socket = serverSocket.accept();
            short currentConnectionId = connectionId++;
            log.info("Incoming connection from " + socket.getRemoteSocketAddress().toString() + " assigned connectionId = " + currentConnectionId);

            //Create new Tx and Rx for this connection
            createTxRxProcessorForEachConnection(currentConnectionId, socket);
        }
    }

    /***
     * Helper method to create Tx/Rx thread and Rx queue for new connection
     * @param currentConnectionId - newly created connection id
     * @param connectionSocket - TCP connection socket of this newly connected client
     * @throws InterruptedException -
     */
    private void createTxRxProcessorForEachConnection(short currentConnectionId, Socket connectionSocket) {

        //Send ConnectMessage to NAT side to tell that new client is connecting
        //If this message failed, the connection will not work properly.
        try {
            txQueue.put(new ConnectMessage(currentConnectionId));
        } catch (Exception e) {
            log.error("Failed to transmit new connection message to NAT side, ignore this connection - " + e.getMessage(), e);
            try {
                connectionSocket.close();
            } catch (Exception ex) {
                //Ignore
            }
            return;
        }

        //Create PublicSideConnection and put to map
        PublicSideConnection publicSideConnection = new PublicSideConnection();
        publicSideConnection.setConnectionSocket(connectionSocket);

        //Create Rx queue for this connection
        publicSideConnection.setRxQueue(new LinkedTransferQueue<>());

        //Create InputStream reader thread for this connection
        //This will read tcp packet from client and send to Tx queue for transmit to NAT side as Data packet
        Thread inputStreamReaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    inputStream = connectionSocket.getInputStream();
                    while(true)
                    {
                        byte[] buffer = new byte[16384];
                        int dataLength = inputStream.read(buffer);
                        if(dataLength < 0)
                        {
                            //End of data, close connection
                            txQueue.put(new DisconnectMessage(currentConnectionId));
                            break;
                        }
                        else
                        {
                            //Create byte array exactly match to actual content length
                            byte[] payload = new byte[dataLength];
                            System.arraycopy(buffer,0, payload,0, dataLength);

                            //Create data message and put to tx queue
                            DataMessage dataMessage = new DataMessage(currentConnectionId, payload);
                            txQueue.put(dataMessage);
                        }
                    }
                }
                catch (SocketException e) {
                    if("Socket closed".equals(e.getMessage()))
                    {
                        //Do nothing
                    }
                    else {
                        try {
                            txQueue.put(new DisconnectMessage(currentConnectionId));
                        } catch (InterruptedException ex) {
                            //Ignore
                        }
                        log.error(e.getMessage(), e);
                    }
                }
                catch (Exception e) {
                    try {
                        txQueue.put(new DisconnectMessage(currentConnectionId));
                    } catch (InterruptedException ex) {
                        //Ignore
                    }
                    log.error(e.getMessage(), e);
                }

                log.info("Tx Thread for connection id " + currentConnectionId + " is closing...");
                cleanupConnection(currentConnectionId);
            }
        });
        publicSideConnection.setTxThread(inputStreamReaderThread);
        inputStreamReaderThread.setName("PublicSideTx-ConId-" + currentConnectionId);


        //Create OutputStream writer thread for this connection
        //This will poll message from Connection's Rx queue and write to client
        Thread outputStreamWriterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream outputStream = connectionSocket.getOutputStream();
                    BlockingQueue<DataMessage> rxQueue = publicSideConnection.getRxQueue();
                    while(true)
                    {
                        DataMessage dataMessage = rxQueue.poll(5, TimeUnit.MILLISECONDS);
                        if(dataMessage != null)
                            outputStream.write(dataMessage.getData());
                    }
                }
                catch (InterruptedException e) {
                    //Ignore
                }
                catch (Exception e) {
                    try {
                        txQueue.put(new DisconnectMessage(currentConnectionId));
                    } catch (InterruptedException ex) {
                        //Ignore
                    }
                    log.error(e.getMessage(), e);
                }

                log.info("Rx Thread for connection id " + currentConnectionId + " is closing...");
                cleanupConnection(currentConnectionId);
            }
        });
        publicSideConnection.setRxThread(outputStreamWriterThread);
        outputStreamWriterThread.setName("PublicSideRx-ConId-" + currentConnectionId);

        //Add connection to map
        connectionMap.put(currentConnectionId, publicSideConnection);

        //Start processing
        inputStreamReaderThread.start();
        outputStreamWriterThread.start();
    }

    /***
     * Dispatcher - It will dispatch message to Rx queue for each connection
     * @param message
     */
    private void dispatchMessage(TunnelMessage message) {
        if (message != null) {
            //TODO: Dispatch based on command
            switch (message.getCommand()) {
                case DATA:
                    DataMessage dataMessage = new DataMessage(message.getPayload());
                    PublicSideConnection publicSideConnection = connectionMap.get(dataMessage.getConnectionId());
                    if (publicSideConnection == null) {
                        log.warn("Connection map for connection id " + dataMessage.getConnectionId() + " not found, discard msg");
                        break;
                    }

                    try {
                        publicSideConnection.getRxQueue().put(dataMessage);
                    } catch (InterruptedException e) {
                        //Ignore
                    }
                    break;
                case DISCONNECT:
                    DisconnectMessage disconnectMessage = new DisconnectMessage(message.getPayload());
                    cleanupConnection(disconnectMessage.getConnectionId());
                    break;
                default:
                    log.warn("Unknown command - " + message.getCommand() + " for dispatcher, discard msg");
                    break;
            }
        }
    }

    /***
     * Start Tx queue poller loop for transmitting to NAT side
     * Message will be transmit to NAT side by calling transmitMessage method in PublicSideTunneler's interface
     * It is Tunneler's responsibility to transmit this message to NAT side
     *
     * Start Rx message poller loop which pull message from NAT side and send to dispatcher
     * Message will be receive by calling receiveMessage method in PublicSideTunneler's interface
     * It is Tunneler's responsibility to implement the logic and return the tunnel message
     */
    private void startTxRxLoop()
    {
        //Start tx q loop
        Thread txThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true)
                {
                    try {
                        TunnelMessage txMessage = txQueue.poll(5, TimeUnit.MILLISECONDS);
                        if(txMessage != null)
                            publicSideTunneler.transmitMessage(txMessage);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                log.error("Main Tx loop exited, Future message will not get processed, consider restart entire server");
            }
        });
        txThread.setName("PublicSideTx-Main");
        txThread.start();

        //Start rx q loop
        Thread rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        TunnelMessage rxMessage = publicSideTunneler.receiveMessage();
                        if (rxMessage != null)
                            dispatchMessage(rxMessage);
                    } catch (Exception e) {
                        break;
                    }
                }
                log.error("Main Rx loop exited, Future message will not get processed, consider restart entire server");
            }
        });
        rxThread.setName("PublicSideRx-Main");
        rxThread.start();
    }

    /***
     * Clean up connection once client disconnected
     * This will stop Tx and Rx thread and clean up remaining item in rx queue of this connection
     * @param connectionId Connection ID which was assigned during connection setup
     */
    private void cleanupConnection(short connectionId)
    {
        PublicSideConnection publicSideConnection = connectionMap.get(connectionId);
        if(publicSideConnection == null)
            return;


        //Close Tx thread
        try {
            publicSideConnection.getTxThread().interrupt();
        }
        catch (Exception ex)
        {
            //Ignore
        }

        //Close Rx thread
        try {
            publicSideConnection.getRxThread().interrupt();
        }
        catch (Exception ex)
        {
            //Ignore
        }

        //Clear queue
        try {
            publicSideConnection.getRxQueue().clear();
        }
        catch (Exception ex)
        {
            //Ignore
        }

        //Close client connection
        try {
            publicSideConnection.getConnectionSocket().close();
        } catch (Exception ex) {
            //Ignore
        }

        //Remove from queue
        connectionMap.put(connectionId, null);

    }
}
