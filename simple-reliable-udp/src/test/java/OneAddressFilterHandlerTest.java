import com.wanfajie.net.sudp.handler.filter.OneAddressFilterHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.*;

public class OneAddressFilterHandlerTest {

    private static final String DEFAULT_HOST = "192.168.1.1";
    private static final int DEFAULT_PORT = 9999;
    private static final InetSocketAddress DEFAULT_ADDRESS = new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT);

    private OneAddressFilterHandler handler;
    private EmbeddedChannel channel;

    @Before
    public void setupChannel() {
        handler = new OneAddressFilterHandler(DEFAULT_ADDRESS);
        channel = new EmbeddedChannel(handler);
    }

    @After
    public void finishChannel() {
        channel.finishAndReleaseAll();
        channel = null;
        handler = null;
    }

    @Test
    public void testFilterMethod() {
        assertTrue(handler.filter(DEFAULT_ADDRESS));
        assertTrue(handler.filter(new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT)));
        assertFalse(handler.filter(new InetSocketAddress(DEFAULT_HOST, 1234)));
        assertFalse(handler.filter(new InetSocketAddress("127.0.0.1", 9999)));
    }

    @Test
    public void testMatch() {
        ByteBuf content = channel.alloc().buffer(0);
        DatagramPacket packet = new DatagramPacket(content, new InetSocketAddress(2222), DEFAULT_ADDRESS);
        assertTrue(channel.writeInbound(packet));
        assertEquals(channel.readInbound(), packet);
        assertEquals(content.refCnt(), 1);

        content.release();
    }

    @Test
    public void testMismatch() {
        InetSocketAddress sender = new InetSocketAddress(3333);
        InetSocketAddress recipient = new InetSocketAddress(2222);

        ByteBuf content = channel.alloc().buffer(0);
        DatagramPacket packet = new DatagramPacket(content, recipient, sender);

        assertFalse(channel.writeInbound(packet));
        assertFalse(channel.finish());

        assertEquals(content.refCnt(), 0);
    }

    @Test
    public void testAddressProperty() {
        assertEquals(handler.getAddress(), DEFAULT_ADDRESS);
        InetSocketAddress recipient = new InetSocketAddress(4444);
        ByteBuf content = channel.alloc().buffer(0);

        InetSocketAddress newAddress = new InetSocketAddress(1234);

        content.retain();
        assertEquals(content.refCnt(), 2);
        DatagramPacket packet = new DatagramPacket(content, recipient, newAddress);
        assertFalse(channel.writeInbound(packet));
        assertEquals(content.refCnt(), 1);

        handler.setAddress(newAddress);
        assertEquals(handler.getAddress(), newAddress);

        assertTrue(channel.writeInbound(packet));
        assertEquals(packet, channel.readInbound());
        assertEquals(content.refCnt(), 1);

        packet.release();
    }
}
