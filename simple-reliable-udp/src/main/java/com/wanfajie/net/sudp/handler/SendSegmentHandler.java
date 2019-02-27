package com.wanfajie.net.sudp.handler;

import com.wanfajie.net.sudp.Config;
import com.wanfajie.net.sudp.exception.SegmentFailureException;
import com.wanfajie.net.sudp.packet.DataPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SendSegmentHandler extends ChannelOutboundHandlerAdapter {

    public static final String NAME = "s_udp_segment";

    private static final int MAX_CONTENT_SIZE = Config.PACKET_MAX_CONTENT_LENGTH;

    private ChannelHandlerContext context;
    private AtomicInteger idGenerator = new AtomicInteger();

    private static class SegmentTask implements Runnable {
        private short seq;
        private int total;
        private volatile int finishCount = 0;
        private ChannelPromise promise;
        private Lock lock = new ReentrantLock();
        private final Map<Short, Throwable> exceptions = new HashMap<>();

        private SegmentTask(short _seq, int _total, ChannelPromise _promise) {
            seq = _seq;
            promise = _promise;
            total = _total;
        }

        private boolean isFinish() {
            return finishCount >= total;
        }

        @Override
        public void run() {
            if (!isFinish()) return;

            if (exceptions.isEmpty()) {
                promise.setSuccess();
            } else {
                Throwable cause = new SegmentFailureException();
                for (Throwable each: exceptions.values()) {
                    cause.addSuppressed(each);
                }
                promise.setFailure(cause);
            }
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        DatagramPacket udpPacket = (DatagramPacket) msg;

        send(udpPacket, promise, null);
    }

    public ChannelFuture send(DatagramPacket udpPacket) {
        ChannelPromise promise = context.newPromise();
        send(udpPacket, promise, null);
        return promise;
    }

    public ChannelFuture send(DatagramPacket udpPacket, int flag) {
        ChannelPromise promise = context.newPromise();
        send(udpPacket, promise, flag);
        return promise;
    }

    private void send(DatagramPacket udpPacket, ChannelPromise promise, Integer flag) {
        short id = (short) (idGenerator.incrementAndGet() & 0xffff);
        int seq = id << 16;
        InetSocketAddress address = udpPacket.recipient();

        ByteBuf content = udpPacket.content();
        if (content.readableBytes() > MAX_CONTENT_SIZE) {
            ByteBufAllocator alloc = context.alloc();

            int total = (int) Math.ceil(content.readableBytes() / (float) MAX_CONTENT_SIZE);

            SegmentTask task = new SegmentTask(id, total, promise);

            for (int i = 1; i <= total; i++) {

                ByteBuf segBuf = ByteBufUtil.readBytes(alloc, content,
                        Math.min(MAX_CONTENT_SIZE, content.readableBytes()));

                int segSeq = seq | i;
                DataPacket segPacket = new DataPacket(address, segSeq, segBuf);
                if (flag != null) {
                    segPacket.flag(flag);
                }

                if (i == total) segPacket.flagLast();

                short finalI = (short) i;
                context.write(segPacket).addListener(future -> {

                    try {
                        task.lock.lock();

                        task.finishCount++;
                        if (!future.isSuccess()) {
                            task.exceptions.put(finalI, future.cause());
                        }

                        if (task.isFinish())
                            context.executor().execute(task);
                    } finally {
                        task.lock.unlock();
                    }
                });
            }

            udpPacket.release();
        } else {
            DataPacket packet = new DataPacket(address, seq, content);

            if (flag != null) {
                packet.flag(flag);
            }
            packet.flagLast();
            context.write(packet, promise);
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        context = ctx;
    }
}
