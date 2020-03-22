package th.in.meen.natmeout.util;

import org.junit.Test;



public class PacketUtilTest {

    @Test
    public void testCovertShort1()
    {
        short originalValue = 24832;
        byte[] bytes = PacketUtil.convertFromShortToBytes(originalValue);
        Short result = PacketUtil.convertFromBytesToShort(bytes);
        if(result != originalValue)
            throw new RuntimeException("Value not matched");
    }
}
