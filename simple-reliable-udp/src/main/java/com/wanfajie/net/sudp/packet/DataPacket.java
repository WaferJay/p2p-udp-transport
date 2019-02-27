package com.wanfajie.net.sudp.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

import java.net.InetSocketAddress;

public class DataPacket extends BasePacket<ByteBuf> implements ByteBufHolder {

    public static final byte FLAG_LAST = 0x01;
    public static final byte FLAG_NO_REPLY = 0x02;

    private int replayCount;
    private byte flag;
    private ByteBuf content;
    private int contentLength;

    public DataPacket(InetSocketAddress sender,
                      InetSocketAddress recipient,
                      int seq, byte crc,
                      int replayCount,
                      ByteBuf byteBuf,
                      int length) {

        this(sender, recipient, seq, crc, replayCount, byteBuf, length, (byte) 0);
    }

    public DataPacket(InetSocketAddress sender,
                      InetSocketAddress recipient,
                      int seq, byte crc,
                      int _replayCount,
                      ByteBuf byteBuf,
                      int length, byte _flag) {

        super(sender, recipient, seq, crc);
        replayCount = _replayCount;
        content = byteBuf;
        contentLength = length;
        flag = _flag;
    }

    public DataPacket(InetSocketAddress recipient,
                      int seq,
                      ByteBuf byteBuf) {

        this(recipient, seq, byteBuf, 0, (byte) 0);
    }

    private DataPacket(InetSocketAddress recipient,
                      int seq,
                      ByteBuf byteBuf,
                      int _replayCount,
                      byte _flag) {

        super(recipient, seq);
        replayCount = _replayCount;
        content = byteBuf;
        contentLength = byteBuf.writerIndex();
        flag = _flag;
    }

    public int incrReplayCount() {
        return ++replayCount;
    }

    public int replayCount() {
        return replayCount;
    }

    public boolean isReplay() {
        return replayCount != 0;
    }

    public int contentLength() {
        return contentLength;
    }

    @Override
    public ByteBuf content() {
        return content;
    }

    @Override
    public boolean release() {
        return content.release();
    }

    @Override
    public boolean release(int decrement) {
        return content.release(decrement);
    }

    @Override
    public int refCnt() {
        return content.refCnt();
    }

    @Override
    public DataPacket copy() {
        return replace(content.copy());
    }

    @Override
    public DataPacket duplicate() {
        return replace(content.duplicate());
    }

    @Override
    public DataPacket retainedDuplicate() {
        return replace(content.retainedDuplicate());
    }

    @Override
    public DataPacket replace(ByteBuf content) {
        if (sender() != null) {
            return new DataPacket(sender(), recipient(), sequence(), crc8(),
                    replayCount, content, content.readableBytes(), flag);
        } else {
            return new DataPacket(recipient(), sequence(), content, replayCount, flag);
        }
    }

    @Override
    public DataPacket retain() {
        content.retain();
        return this;
    }

    @Override
    public DataPacket retain(int increment) {
        content.retain(increment);
        return this;
    }

    @Override
    public DataPacket touch() {
        content.touch();
        return this;
    }

    @Override
    public DataPacket touch(Object hint) {
        content.touch(hint);
        return this;
    }

    @Override
    public String toString() {
        return "DataPacket{" +
                "sequence=" + sequence() +
                ", replayCount=" + replayCount +
                ", contentLength=" + contentLength +
                ", flag=" + Integer.toBinaryString(flag) +
                '}';
    }

    public DataPacket flagNoReply() {
        flag |= FLAG_NO_REPLY;
        return this;
    }

    public DataPacket flagLast() {
        flag |= FLAG_LAST;
        return this;
    }

    public byte flag() {
        return flag;
    }

    public void flag(int f) {
        flag = (byte) f;
    }
}
