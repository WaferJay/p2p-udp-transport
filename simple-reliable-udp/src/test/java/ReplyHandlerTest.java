import com.wanfajie.net.sudp.Config;
import com.wanfajie.net.sudp.handler.ReplyHandler;
import com.wanfajie.net.sudp.packet.DataPacket;
import com.wanfajie.net.sudp.packet.ReplyPacket;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Random;

import static org.junit.Assert.*;

public class ReplyHandlerTest {

    private EmbeddedChannel channel;

    private static final Random random = new Random();

    @Test
    public void testMaxReplayCountArgument() {
        new ReplyHandler(0);

        try {
            new ReplyHandler(-1);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Before
    public void setupReplyHandler() {
        ReplyHandler handler = new ReplyHandler();
        channel = new EmbeddedChannel(handler);
        handler.setDirectOutboundContext(channel.pipeline().context(handler));
    }

    @After
    public void finishChannel() {
        channel.finishAndReleaseAll();
    }

    @Test
    public void testOtherMessage() {
        Object message = new Object();
        assertTrue(channel.writeInbound(message));
        assertTrue(channel.finish());
        assertSame(channel.readInbound(), message);
    }

    @Test
    public void testReplayOverMaxMessage() {
        byte maxReplayCount = (byte) Config.MAX_REPLAY_COUNT;

        InetSocketAddress sender = randomAddress();
        InetSocketAddress recipient = randomAddress();
        int sequence = random.nextInt();

        DataPacket packet = new DataPacket(sender,
                recipient,
                sequence,
                (byte) 0x00,
                (byte) (maxReplayCount+1),
                Unpooled.EMPTY_BUFFER,
                0).flagLast();

        assertFalse(channel.writeInbound(packet));

        ReplyPacket reply = channel.readOutbound();
        assertNotNull(reply);
        assertEquals(reply.recipient(), sender);
        assertEquals(reply.getType(), ReplyPacket.ReplyType.REJECT);
    }

    @Test
    public void testReplayMessage() {
        InetSocketAddress sender = randomAddress();
        InetSocketAddress recipient = randomAddress();
        int sequence = random.nextInt();

        DataPacket packet = new DataPacket(sender, recipient, sequence, (byte) 0x00, (byte) 0, Unpooled.EMPTY_BUFFER, 0);

        assertTrue(channel.writeInbound(packet));
        DatagramPacket udpPacket = channel.readInbound();
        assertNotNull(udpPacket);

        ReplyPacket reply = channel.readOutbound();
        assertNotNull(reply);
        assertEquals(reply.recipient(), sender);
        assertEquals(reply.getType(), ReplyPacket.ReplyType.RECEIVED);


        packet.incrReplayCount();
        assertFalse(channel.writeInbound(packet));

        reply = channel.readOutbound();
        assertNotNull(reply);
        assertEquals(reply.recipient(), sender);
        assertEquals(reply.getType(), ReplyPacket.ReplyType.RECEIVED);
    }

    private InetSocketAddress randomAddress() {
        int ip = random.nextInt();
        String senderHost = String.join(
                String.valueOf(ip & 0xff),
                String.valueOf(ip >> 8 & 0xff),
                String.valueOf(ip >> 16 & 0xff),
                String.valueOf(ip >> 24)
        );

        int senderPort = random.nextInt() & 0xffff;

        return new InetSocketAddress(senderHost, senderPort);
    }
}
