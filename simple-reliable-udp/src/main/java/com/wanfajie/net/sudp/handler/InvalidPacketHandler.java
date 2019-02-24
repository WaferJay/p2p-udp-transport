package com.wanfajie.net.sudp.handler;

import com.wanfajie.net.sudp.handler.codec.exception.CRCMismatchException;
import com.wanfajie.net.sudp.handler.codec.exception.TooLongPacketException;
import com.wanfajie.net.sudp.packet.ReplyPacket;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetSocketAddress;

public class InvalidPacketHandler extends ChannelInboundHandlerAdapter implements IDirectOutbound {

    public static final String NAME = "s_udp_catch_invalid";

    private ChannelHandlerContext senderContext;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        if (cause instanceof TooLongPacketException) {
            TooLongPacketException tooLong = (TooLongPacketException) cause;

            InetSocketAddress sender = tooLong.sender();
            int sequence = tooLong.sequence();

            sendDirectly(new ReplyPacket(sender, sequence, ReplyPacket.ReplyType.REJECT));
        } else if (cause instanceof CRCMismatchException) {
            CRCMismatchException mismatch = (CRCMismatchException) cause;

            InetSocketAddress sender = mismatch.sender();
            int sequence = mismatch.sequence();

            sendDirectly(new ReplyPacket(sender, sequence, ReplyPacket.ReplyType.REPLAY));
        } else {
            ctx.fireExceptionCaught(cause);
        }
    }

    @Override
    public void setDirectOutboundContext(ChannelHandlerContext ctx) {
        senderContext = ctx;
    }

    @Override
    public ChannelFuture sendDirectly(Object msg) {
        return senderContext.writeAndFlush(msg);
    }
}
