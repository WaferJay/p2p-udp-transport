package com.wanfajie.net.sudp.handler.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

public abstract class UdpAddressFilterHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        DatagramPacket packet = (DatagramPacket) msg;
        InetSocketAddress sender = packet.sender();

        if (filter(sender)) {
            ctx.fireChannelRead(packet);
        } else {
            packet.release();
        }
    }

    public abstract boolean filter(InetSocketAddress sender);
}
