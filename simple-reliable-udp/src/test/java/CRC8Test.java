import com.wanfajie.net.sudp.CRCUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class CRC8Test {

    @Test
    public void testBytesCRC8() {
        byte crcTest = 0x4c;
        byte crcHelloWorld = 0x66;
        byte crcHello = 0x13;

        String dataTest = "test";
        String dataHello = "hello, world.";

        byte[] bytesTest = dataTest.getBytes();
        byte[] bytesHello = dataHello.getBytes();

        assertEquals(CRCUtil.crc8(bytesTest), crcTest);
        assertEquals(CRCUtil.crc8(bytesHello), crcHelloWorld);

        assertEquals(CRCUtil.crc8(bytesHello, 0, 5), crcHello);
        assertEquals(CRCUtil.crc8(bytesHello, 5, dataHello.length() - 5, crcHello), crcHelloWorld);
    }

    @Test
    public void testByteBufCRC8() {
        byte crcHello = (byte) 0xeb;
        byte crcHelloWorld = (byte) 0xdd;

        String dataTest = "test";
        String dataHello = "Hello, World.";

        byte[] bytesTest = dataTest.getBytes();
        byte[] bytesHello = dataHello.getBytes();

        ByteBuf bufTest = Unpooled.copiedBuffer(bytesTest);
        ByteBuf bufHello = Unpooled.copiedBuffer(bytesHello);

        assertEquals(CRCUtil.crc8(bytesTest), CRCUtil.crc8(bufTest));
        assertEquals(CRCUtil.crc8(bytesHello), CRCUtil.crc8(bufHello), crcHelloWorld);

        assertEquals(CRCUtil.crc8(bufHello, 0, 5), crcHello);
        assertEquals(CRCUtil.crc8(bufHello, 5, bufHello.writerIndex() - 5, crcHello), crcHelloWorld);

        assertEquals(bufTest.readerIndex(), 0);
        assertEquals(bufHello.readerIndex(), 0);
        assertEquals(bufTest.writerIndex(), bytesTest.length);
        assertEquals(bufHello.writerIndex(), bytesHello.length);

        bufTest.release();
        bufHello.release();
    }
}
