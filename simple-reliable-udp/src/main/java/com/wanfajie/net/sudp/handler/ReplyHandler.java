package com.wanfajie.net.sudp.handler;

import com.wanfajie.net.sudp.packet.DataPacket;
import com.wanfajie.net.sudp.packet.ReplyPacket;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class ReplyHandler extends ChannelInboundHandlerAdapter implements IDirectOutbound {

    public static final String NAME = "s_udp_reply";

    private final int maxReplayCount;

    private Map<Integer, Set<Integer>> table = new HashMap<>();
    private Function<Integer, Set<Integer>> newSetFunc = k -> new HashSet<>();
    private ChannelHandlerContext preCodecContext;

    public ReplyHandler() {
        this(Config.MAX_REPLAY_COUNT);
    }

    public ReplyHandler(int maxReplayCount) {
        if (maxReplayCount < 0)
            throw new IllegalArgumentException(maxReplayCount + " <= 0");

        this.maxReplayCount = maxReplayCount;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        if (!(msg instanceof DataPacket)) {
            ctx.fireChannelRead(msg);
            return;
        }

        DataPacket packet = (DataPacket) msg;
        int seq = packet.sequence();
        InetSocketAddress sender = packet.sender();

        int addrHashCode = sender.hashCode();
        Set<Integer> set = table.computeIfAbsent(addrHashCode, newSetFunc);

        ReplyPacket reply;
        if (packet.replayCount() > maxReplayCount) {
            reply = new ReplyPacket(sender, seq, ReplyPacket.ReplyType.REJECT);
        } else if (set.contains(seq) && packet.isReplay()) {
            reply = new ReplyPacket(sender, seq, ReplyPacket.ReplyType.RECEIVED);
        } else {
            set.add(seq);
            reply = new ReplyPacket(sender, seq, ReplyPacket.ReplyType.RECEIVED);

            ctx.fireChannelRead(new DatagramPacket(packet.content(), packet.recipient(), packet.sender()));
        }

        sendDirectly(reply);
    }

    @Override
    public void setDirectOutboundContext(ChannelHandlerContext ctx) {
        preCodecContext = ctx;
    }

    @Override
    public ChannelFuture sendDirectly(Object msg) {
        return preCodecContext.writeAndFlush(msg);
    }
}
