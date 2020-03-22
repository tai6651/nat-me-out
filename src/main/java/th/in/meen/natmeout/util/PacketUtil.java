package th.in.meen.natmeout.util;

import th.in.meen.natmeout.model.message.TunnelMessage;

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
            default:
                throw new RuntimeException("Unknown command - " + command);
        }
    }
}
