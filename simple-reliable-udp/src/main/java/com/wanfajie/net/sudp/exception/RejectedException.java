package com.wanfajie.net.sudp.exception;

import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelException;

public class RejectedException extends ChannelException  {

    public RejectedException() {}

    public RejectedException(ByteBufHolder packet) {
        super(packet.toString());
    }

    public RejectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RejectedException(String message) {
        super(message);
    }

    public RejectedException(Throwable cause) {
        super(cause);
    }
}
