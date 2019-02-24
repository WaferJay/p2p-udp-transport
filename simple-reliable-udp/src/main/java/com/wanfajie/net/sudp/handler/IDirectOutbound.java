package com.wanfajie.net.sudp.handler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

public interface IDirectOutbound {
    void setDirectOutboundContext(ChannelHandlerContext ctx);
    ChannelFuture sendDirectly(Object msg);
}
