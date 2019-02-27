package com.wanfajie.net.sudp;

import com.wanfajie.net.sudp.handler.*;
import com.wanfajie.net.sudp.handler.codec.SUdpCodec;
import com.wanfajie.net.sudp.handler.filter.OneAddressFilterHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

public class SUdpInitializer extends ChannelInitializer<DatagramChannel> {

    public static final AttributeKey<InetSocketAddress> CONNECT_TO = AttributeKey.valueOf("connect_to");

    @Override
    protected void initChannel(DatagramChannel ch) {
        SUdpCodec codec = new SUdpCodec();
        InvalidPacketHandler invalidHandler = new InvalidPacketHandler();
        ReplyHandler replyHandler = new ReplyHandler();
        ReplayHandler sendHandler = new ReplayHandler();

        ch.pipeline().addLast(SUdpCodec.NAME, codec)
            .addAfter(SUdpCodec.NAME, InvalidPacketHandler.NAME, invalidHandler)
            .addAfter(InvalidPacketHandler.NAME, ReplyHandler.NAME, replyHandler)
            .addAfter(ReplyHandler.NAME, ReplayHandler.NAME, sendHandler);

        ch.pipeline()
            .addAfter(ReplayHandler.NAME, SendSegmentHandler.NAME, new SendSegmentHandler())
            .addAfter(SendSegmentHandler.NAME, AssembleSegmentHandler.NAME, new AssembleSegmentHandler());

        InetSocketAddress address = ch.attr(CONNECT_TO).get();
        if (address != null) {
            ch.pipeline().addBefore(SUdpCodec.NAME, "address_filter", new OneAddressFilterHandler(address));
        }

        ChannelHandlerContext preCodecContext = ch.pipeline().context(ReplayHandler.NAME);

        invalidHandler.setDirectOutboundContext(preCodecContext);
        replyHandler.setDirectOutboundContext(preCodecContext);
    }
}
