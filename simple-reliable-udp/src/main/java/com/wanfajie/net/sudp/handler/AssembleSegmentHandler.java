package com.wanfajie.net.sudp.handler;

import com.wanfajie.net.sudp.packet.DataPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Function;

public class AssembleSegmentHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME = "s_udp_assemble";

    private Map<PacketKey, SortedSet<DataPacket>> segments = new HashMap<>();
    private static final Comparator<DataPacket> PACKET_COMPARATOR
            = Comparator.comparingInt(DataPacket::sequence);

    private static final Function<PacketKey, SortedSet<DataPacket>> SET_FACTORY
            = k -> new TreeSet<>(PACKET_COMPARATOR);

    private static class PacketKey {
        private short seq;
        private InetSocketAddress sender;

        private PacketKey(short _seq, InetSocketAddress _sender) {
            seq = _seq;
            sender = _sender;
        }

        @Override
        public int hashCode() {
            return Objects.hash(seq, sender);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PacketKey packetKey = (PacketKey) o;
            return seq == packetKey.seq &&
                    Objects.equals(sender, packetKey.sender);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        DataPacket packet = (DataPacket) msg;

        int sequence = packet.sequence();
        short packetSeq = (short) (sequence >>> 16);
        int segmentSeq = sequence & 0xffff;

        if (segmentSeq == 0) {
            ByteBuf content = packet.content();
            InetSocketAddress sender = packet.sender();
            InetSocketAddress recipient = packet.recipient();

            ctx.fireChannelRead(new DatagramPacket(content, recipient, sender));
            return;
        }

        PacketKey packetKey = new PacketKey(packetSeq, packet.sender());

        SortedSet<DataPacket> segmentSet = getSegmentSet(packetKey);
        segmentSet.add(packet);

        DataPacket lastSegment = segmentSet.last();
        if (!isLastSegment(lastSegment)) {
            return;
        }

        int srcSegSeqSum = 0;
        for (DataPacket segment: segmentSet) {
            int segSeq = segment.sequence() & 0xffff;
            srcSegSeqSum += segSeq;
        }

        int lastSegSeq = lastSegment.sequence() & 0xffff;
        int dstSegSeqSum = (1 + lastSegSeq) * segmentSet.size() / 2;
        if (srcSegSeqSum < dstSegSeqSum) {
            return;
        }

        if (srcSegSeqSum > dstSegSeqSum) {
            for (DataPacket each: segmentSet)
                ReferenceCountUtil.release(each);

            segments.remove(packetKey);
            throw new IllegalStateException();
        }

        int contentLength = 0;
        CompositeByteBuf content = ctx.alloc().compositeBuffer(segmentSet.size());
        for (DataPacket each: segmentSet) {
            content.addComponent(each.content());
            contentLength += each.contentLength();
        }
        content.writerIndex(contentLength);

        ctx.fireChannelRead(new DatagramPacket(content, packet.recipient(), packet.sender()));
    }

    private static boolean isLastSegment(DataPacket packet) {
        return (packet.flag() & DataPacket.FLAG_LAST) != 0;
    }

    private SortedSet<DataPacket> getSegmentSet(PacketKey key) {
        return segments.computeIfAbsent(key, SET_FACTORY);
    }
}
