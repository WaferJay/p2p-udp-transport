package com.wanfajie.net.sudp.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import com.wanfajie.net.sudp.packet.BasePacket;
import com.wanfajie.net.sudp.packet.DataPacket;
import com.wanfajie.net.sudp.packet.ReplyPacket;
import com.wanfajie.net.sudp.CRCUtil;

import java.net.InetSocketAddress;
import java.util.List;

public class SUdpEncoder extends MessageToMessageEncoder<BasePacket> {

    private static final int DEFAULT_CAPACITY = 1536;

    @Override
    protected void encode(ChannelHandlerContext ctx, BasePacket msg, List<Object> out) {

        DatagramPacket udpPacket = null;
        if (msg instanceof ReplyPacket) {
            ReplyPacket packet = (ReplyPacket) msg;
            udpPacket = udpPacket(packet, ctx.alloc());
        } else if (msg instanceof DataPacket) {
            DataPacket packet = (DataPacket) msg;
            udpPacket = udpPacket(packet, ctx.alloc());
        }

        if (udpPacket != null) out.add(udpPacket);
    }

    private static DatagramPacket udpPacket(ReplyPacket packet, ByteBufAllocator alloc) {
        InetSocketAddress receiver = packet.recipient();
        ByteBuf buf = alloc.buffer(8);

        buf.writeShort(BasePacket.PROTOCOL_REPLY);

        buf.writeInt(packet.sequence());
        buf.writeByte(0x00);
        buf.writeByte(packet.getType().getCode());

        byte crc = CRCUtil.crc8(buf);
        replaceCrc(buf, crc);

        return new DatagramPacket(buf, receiver);
    }

    private static DatagramPacket udpPacket(DataPacket packet, ByteBufAllocator alloc) {
        InetSocketAddress receiver = packet.recipient();
        ByteBuf buf = alloc.buffer(DEFAULT_CAPACITY);

        buf.writeShort(BasePacket.PROTOCOL_DATA);

        buf.writeInt(packet.sequence());
        buf.writeByte(0x00);
        buf.writeByte(packet.flag());

        byte replayCount;
        if (packet.replayCount() >> 8 == 0) {
            replayCount = (byte) packet.replayCount();
        } else {
            replayCount = (byte) 0xff;
        }

        buf.writeByte(replayCount);
        buf.writeInt(packet.contentLength());
        buf.writeBytes(packet.content());

        byte crc = CRCUtil.crc8(buf);
        replaceCrc(buf, crc);

        return new DatagramPacket(buf, receiver);
    }

    private static void replaceCrc(ByteBuf buf, byte b) {
        buf.setByte(6, b);
    }
}
