package io.ml.relay.handler.codec.lee;

import io.netty.channel.CombinedChannelDuplexHandler;

public class LeeServerCodec extends CombinedChannelDuplexHandler<LeeDecoder, LeeEncoder> {
    public LeeServerCodec(LeeDecoder inboundHandler, LeeEncoder outboundHandler) {
        super(inboundHandler, outboundHandler);
    }

    public LeeServerCodec() {
        this(new LeeDecoder(), new LeeEncoder());
    }
}
