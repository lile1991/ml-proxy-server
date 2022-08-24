package io.ml.proxy.server.handler.socks5;

import io.ml.proxy.server.config.UsernamePasswordAuth;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Socks5PasswordAuthRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest> {
    private final List<UsernamePasswordAuth> usernamePasswordAuths;

    public Socks5PasswordAuthRequestHandler(List<UsernamePasswordAuth> usernamePasswordAuths) {
        this.usernamePasswordAuths = usernamePasswordAuths;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5PasswordAuthRequest msg) {
        if(isAuthorized(msg)) {
            ctx.pipeline().remove(Socks5PasswordAuthRequestDecoder.class);
            ctx.pipeline().remove(ctx.name());

            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS);
            ctx.writeAndFlush(passwordAuthResponse);
        } else {
            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
            ctx.writeAndFlush(passwordAuthResponse).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private boolean isAuthorized(DefaultSocks5PasswordAuthRequest msg) {
        for(UsernamePasswordAuth usernamePasswordAuth: usernamePasswordAuths) {
            if(usernamePasswordAuth.getUsername().equals(msg.username()) && usernamePasswordAuth.getPassword().equals(msg.password())) {
                return true;
            }
        }
        return false;
    }
}
