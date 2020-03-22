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

public class PublicSideTunnelerImpl implements PublicSideTunneler {

    private static final Logger log = LoggerFactory.getLogger(PublicSideTunnelerImpl.class);
    private boolean isReady = false;

    private InputStream inputStreamFromNatSide;
    private OutputStream outputStreamToNatSide;

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

    public void initConnection(Map<String, Object> configuration)
    {

        Integer tunnelPort = (Integer) configuration.get("tunnelPort");
        try {
            ServerSocket serverSocket = new ServerSocket(tunnelPort);
            log.info("TCP Tunneler is listen for NAT side at port " + tunnelPort);
            while(true)
            {
                try {
                    Socket socket = serverSocket.accept();
                    log.info("Incoming connection from NAT side address " + socket.getRemoteSocketAddress().toString());
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

    public void handleNewNATSideConnection(Socket socket, String secret) throws IOException {
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
                AuthSuccessMessage message = new AuthSuccessMessage("Auth success, I'm server");
                byte[] dataToSend = PacketUtil.generateBytesToSend(message);
                outputStream.write(dataToSend);
                log.info("Authentication success");

                inputStreamFromNatSide = inputStream;
                outputStreamToNatSide = outputStream;

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

        while(!isReady)
        {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                //Nothing
            }
        }

        byte[] dataToSend = PacketUtil.generateBytesToSend(tunnelMessage);
        log.debug("Transmitting message type " + tunnelMessage.getCommand() + " - " + DatatypeConverter.printHexBinary(dataToSend));

        //Send to NAT Side
        try {
            outputStreamToNatSide.write(dataToSend);
        } catch (IOException e) {
            isReady = false;
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public TunnelMessage receiveMessage() {
        while(!isReady)
        {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                //Nothing
            }
        }

        //Receive from NAT side
        try {
            return PacketUtil.readMessageFromInputStream(inputStreamFromNatSide);
        } catch (IOException e) {
            isReady = false;
            log.error(e.getMessage(), e);
        }

        return null;
        //return new DataMessage((short)0, "HELLO FROM Another side".getBytes());
    }


}
