package io.ml.proxy.server.handler.codec.minusone;

import io.netty.channel.CombinedChannelDuplexHandler;

public class MinusOneClientCodec extends CombinedChannelDuplexHandler<MinusOneDecoder, MinusOneEncoder> {
    public MinusOneClientCodec(MinusOneDecoder inboundHandler, MinusOneEncoder outboundHandler) {
        super(inboundHandler, outboundHandler);
    }

    public MinusOneClientCodec() {
        this(new MinusOneDecoder(), new MinusOneEncoder());
    }
}
