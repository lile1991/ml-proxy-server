package io.ml.proxy.server.handler.codec.minusone;

import io.netty.channel.CombinedChannelDuplexHandler;

public class MinusOneServerCodec extends CombinedChannelDuplexHandler<MinusOneDecoder, MinusOneEncoder> {
    public MinusOneServerCodec(MinusOneDecoder inboundHandler, MinusOneEncoder outboundHandler) {
        super(inboundHandler, outboundHandler);
    }

    public MinusOneServerCodec() {
        this(new MinusOneDecoder(), new MinusOneEncoder());
    }
}
