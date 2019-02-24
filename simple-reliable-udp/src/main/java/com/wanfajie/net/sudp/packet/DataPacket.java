package com.wanfajie.net.sudp.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;

import java.net.InetSocketAddress;

public class DataPacket extends BasePacket implements ByteBufHolder {

    private int replayCount;
    private ByteBuf content;
    private int contentLength;

    public DataPacket(InetSocketAddress sender,
                      InetSocketAddress recipient,
                      int seq, byte crc,
                      int _replayCount,
                      ByteBuf byteBuf,
                      int length) {

        super(sender, recipient, seq, crc);
        replayCount = _replayCount;
        content = byteBuf;
        contentLength = length;
    }

    public DataPacket(InetSocketAddress recipient,
                      int seq,
                      ByteBuf byteBuf) {

        super(recipient, seq);
        replayCount = 0;
        content = byteBuf;
        contentLength = byteBuf.writerIndex();
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
    public ByteBufHolder copy() {
        return replace(content.copy());
    }

    @Override
    public ByteBufHolder duplicate() {
        return replace(content.duplicate());
    }

    @Override
    public ByteBufHolder retainedDuplicate() {
        return replace(content.retainedDuplicate());
    }

    @Override
    public ByteBufHolder replace(ByteBuf content) {
        if (sender() != null) {
            return new DataPacket(sender(), recipient(), sequence(), crc8(),
                    replayCount, content, content.readableBytes());
        } else {
            return new DataPacket(recipient(), sequence(), content);
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
    public ByteBufHolder touch() {
        content.touch();
        return this;
    }

    @Override
    public ByteBufHolder touch(Object hint) {
        content.touch(hint);
        return this;
    }

    @Override
    public String toString() {
        return "DataPacket{" +
                "sequence=" + sequence() +
                ", replayCount=" + replayCount +
                ", contentLength=" + contentLength +
                '}';
    }
}
