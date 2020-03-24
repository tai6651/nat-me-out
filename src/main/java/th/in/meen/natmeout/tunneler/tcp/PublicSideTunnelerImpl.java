package th.in.meen.natmeout.tunneler.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import th.in.meen.natmeout.model.exception.TunnelerException;
import th.in.meen.natmeout.model.message.*;
import th.in.meen.natmeout.tunneler.PublicSideTunneler;
import th.in.meen.natmeout.util.PacketUtil;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

/***
 * Sample implementation for Tunneler - This is simple TCP tunneler
 *
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public class PublicSideTunnelerImpl implements PublicSideTunneler {

    private static final Logger log = LoggerFactory.getLogger(PublicSideTunnelerImpl.class);
    private boolean isReady = false;

    private Socket currentTunnelSocket;

    @Override
    public void initialize(Map<String, Object> configuration) throws TunnelerException {
        Thread mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                initConnection(configuration);
            }
        });
        mainThread.setName("PublicSideTunneler-Main");
        mainThread.start();
    }

    private void initConnection(Map<String, Object> configuration)
    {

        Integer tunnelPort = (Integer) configuration.get("tunnelPort");
        try {
            ServerSocket serverSocket = new ServerSocket(tunnelPort);
            log.info("TCP Tunneler is listen for NAT side at port " + tunnelPort);
            int natSideCounter = 0;
            while(true)
            {
                try {
                    Socket socket = serverSocket.accept();
                    int natSideConnectionId = natSideCounter++;
                    log.info("Incoming connection from NAT side address " + socket.getRemoteSocketAddress().toString() + "; Assigned as NAT client id " + natSideConnectionId);

                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                handleNewNATSideConnection(socket, configuration.get("secret").toString());
                            } catch (IOException e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    });
                    t.setName("TcpTunnel-NatId-"+natSideConnectionId);
                    t.start();

                }catch (Exception ex)
                {
                    log.error(ex.getMessage(), ex);
                }
            }
        } catch (IOException e) {
            throw new TunnelerException(e);
        }
    }

    private void handleNewNATSideConnection(Socket socket, String secret) throws IOException {
        log.info("Authenticating...");
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        //Read 1st Tunnel Message
        TunnelMessage tunnelMessage = PacketUtil.readMessageFromInputStream(inputStream);

        //Should be hello message
        if(tunnelMessage instanceof HelloMessage) {
            //Verify secret
            HelloMessage helloMessage = (HelloMessage) tunnelMessage;
            if (secret.equals(helloMessage.getSecret())) {
                AuthSuccessMessage message = new AuthSuccessMessage("Auth success, Ready to continue");
                byte[] dataToSend = PacketUtil.generateBytesToSend(message);
                outputStream.write(dataToSend);
                log.info("Authentication success");

                currentTunnelSocket = socket;

                //Mark current state as ready
                isReady = true;
            }
            else
            {
                AuthFailureMessage message = new AuthFailureMessage("Incorrect secret");
                byte[] dataToSend = PacketUtil.generateBytesToSend(message);
                outputStream.write(dataToSend);
                log.info("Authentication failure - Invalid secret");
                socket.close();
            }
        }
        else {
            //Close
            log.info("Invalid message, Disconnect!");
            socket.close();
        }

        //////////////////////////
    }

    @Override
    public void transmitMessage(TunnelMessage tunnelMessage) {
        boolean isSuccess = false;
        byte[] dataToSend = PacketUtil.generateBytesToSend(tunnelMessage);

        for(int i = 0; i < 3; i++) {
            //Wait for connection to be ready
            while (!isReady) {
                sleep(50);
            }

            Socket currentConnection = currentTunnelSocket;
            if(log.isDebugEnabled())
                log.debug("Transmitting message type " + tunnelMessage.getCommand() + " - " + DatatypeConverter.printHexBinary(dataToSend));

            try {
                //Send to NAT Side
                currentConnection.getOutputStream().write(dataToSend);
                isSuccess = true;
                break;
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                markCurrentTunnelConnectionAsInvalid(currentConnection);
            }
        }

        if(!isSuccess)
            log.error("Failed to transmit message after 3 retry(s)");
    }

    @Override
    public TunnelMessage receiveMessage() {
        while (!isReady) {
            sleep(50);
        }

        Socket currentConnection = currentTunnelSocket;
        //Receive from NAT side
        try {
            return PacketUtil.readMessageFromInputStream(currentConnection.getInputStream());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            markCurrentTunnelConnectionAsInvalid(currentConnection);
        }


        return null;
    }

    /***
     * Mark current tunnel connection as invalid
     * @param currentTunnelSocket
     */
    private void markCurrentTunnelConnectionAsInvalid(Socket currentTunnelSocket)
    {
        isReady = false;
        try {
            currentTunnelSocket.close();
        } catch (Exception e) {
            //Ignore
        }
    }

    private static void sleep(long millis)
    {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            //Nothing
        }
    }
}
