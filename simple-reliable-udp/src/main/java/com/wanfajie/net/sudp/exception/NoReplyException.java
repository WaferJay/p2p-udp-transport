package com.wanfajie.net.sudp.exception;

import io.netty.buffer.ByteBufHolder;

public class NoReplyException extends RejectedException {
    public NoReplyException() {}

    public NoReplyException(ByteBufHolder packet) {
        super(packet);
    }

    public NoReplyException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoReplyException(String message) {
        super(message);
    }

    public NoReplyException(Throwable cause) {
        super(cause);
    }
}
