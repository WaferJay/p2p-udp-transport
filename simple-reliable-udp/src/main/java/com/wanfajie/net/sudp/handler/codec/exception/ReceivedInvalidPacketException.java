package com.wanfajie.net.sudp.handler.codec.exception;

import io.netty.handler.codec.DecoderException;

import java.net.InetSocketAddress;

public abstract class ReceivedInvalidPacketException extends DecoderException {

    private InetSocketAddress sender;
    private int sequence;

    public ReceivedInvalidPacketException(InetSocketAddress _sender, int _sequence) {
        super();
        sender = _sender;
        sequence = _sequence;
    }

    public ReceivedInvalidPacketException(InetSocketAddress _sender, int _sequence, String msg) {
        super(msg);
        sender = _sender;
        sequence = _sequence;
    }

    public ReceivedInvalidPacketException(
        InetSocketAddress _sender,
        int _sequence,
        String msg,
        Throwable cause
    ) {
        super(msg, cause);
        sender = _sender;
        sequence = _sequence;
    }

    public ReceivedInvalidPacketException(
            InetSocketAddress _sender,
            int _sequence,
            Throwable cause
    ) {
        super(cause);
        sender = _sender;
        sequence = _sequence;
    }

    public InetSocketAddress sender() {
        return sender;
    }

    public int sequence() {
        return sequence;
    }
}
