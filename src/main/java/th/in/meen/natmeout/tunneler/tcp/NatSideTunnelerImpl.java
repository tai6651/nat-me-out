package th.in.meen.natmeout.tunneler.tcp;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import th.in.meen.natmeout.model.exception.TunnelerException;
import th.in.meen.natmeout.model.message.*;
import th.in.meen.natmeout.tunneler.NatSideTunneler;
import th.in.meen.natmeout.util.PacketUtil;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NatSideTunnelerImpl implements NatSideTunneler {

    private static final Logger log = LoggerFactory.getLogger(NatSideTunnelerImpl.class);
    private boolean isReady = false;

    private InputStream inputStreamFromPublicSide;
    private OutputStream outputStreamToPublicSide;

    Map<Short, Socket> destinationConnectionMap;
    Map<Short, Thread> destinationRxThreadMap;

    private String natSideDestinationHost;
    private Integer natSideDestinationPort;

    private BlockingQueue<TunnelMessage> txQueue;

    @Override
    public void initialize(String natSideDestinationHost, Integer natSideDestinationPort, Map<String, Object> configuration) throws TunnelerException {
        destinationConnectionMap = new HashMap<>();
        destinationRxThreadMap = new HashMap<>();
        txQueue = new LinkedBlockingQueue<>();

        this.natSideDestinationHost = natSideDestinationHost;
        this.natSideDestinationPort = natSideDestinationPort;

        Thread mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                initConnection(configuration);
            }
        });
        mainThread.setName("NatSideTunneler-Main");
        mainThread.start();
    }

    @Override
    public void transmitMessage(TunnelMessage tunnelMessage) {
        while(!isReady)
        {
            sleep(50);
        }

        byte[] dataToSend = PacketUtil.generateBytesToSend(tunnelMessage);
        log.debug("Transmitting message type " + tunnelMessage.getCommand() + " - " + DatatypeConverter.printHexBinary(dataToSend));

        //Send to public Side
        try {
            outputStreamToPublicSide.write(dataToSend);
        } catch (IOException e) {
            log.error(e.getMessage(), e);

            //Mark as failure
            isReady = false;
        }
    }

    @Override
    public TunnelMessage receiveMessage() {
        while(!isReady)
        {
            sleep(50);
        }

        //Receive from NAT side
        try {
            return PacketUtil.readMessageFromInputStream(inputStreamFromPublicSide);
        } catch (IOException e) {
            isReady = false;
            log.error(e.getMessage(), e);
        }

        return null;
    }

    @Override
    public void handleConnectMessage(ConnectMessage connectMessage) {
        Short connectionId = connectMessage.getConnectionId();
        log.info("Going to create new connection for " + connectionId + " to " + natSideDestinationHost + " port " + natSideDestinationPort);
        try {
            Socket socket = new Socket(natSideDestinationHost, natSideDestinationPort);
            destinationConnectionMap.put(connectionId, socket);

            //Create new receiver thread
            Thread rxThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        InputStream  inputStream = socket.getInputStream();
                        while(true)
                        {
                            byte[] buffer = new byte[4096];
                            int dataLength = inputStream.read(buffer);
                            if(dataLength < 0)
                            {
                                txQueue.put(new DisconnectMessage(connectionId));
                                break;
                            }
                            else
                            {
                                //Create byte array exactly match to actual content length
                                byte[] payload = new byte[dataLength];
                                System.arraycopy(buffer,0, payload,0, dataLength);
                                DataMessage dataMessage = new DataMessage(connectionId, payload);
                                txQueue.put(dataMessage);
                            }
                        }
                    } catch (Exception e) {
                        try {
                            txQueue.put(new DisconnectMessage(connectionId));
                        } catch (InterruptedException ex) {
                            log.warn(ex.getMessage(), ex);
                        }
                        log.error(e.getMessage(), e);
                    }
                }
            });
            rxThread.setName("NatSideDestinationRx-"+connectionId);
            rxThread.start();
            destinationRxThreadMap.put(connectionId, rxThread);
        }catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void handleDisconnectMessage(DisconnectMessage disconnectMessage) {
        Short conenctionId = disconnectMessage.getConnectionId();
        log.info("Going to disconnect connection id " + conenctionId);
        Socket socket = destinationConnectionMap.get(conenctionId);

        //Disconnect
        if(socket != null)
        {
            try {
                socket.close();
            } catch (Exception e) {
                //Do nothing
            }

            //Remove from connection map
            destinationConnectionMap.put(conenctionId, null);


        }

        //Stop receiver thread
        Thread rxThread = destinationRxThreadMap.get(conenctionId);
        if(rxThread != null)
        {
            try {
                rxThread.interrupt();
            }catch (Exception ex)
            {
                //Do nothing
            }

            destinationRxThreadMap.put(conenctionId, null);
        }
    }

    @Override
    public void handleDataMessage(DataMessage dataMessage) {
        log.debug("Handle data for connection id " + dataMessage.getConnectionId());
        Socket socket = destinationConnectionMap.get(dataMessage.getConnectionId());
        if(socket != null)
        {
            try {
                socket.getOutputStream().write(dataMessage.getData());
            } catch (IOException e) {
                log.error("Failed to write packet to destination for connection Id " + dataMessage.getConnectionId() + " - " + e.getMessage(), e);
            }
        }
        else
            log.error("Failed to write packet to destination for connection Id " + dataMessage.getConnectionId() + " - connection map not found");

    }

    @Override
    public TunnelMessage pollMessageFromTxQueue() {
        try {
            return txQueue.poll(50, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public void initConnection(Map<String, Object> configuration)
    {
        String tunnelHost = (String) configuration.get("tunnelHost");
        Integer tunnelPort = (Integer) configuration.get("tunnelPort");

        while(true)
        {
            try
            {
                log.info("Connecting to public side on " + tunnelHost + " port " + tunnelPort);
                Socket clientSocket = new Socket(tunnelHost, tunnelPort);
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();

                TunnelMessage tunnelMessage = new HelloMessage((String)configuration.get("secret"));
                byte[] dataToSend = PacketUtil.generateBytesToSend(tunnelMessage);
                log.info("Authenticating...");
                outputStream.write(dataToSend);
                TunnelMessage helloResponseMessage = PacketUtil.readMessageFromInputStream(inputStream);
                if(helloResponseMessage instanceof AuthSuccessMessage)
                {
                    AuthSuccessMessage authSuccessMessage = (AuthSuccessMessage) helloResponseMessage;
                    log.info("Authentication success - " + authSuccessMessage.getMessage());


                    outputStreamToPublicSide = outputStream;
                    inputStreamFromPublicSide = inputStream;

                    isReady = true;

                    //Block until someone change state to not ready
                    while(isReady)
                        sleep(1000);
                }
                else
                {
                    if(helloResponseMessage instanceof AuthFailureMessage) {
                        log.error("Authentication failed - " + ((AuthFailureMessage)helloResponseMessage).getMessage());
                    }
                    else
                    {
                        log.error("Authentication failed - unknown reason");
                    }
                    //Sleep 10 second before retry
                    sleep(10000);
                }

            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);

                //Sleep 5 second before retry
                sleep(5000);
            }
        }
    }

    private static void sleep(long millis)
    {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //Nothing
        }
    }
}
