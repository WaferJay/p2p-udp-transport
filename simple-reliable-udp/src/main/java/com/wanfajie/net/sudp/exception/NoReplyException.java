package com.wanfajie.net.sudp.exception;

import com.wanfajie.net.sudp.packet.DataPacket;

public class NoReplyException extends RejectedException {
    public NoReplyException() {}

    public NoReplyException(DataPacket packet) {
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
