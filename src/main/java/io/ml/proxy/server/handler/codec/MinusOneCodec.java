package io.ml.proxy.server.handler.codec;

import io.netty.channel.CombinedChannelDuplexHandler;

public class MinusOneCodec extends CombinedChannelDuplexHandler<MinusOneDecoder, MinusOneEncoder> {
    public MinusOneCodec(MinusOneDecoder inboundHandler, MinusOneEncoder outboundHandler) {
        super(inboundHandler, outboundHandler);
    }

    public MinusOneCodec() {
        this(new MinusOneDecoder(), new MinusOneEncoder());
    }
}
