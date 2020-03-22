package th.in.meen.natmeout.util;

import th.in.meen.natmeout.model.message.*;

import java.io.IOException;
import java.io.InputStream;

public class PacketUtil {
    public static byte[] convertFromShortToBytes(short data)
    {
        byte[] result = new byte[2];
        result[0] = (byte) ((data & 0x0000FF00) >> 8);
        result[1] = (byte) ((data & 0x000000FF) >> 0);
        return result;
    }

    public static short convertFromBytesToShort(byte[] data)
    {
       return (short) (((data[0] & 0xFF) << 8 ) |
                       ((data[1] & 0xFF) << 0 ));
    }

    public static byte convertFromCommandToByte(TunnelMessage.COMMAND command)
    {
        switch (command)
        {
            case HELLO:
                return 0;
            case CONNECT:
                return 1;
            case DATA:
                return 2;
            case DISCONNECT:
                return 3;
            case AUTH_SUCCESS:
                return 4;
            case AUTH_FAILURE:
                return 5;
            default:
                throw new RuntimeException("Unknown command - " + command);
        }
    }

    public static TunnelMessage.COMMAND convertFromByteToCommand(byte commandByte)
    {
        switch(commandByte)
        {
            case 0:
                return TunnelMessage.COMMAND.HELLO;
            case 1:
                return TunnelMessage.COMMAND.CONNECT;
            case 2:
                return TunnelMessage.COMMAND.DATA;
            case 3:
                return TunnelMessage.COMMAND.DISCONNECT;
            case 4:
                return TunnelMessage.COMMAND.AUTH_SUCCESS;
            case 5:
                return TunnelMessage.COMMAND.AUTH_FAILURE;
            default:
                throw new RuntimeException("Unknown command byte - " + commandByte);
        }
    }


    public static TunnelMessage readMessageFromInputStream(InputStream inputStream) throws IOException {
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
