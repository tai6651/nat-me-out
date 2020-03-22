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
                    log.info("Authenticating...");
                    InputStream inputStream = socket.getInputStream();
                    OutputStream outputStream = socket.getOutputStream();

                    //Read 1st Tunnel Message
                    TunnelMessage tunnelMessage = readMessage(inputStream);

                    //Should be hello message
                    if(tunnelMessage instanceof HelloMessage) {
                        //Verify secret
                        HelloMessage helloMessage = (HelloMessage) tunnelMessage;
                        if (configuration.get("secret").toString().equals(helloMessage.getSecret())) {
                            AuthSuccessMessage message = new AuthSuccessMessage("Auth success, I'm server");
                            byte[] dataToSend = generateBytesToSend(message);
                            outputStream.write(dataToSend);
                            log.info("Authentication success");

                            inputStreamFromNatSide = inputStream;
                            outputStreamToNatSide = outputStream;

                            //Mark current state as ready
                            isReady = true;
                        }
                    }
                    else {
                        //Close
                        log.info("Invalid message, Disconnect!");
                        socket.close();
                    }
                }catch (Exception ex)
                {
                    log.error(ex.getMessage(), ex);
                }
            }
        } catch (IOException e) {
            throw new TunnelerException(e);
        }
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

        byte[] dataToSend = generateBytesToSend(tunnelMessage);
        log.debug("Transmitting message type " + tunnelMessage.getCommand() + " - " + DatatypeConverter.printHexBinary(dataToSend));

        //Send to NAT Side
        try {
            outputStreamToNatSide.write(dataToSend);
        } catch (IOException e) {
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
            return readMessage(inputStreamFromNatSide);
        } catch (IOException e) {
            isReady = false;
            log.error(e.getMessage(), e);
        }

        return null;
        //return new DataMessage((short)0, "HELLO FROM Another side".getBytes());
    }

    public static TunnelMessage readMessage(InputStream inputStream) throws IOException {
        //Read first 2 byte for message length
        byte[] messageLengthByte = new byte[2];
        inputStream.read(messageLengthByte);
        short msgLength = PacketUtil.convertFromBytesToShort(messageLengthByte);
        byte[] payload = new byte[msgLength];
        inputStream.read(payload);
        TunnelMessage.COMMAND command = PacketUtil.convertFromByteToCommand(payload[0]);
        byte[] data = new byte[msgLength - 1];
        System.arraycopy(payload, 1, data, 0, msgLength - 1);
        switch(command)
        {
            case AUTH_SUCCESS:
                return new AuthSuccessMessage(data);
            case HELLO:
                return new HelloMessage(data);
            case DATA:
                return new DataMessage(data);
            case CONNECT:
                return new ConnectMessage(data);
            case DISCONNECT:
                return new DisconnectMessage(data);
            default:
                return null;
        }
    }

    public static byte[] generateBytesToSend(TunnelMessage tunnelMessage)
    {
        //Message structure is
        // MSG_LENGTH | COMMAND_MODE | PAYLOAD
        // MSG_LENGTH = LENGTH(COMMAND_MODE | PAYLOAD)
        // LENGTH(COMMAND_MODE) = 1
        // LENGTH(PAYLOAD) = payload.length
        // LENGTH(MSG_LENGTH) = 2
        byte[] payload = tunnelMessage.getPayload();
        short messageLength = (short) (payload.length+1);
        byte[] messageLengthByes = PacketUtil.convertFromShortToBytes(messageLength);

        byte[] dataToSend = new byte[payload.length+3];
        System.arraycopy(messageLengthByes, 0, dataToSend, 0, 2);
        dataToSend[2] = PacketUtil.convertFromCommandToByte(tunnelMessage.getCommand());
        System.arraycopy(payload, 0, dataToSend, 3, payload.length);

        return dataToSend;
    }
}
