package com.wanfajie.net.sudp.handler.codec;

import io.netty.channel.CombinedChannelDuplexHandler;

public class SUdpCodec extends CombinedChannelDuplexHandler<SUdpDecoder, SUdpEncoder> {

    public static final String NAME = "s_udp_codec";

    public SUdpCodec() {
        super(new SUdpDecoder(), new SUdpEncoder());
    }
}
