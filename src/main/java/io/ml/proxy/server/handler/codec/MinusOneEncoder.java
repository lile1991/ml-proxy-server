package io.ml.proxy.server.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class MinusOneEncoder extends MessageToMessageEncoder<ByteBuf> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        ByteBuf encodeMsg = ctx.alloc().buffer(msg.readableBytes(), msg.capacity());
        for(int i = msg.readerIndex(); i < msg.writerIndex(); i ++) {
            byte b = msg.readByte();
            if(b == Byte.MIN_VALUE) {
                b = Byte.MAX_VALUE;
            } else {
                b --;
            }
            encodeMsg.writeByte(b);
        }

        out.add(encodeMsg);
    }
}
