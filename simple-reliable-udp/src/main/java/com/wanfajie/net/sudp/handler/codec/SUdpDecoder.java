package com.wanfajie.net.sudp.handler.codec;

import com.wanfajie.net.sudp.CRCUtil;
import com.wanfajie.net.sudp.handler.codec.exception.CRCMismatchException;
import com.wanfajie.net.sudp.handler.codec.exception.TooLongPacketException;
import com.wanfajie.net.sudp.packet.BasePacket;
import com.wanfajie.net.sudp.packet.DataPacket;
import com.wanfajie.net.sudp.packet.ReplyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.net.InetSocketAddress;
import java.util.List;

public class SUdpDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private static final int DEFAULT_MAX_CONTENT_SIZE = 1024;
    private static final byte[] ONE_BYTE_ARRAY = {0x00};

    private int maxContentSize;

    public SUdpDecoder(int _maxContentSize) {
        maxContentSize = _maxContentSize;
    }

    public SUdpDecoder() { this(DEFAULT_MAX_CONTENT_SIZE); }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket udpPacket, List<Object> out) {
        ByteBuf in = udpPacket.content();
        short protocol = in.readShort();

        if (protocol == BasePacket.PROTOCOL_DATA) {
            DataPacket packet = createDataPacket(udpPacket.sender(), udpPacket.recipient(), in, ctx.alloc());
            out.add(packet);
        } else if (protocol == BasePacket.PROTOCOL_REPLY) {
            ReplyPacket packet = createReplyPacket(udpPacket.sender(), udpPacket.recipient(), in);
            out.add(packet);
        }
    }

    private DataPacket createDataPacket(InetSocketAddress sender,
                                        InetSocketAddress recipient,
                                        ByteBuf in,
                                        ByteBufAllocator alloc) {
        int seq = in.readInt();
        byte crc = in.readByte();
        int replayCount = in.readUnsignedByte();
        int contentLength = in.readInt();

        if (contentLength > maxContentSize) {
            throw new TooLongPacketException(sender, seq, contentLength);
        }

        ByteBuf content;
        try {
            content = ByteBufUtil.readBytes(alloc, in, contentLength);
        } catch (IndexOutOfBoundsException e) {
            throw new TooLongPacketException(sender, seq, contentLength, e);
        }

        if (isCRC8Mismatch(crc, in)) {
            throw new CRCMismatchException(sender, seq, DataPacket.class);
        }

        return new DataPacket(sender, recipient, seq, crc, replayCount, content, contentLength);
    }

    private ReplyPacket createReplyPacket(InetSocketAddress sender, InetSocketAddress recipient, ByteBuf in) {
        int seq = in.readInt();
        byte crc = in.readByte();
        byte type = in.readByte();

        if (isCRC8Mismatch(crc, in)) {
            throw new CRCMismatchException(sender, seq, ReplyPacket.class);
        }

        return new ReplyPacket(sender, recipient, seq, crc, ReplyPacket.ReplyType.getType(type));
    }

    private boolean isCRC8Mismatch(byte crc, ByteBuf in) {
        byte currCrc = CRCUtil.crc8(in, 0, 6);
        currCrc = CRCUtil.crc8(ONE_BYTE_ARRAY, 0, 1, currCrc);
        currCrc = CRCUtil.crc8(in, 7, in.writerIndex() - 7, currCrc);
        return currCrc != crc;
    }
}
