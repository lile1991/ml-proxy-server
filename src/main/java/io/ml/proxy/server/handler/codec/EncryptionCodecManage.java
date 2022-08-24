package io.ml.proxy.server.handler.codec;

import io.ml.proxy.server.config.EncryptionProtocolEnum;
import io.ml.proxy.server.handler.codec.bytemap.ByteMapClientCodec;
import io.ml.proxy.server.handler.codec.bytemap.ByteMapServerCodec;
import io.ml.proxy.server.handler.codec.minusone.MinusOneClientCodec;
import io.ml.proxy.server.handler.codec.minusone.MinusOneServerCodec;
import io.netty.channel.CombinedChannelDuplexHandler;

public class EncryptionCodecManage {

    public CombinedChannelDuplexHandler<?, ?> newServerCodec(EncryptionProtocolEnum encryptionProtocol) {
        switch (encryptionProtocol) {
            case MinusOne: return new MinusOneServerCodec();
            case ByteMap: return new ByteMapServerCodec();
        }
        throw new UnsupportedOperationException("Unsupported the encryption protocol " + encryptionProtocol);
    }

    public CombinedChannelDuplexHandler<?, ?> newClientCodec(EncryptionProtocolEnum encryptionProtocol) {
        switch (encryptionProtocol) {
            case MinusOne: return new MinusOneClientCodec();
            case ByteMap: return new ByteMapClientCodec();
        }
        throw new UnsupportedOperationException("Unsupported the encryption protocol " + encryptionProtocol);
    }
}
