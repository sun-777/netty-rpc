package com.sun.common.netty.codec;

import com.sun.common.util.Constants;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @description: 粘包、半包解码器
 * @author: Sun Xiaodong
 */

public class RpcProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {

    public RpcProtocolFrameDecoder() {
        this(0);
    }

    public RpcProtocolFrameDecoder(final int maxFrameLength) {
        this(maxFrameLength <= 0 ? Constants.DEFAULT_MAX_FRAME_LENGTH : maxFrameLength, Constants.BODY_OFFSET, Constants.BODY_LENGTH, 0, 0);
    }

    public RpcProtocolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }
}
