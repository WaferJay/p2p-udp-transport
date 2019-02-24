package com.wanfajie.net.sudp.handler.codec.exception;

import com.wanfajie.net.sudp.packet.BasePacket;

import java.net.InetSocketAddress;

public class CRCMismatchException extends ReceivedInvalidPacketException {

    private Class<? extends BasePacket> packetType;

    public CRCMismatchException(
        InetSocketAddress sender,
        int sequence,
        Class<? extends BasePacket> packetType
    ) {
        super(sender, sequence, packetType.getSimpleName() + "[Sequence=" + sequence + "]");
        this.packetType = packetType;
    }

    public Class<? extends BasePacket> packetType() {
        return packetType;
    }
}
