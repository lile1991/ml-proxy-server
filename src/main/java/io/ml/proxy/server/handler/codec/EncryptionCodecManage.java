package io.ml.proxy.server.handler.codec;

import io.ml.proxy.server.config.EncryptionProtocolEnum;
import io.netty.channel.CombinedChannelDuplexHandler;

public class EncryptionCodecManage {

    public static CombinedChannelDuplexHandler<?, ?> newServerCodec(EncryptionProtocolEnum encryptionProtocol) {
        switch (encryptionProtocol) {
            case MinusOne: return new MinusOneServerCodec();
        }
        throw new UnsupportedOperationException("Unsupported the encryption protocol " + encryptionProtocol);
    }

    public static CombinedChannelDuplexHandler<?, ?> newClientCodec(EncryptionProtocolEnum encryptionProtocol) {
        switch (encryptionProtocol) {
            case MinusOne: return new MinusOneClientCodec();
        }
        throw new UnsupportedOperationException("Unsupported the encryption protocol " + encryptionProtocol);
    }
}
