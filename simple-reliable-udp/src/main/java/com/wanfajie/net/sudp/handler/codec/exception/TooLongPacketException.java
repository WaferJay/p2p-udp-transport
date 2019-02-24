package com.wanfajie.net.sudp.handler.codec.exception;

import java.net.InetSocketAddress;

public class TooLongPacketException extends ReceivedInvalidPacketException {

    public TooLongPacketException(InetSocketAddress sender, int sequence, int length) {
        super(sender, sequence, "Sequence=" + sequence + ", Length=" + length);
    }

    public TooLongPacketException(InetSocketAddress sender, int sequence, int length, Throwable cause) {
        super(sender, sequence, "Sequence=" + sequence + ", Length=" + length, cause);
    }
}
