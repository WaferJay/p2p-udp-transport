package com.wanfajie.net.sudp.handler;

import com.wanfajie.net.sudp.Config;
import com.wanfajie.net.sudp.exception.NoReplyException;
import com.wanfajie.net.sudp.exception.RejectedException;
import com.wanfajie.net.sudp.packet.DataPacket;
import com.wanfajie.net.sudp.packet.ReplyPacket;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SendHandler extends ChannelDuplexHandler implements Runnable {

    public static final String NAME = "s_udp_replay";

    private final int maxReplayCount;
    private final int replayTimeout;

    private Map<Integer, ReplayTask> taskMap = new ConcurrentHashMap<>();
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
        this(Config.MAX_REPLAY_COUNT, Config.REPLAY_TIMEOUT);
    }

    public SendHandler(int _maxReplayCount, int _replayTimeout) {
        maxReplayCount = _maxReplayCount;
        replayTimeout = _replayTimeout;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        context = ctx;
        timeoutSchedule = ctx.executor().scheduleWithFixedDelay(this,
                replayTimeout, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        DataPacket packet = (DataPacket) msg;

        sendCopy(packet);
        taskMap.put(packet.sequence(), new ReplayTask(packet, promise));
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

    private void sendCopy(ByteBufHolder message) {
        context.writeAndFlush(message.copy());
    }

    private void replayTask(ReplayTask task) {
        task.data.incrReplayCount();
        sendCopy(task.data);
    }

    private void removeTask(ReplayTask task) {
        ReferenceCountUtil.release(task.data);
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
            ReferenceCountUtil.release(task.data);
        }

        taskMap.clear();
        super.close(ctx, promise);
    }
}
