import com.wanfajie.net.sudp.handler.codec.exception.CRCMismatchException;
import com.wanfajie.net.sudp.handler.codec.exception.TooLongPacketException;
import com.wanfajie.net.sudp.handler.InvalidPacketHandler;
import com.wanfajie.net.sudp.packet.DataPacket;
import com.wanfajie.net.sudp.packet.ReplyPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Random;

import static com.wanfajie.net.sudp.packet.ReplyPacket.ReplyType;
import static io.netty.channel.ChannelHandler.Sharable;
import static org.junit.Assert.*;

public class InvalidPacketHandlerTest {

    @Sharable
    private static final class RaiseExceptionHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            throw (DecoderException) msg;
        }
    }

    private static final ChannelInboundHandler raiseExceptionHandler = new RaiseExceptionHandler();
    private static final Random random = new Random();

    @Test
    public void testCRCMismatchException() {
        int sequence = random.nextInt();
        InetSocketAddress sender = randomSender();

        EmbeddedChannel channel = setupChannel();

        assertFalse(channel.writeInbound(new CRCMismatchException(sender, sequence, DataPacket.class)));
        assertTrue(channel.finish());

        ReplyPacket reply = channel.readOutbound();
        checkReply(reply, sender, sequence, ReplyType.REPLAY);
    }

    @Test
    public void testTooLongPacketException() {

        int sequence = random.nextInt();
        int contentLength = random.nextInt();
        InetSocketAddress sender = randomSender();

        EmbeddedChannel channel = setupChannel();

        assertFalse(channel.writeInbound(new TooLongPacketException(sender, sequence, contentLength)));
        assertTrue(channel.finish());

        ReplyPacket reply = channel.readOutbound();
        checkReply(reply, sender, sequence, ReplyType.REJECT);

        channel.finish();
    }

    @Test
    public void testOtherException() {
        InvalidPacketHandler invalidHandler = new InvalidPacketHandler();
        EmbeddedChannel channel = new EmbeddedChannel(raiseExceptionHandler, invalidHandler);

        try {
            assertFalse(channel.writeInbound(new DecoderException()));
            fail();
        } catch (DecoderException ignored) {
        } finally {
            assertFalse(channel.finish());
        }
    }

    @Test
    public void testNoException() {
        EmbeddedChannel channel = new EmbeddedChannel(new InvalidPacketHandler());
        Object message = new Object();

        assertTrue(channel.writeInbound(message));

        assertTrue(channel.finish());
        assertSame(channel.readInbound(), message);
        assertNull(channel.readOutbound());
    }

    private void checkReply(ReplyPacket reply, InetSocketAddress sender, int sequence, ReplyType type) {
        assertNotNull(reply);
        assertEquals(reply.recipient(), sender);
        assertEquals(reply.sequence(), sequence);
        assertEquals(reply.getType(), type);
    }

    private EmbeddedChannel setupChannel() {
        InvalidPacketHandler invalidHandler = new InvalidPacketHandler();
        EmbeddedChannel channel = new EmbeddedChannel(raiseExceptionHandler, invalidHandler);

        ChannelHandlerContext context = channel.pipeline().context(invalidHandler);
        invalidHandler.setDirectOutboundContext(context);
        return channel;
    }

    private InetSocketAddress randomSender() {
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
