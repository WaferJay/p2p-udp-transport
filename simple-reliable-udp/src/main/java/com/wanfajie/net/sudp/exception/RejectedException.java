package com.wanfajie.net.sudp.exception;

import io.netty.channel.ChannelException;
import com.wanfajie.net.sudp.packet.DataPacket;

public class RejectedException extends ChannelException  {

    public RejectedException() {}

    public RejectedException(DataPacket packet) {
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
