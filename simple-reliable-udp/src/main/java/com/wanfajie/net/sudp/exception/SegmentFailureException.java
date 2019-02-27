package com.wanfajie.net.sudp.exception;

import io.netty.channel.ChannelException;

public class SegmentFailureException extends ChannelException {

    public SegmentFailureException() {
        super();
    }

    public SegmentFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public SegmentFailureException(String message) {
        super(message);
    }

    public SegmentFailureException(Throwable cause) {
        super(cause);
    }
}
