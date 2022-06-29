package com.sun.common.netty.codec;

import java.util.List;

import com.sun.common.exchange.codec.Codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @description: RPC编码解码适配器
 * @author: Sun Xiaodong
 */
public class CodecAdapter {

    private final ChannelHandler encoder = new RpcEncoder();

    private final ChannelHandler decoder = new RpcDecoder();

    private final Codec codec;

    public CodecAdapter(Codec codec) {
        this.codec = codec;
    }


    public ChannelHandler getEncoder() {
        return encoder;
    }

    public ChannelHandler getDecoder() {
        return decoder;
    }

    /**
     * RPC消息编码器
     */
    class RpcEncoder extends MessageToByteEncoder<Object> {
        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
            final byte[] encode = codec.encode(msg);
            out.writeBytes(encode);
        }
    }

    /**
     * RPC消息解码器
     */
    class RpcDecoder extends ByteToMessageDecoder {
        // 因为自定义了处理粘包、半包解码器LengthFieldBasedFrameDecoder，所以此处in可保证是完整的消息
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            final byte[] data = new byte[in.capacity()];
            in.readBytes(data, 0, in.capacity());
            Object msg = codec.decode(data);
            out.add(msg);
        }
    }
}
