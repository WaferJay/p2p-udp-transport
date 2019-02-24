package com.wanfajie.net.sudp.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import com.wanfajie.net.sudp.exception.NoReplyException;
import com.wanfajie.net.sudp.exception.RejectedException;
import com.wanfajie.net.sudp.packet.DataPacket;
import com.wanfajie.net.sudp.packet.ReplyPacket;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SendHandler extends ChannelDuplexHandler implements Runnable {

    public static final String NAME = "s_udp_replay";

    private final int maxReplayCount;
    private final int replayTimeout;
    private final int replayPeriod;

    private AtomicInteger idGenerator = new AtomicInteger();
    private Map<Integer, ReplayTask> taskMap = new HashMap<>();
    private ChannelHandlerContext context;
    private ScheduledFuture timeoutSchedule;

    private static class ReplayTask {
        private ChannelPromise resultPromise;
        private DataPacket data;
        private long sendingTime = System.currentTimeMillis();

        private ReplayTask(DataPacket _data, ChannelPromise promise) {
            data = _data;
            resultPromise = promise;
        }
    }

    public SendHandler() {
        this(Config.MAX_REPLAY_COUNT, Config.REPLAY_TIMEOUT, Config.REPLAY_TIMEOUT_PERIOD);
    }

    public SendHandler(int _maxReplayCount, int _replayTimeout, int _replayPeriod) {
        maxReplayCount = _maxReplayCount;
        replayTimeout = _replayTimeout;
        replayPeriod = _replayPeriod;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        context = ctx;
        timeoutSchedule = ctx.executor().scheduleAtFixedRate(this,
                replayTimeout, replayPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        DatagramPacket udpPacket = (DatagramPacket) msg;

        int seq = (idGenerator.incrementAndGet() & 0xffff) << 16;
        InetSocketAddress address = udpPacket.recipient();
        DataPacket packet = new DataPacket(address, seq, udpPacket.content());

        sendCopy(packet);
        taskMap.put(seq, new ReplayTask(packet, promise));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ReplyPacket)) {
            ctx.fireChannelRead(msg);
            return;
        }

        ReplyPacket packet = (ReplyPacket) msg;
        int seq = packet.sequence();
        ReplayTask task = taskMap.get(seq);

        if (task != null) {
            switch (packet.getType()) {
                case RECEIVED:
                    removeTask(task);
                    task.resultPromise.setSuccess();
                    System.out.println("Received: " + seq);
                    break;
                case REPLAY:
                    replayTask(task);
                    task.sendingTime = System.currentTimeMillis();
                    System.out.println("Replay: " + seq);
                    break;
                case REJECT:
                    removeTask(task);
                    task.resultPromise.setFailure(new RejectedException(task.data));
                    System.out.println("Reject: " + seq);
                    break;
                default:
                    // no default
            }
        }
    }

    private void sendCopy(DataPacket packet) {
        context.writeAndFlush(packet.copy());
    }

    private void replayTask(ReplayTask task) {
        task.data.incrReplayCount();
        sendCopy(task.data);
    }

    private void removeTask(ReplayTask task) {
        task.data.release();
        taskMap.remove(task.data.sequence());
    }

    @Override
    public void run() {
        for (ReplayTask task : taskMap.values()) {
            long curr = System.currentTimeMillis();

            if (curr - task.sendingTime > replayTimeout) {

                if (task.data.replayCount() >= maxReplayCount) {
                    removeTask(task);
                    task.resultPromise.setFailure(new NoReplyException(task.data));
                } else {
                    replayTask(task);
                    task.sendingTime = curr;
                }
            }
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        timeoutSchedule.cancel(true);
        for (ReplayTask task : taskMap.values()) {
            task.data.release();
        }

        taskMap.clear();
        super.close(ctx, promise);
    }
}
