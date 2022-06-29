package com.sun.common.exchange.codec;

import com.sun.common.enumerator.Event;
import com.sun.common.enumerator.Serialization;
import com.sun.common.exchange.message.Header;
import com.sun.common.exchange.message.Request;
import com.sun.common.exchange.message.RequestBody;
import com.sun.common.exchange.message.RequestHeader;
import com.sun.common.exchange.message.Response;
import com.sun.common.exchange.message.ResponseBody;
import com.sun.common.exchange.message.ResponseHeader;
import com.sun.common.id.serialization.Serializer;
import com.sun.common.id.Id;
import com.sun.common.id.ObjectId;
import com.sun.common.util.Crc32C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Objects;

import static com.sun.common.util.Assertions.isTrueArgument;

/**
 * @description: 定义通信协议格式（参考了dubbo协议）
 *            Bit offset:
 *            4 bytes       0 - 31: CRC32    // CRC32校验码
 *            2 bytes      32 - 47: MAGIC_NUMBER    // 魔数
 *            2 bytes      48 - 63: REQUEST_RESPONSE_CONTROL    // 请求、响应控制
 *            1 bytes      64 - 71: SERIALIZATION_TYPE    // 序列化类型，见Serialization类定义
 *            1 bytes      72 - 79: RESPONSE_STATUS    // 响应状态，见ResponseStatus类状态定义
 *           12 bytes     80 - 175: REQUEST_ID    // 请求ID，由Id类生成
 *            4 bytes    176 - 207: TIMEOUT    // 超时时间（客户端请求的超时时间，单位ms。相对值）
 *            4 bytes    208 - 239: BODY_LENGTH    // 请求Data长度
 *
 *          REQUEST_RESPONSE_CONTROL：
 *           Bit offset:
 *           1st bit: 请求或响应标识(1 -> request; 0 -> response)
 *           2nd bit: 是否响应(1 -> a response required; 0 -> no response required)
 *           3rd ~ 5th bits: 事件标记(见Event枚举类定义。0 -> NONE，非事件; 1 -> HEARTBEAT，由于使用的Netty自带心跳机制，暂时不用此标记; 2 -> FILE_UPLOAD; )
 *           6th ~ 8th bits: IdType标记
 *           9 ~ 16th bits: 保留比特位（reserved bits），待后续新增功能使用。
 *
 * @author: Sun Xiaodong
 *
 */
public final class ExchangeCodec implements Codec {
    private static final Logger log = LoggerFactory.getLogger(ExchangeCodec.class);
    static final int CRC_OFFSET = 0;
    static final int CRC_LENGTH = 4;
    static final int MAGIC_OFFSET = CRC_OFFSET + CRC_LENGTH;
    static final int MAGIC_LENGTH = 2;
    static final int REQUEST_RESPONSE_CONTROL_OFFSET = MAGIC_OFFSET + MAGIC_LENGTH;
    static final int REQUEST_RESPONSE_CONTROL_LENGTH = 2;
    static final int SERIALIZATION_TYPE_OFFSET = REQUEST_RESPONSE_CONTROL_OFFSET + REQUEST_RESPONSE_CONTROL_LENGTH;
    static final int SERIALIZATION_TYPE_LENGTH = 1;
    static final int RESPONSE_STATUS_OFFSET = SERIALIZATION_TYPE_OFFSET + SERIALIZATION_TYPE_LENGTH;
    static final int RESPONSE_STATUS_LENGTH = 1;
    static final int REQUEST_ID_OFFSET = RESPONSE_STATUS_OFFSET + RESPONSE_STATUS_LENGTH;
    static final int REQUEST_ID_LENGTH = 12;
    static final int TIMEOUT_OFFSET = REQUEST_ID_OFFSET + REQUEST_ID_LENGTH;
    static final int TIMEOUT_LENGTH = 4;
    public static final int BODY_LENGTH_OFFSET = TIMEOUT_OFFSET + TIMEOUT_LENGTH;
    public static final int BODY_LENGTH_LENGTH = 4;

    // protocol header length
    public static final int HEADER_LENGTH = BODY_LENGTH_OFFSET + BODY_LENGTH_LENGTH;

    static final short MAGIC = (short) 0x2bc;

    //---- REQUEST_RESPONSE_CONTROL start ----
    //# 1st bit: request or response tag mask #
    static final int REQUEST_RESPONSE_SHIFT_BITS = 15;
    static final int REQUEST_RESPONSE_TAG_MASK = 0x8000;
    public static final int REQUEST = 1;
    //public static final int RESPONSE = 0;
    //# 2nd bit: Required 1st bit equals to REQUEST. #
    static final int RESPONSE_REQUIRED_SHIFT_BITS = 14;
    static final int RESPONSE_REQUIRED_TAG_MASK = 0x4000;
    static final int RESPONSE_REQUIRED = 1;
    //static final int NO_RESPONSE_REQUIRED = 0;
    //# 3rd ~ 5th bits: event tag #
    static final int EVENT_SHIFT_BITS = 11;
    static final int EVENT_TAG_MASK = 0x3800;
    //# 6th ~ 8th bits: id tag #
    static final int ID_SHIFT_BITS = 8;
    static final int ID_TAG_MASK = 0x700;
    public static final int OBJECT_ID = 0;
    //---- REQUEST_RESPONSE_CONTROL end ----


    public ExchangeCodec() {}


    // 编码RPC请求或RPC响应
    @Override
    public byte[] encode(final Object msg) {
        if (msg instanceof Request) {
            return encodeRequest((Request) msg).array();
        } else if (msg instanceof Response) {
            return encodeResponse((Response) msg).array();
        } else {
            log.error("Unknown object: {}", msg);
            throw new IllegalArgumentException("Unknown object");
        }
    }

    // RPC请求、RPC响应解码
    // 解码后的对象可能是响应，也可能是请求。需要根据协议中的REQUEST_RESPONSE_CONTROL字段判断
    @Override
    public Object decode(final byte[] data) {
        final ByteBuffer encodeHeader = ByteBuffer.wrap(data, 0, HEADER_LENGTH);
        // 校验魔数、校验CRC32
        final short magic = encodeHeader.getShort(MAGIC_OFFSET);
        if (magic != MAGIC) {
            return null;
        }
        final long crc32 = Crc32C.compute(encodeHeader, MAGIC_OFFSET, data.length - CRC_LENGTH);
        if (encodeHeader.getInt(CRC_OFFSET) != (int) (crc32 & 0xFFFFFFFFL)) {
            throw new IllegalStateException("Incorrect crc32 checksum");
        }

        // 超时时间
        final int timeout = encodeHeader.getInt(TIMEOUT_OFFSET);
        // 请求、响应控制字段
        final short control = encodeHeader.getShort(REQUEST_RESPONSE_CONTROL_OFFSET);
        final boolean isRequest = REQUEST == ((control & REQUEST_RESPONSE_TAG_MASK) >>> REQUEST_RESPONSE_SHIFT_BITS) ,
                isResponseRequired = RESPONSE_REQUIRED == ((control & RESPONSE_REQUIRED_TAG_MASK) >>> RESPONSE_REQUIRED_SHIFT_BITS);
        final Header header = isRequest ? RequestHeader.getDefault(timeout).setResponseRequired(isResponseRequired) : ResponseHeader.getDefault();

        // 事件标记
        final byte event = (byte) ((control & EVENT_TAG_MASK) >>> EVENT_SHIFT_BITS);
        Event e = Event.values()[0].codeOf(event).orElse(null);
        header.setEvent(Objects.requireNonNull(e));

        // 写入请求ID
        final int idType = ((control & ID_TAG_MASK) >>> ID_SHIFT_BITS);
        final byte[] requestId = new byte[REQUEST_ID_LENGTH];
        System.arraycopy(data, REQUEST_ID_OFFSET, requestId, 0, REQUEST_ID_LENGTH);
        final Id id = getRequestId(requestId, idType);
        header.setId(id);

        // 序列化类型
        final byte serialization = encodeHeader.get(SERIALIZATION_TYPE_OFFSET);
        Serialization s = Serialization.values()[0].codeOf(serialization).orElse(null);
        header.setSerialization(Objects.requireNonNull(s));

        // RequestData长度
        final int bodyLength = encodeHeader.getInt(BODY_LENGTH_OFFSET);
        isTrueArgument("bodyLength == data.length - HEADER_LENGTH", bodyLength == data.length - HEADER_LENGTH);
        final byte[] decodeData = new byte[bodyLength];
        System.arraycopy(data, HEADER_LENGTH, decodeData, 0, bodyLength);
        Serializer serializer = header.serializerFactory().newSerializer();

        if (isRequest) {  // 请求
            final RequestBody body = serializer.deserialize(RequestBody.class, decodeData);
            return new Request((RequestHeader) header, body);
        } else {  // 响应
            // 响应状态
            ResponseHeader responseHeader = (ResponseHeader) header;
            responseHeader.setStatus(encodeHeader.get(RESPONSE_STATUS_OFFSET));

            final ResponseBody body = serializer.deserialize(ResponseBody.class, decodeData);
            return new Response(responseHeader, body);
        }
    }


    // 编码请求数据
    private static ByteBuffer encodeRequest(final Request request) {
        Serializer serializer = request.getHeader().serializerFactory().newSerializer();
        final byte[] body = serializer.serialize(request.getBody());
        return encodeData(request.getHeader(), body);
    }

    // 编码响应数据
    private static ByteBuffer encodeResponse(final Response response) {
        Serializer serializer = response.getHeader().serializerFactory().newSerializer();
        final byte[] body = serializer.serialize(response.getBody());
        return encodeData(response.getHeader(), body);
    }

    private static ByteBuffer encodeData(final Header header, final byte[] body) {
        final boolean isRequest = header instanceof RequestHeader;
        final int bodyLength = body.length;
        // 根据协议头 + 协议体，分配buffer
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH + bodyLength);

        //Crc32校验值需要最后才可写入，位置先空出
        buffer.position(MAGIC_OFFSET);
        // 1、魔数
        buffer.putShort(MAGIC);
        // 2、请求响应控制
        //      请求: 请求标识、是否响应、事件标记
        //      响应: 响应标识，事件标记
        final short control = getRequestResponseControl(header);
        buffer.putShort(control);
        // 3、写入序列化算法
        buffer.put(header.getSerialization().code());
        // 4、写入响应状态（只在Response响应下生效；如果是Request请求，则为0）
        buffer.put(isRequest ? ((byte) 0) : ((ResponseHeader) header).getStatus());
        // 5、写入请求ID
        buffer.put(Objects.requireNonNull(header.getId()).toByteArray());
        // 6、写入超时时间
        buffer.putInt(isRequest ? ((RequestHeader) header).getTimeoutMillis() : 0);
        // 7、写入序列化后的requestData长度
        buffer.putInt(bodyLength);
        // 8、写入序列化后的RequestData
        buffer.put(body);
        // 9、计算header的crc32值，并指定位置、长度，写入CRC32
        buffer.flip();  //切换到读模式
        final long crc32 = Crc32C.compute(buffer, MAGIC_OFFSET, buffer.limit() - CRC_LENGTH);
        buffer.putInt(CRC_OFFSET, (int) (crc32 & 0xFFFFFFFFL));

        return buffer;
    }

    // 编码请求、响应控制
    private static short getRequestResponseControl(final Header header) {
        final boolean isRequest = header instanceof RequestHeader;
        short control = isRequest ? (short) (1 << REQUEST_RESPONSE_SHIFT_BITS) : 0;
        // Request请求是否需要响应
        if (isRequest && ((RequestHeader) header).getResponseRequired()) {
            control |= (short) (RESPONSE_REQUIRED << RESPONSE_REQUIRED_SHIFT_BITS);
        }
        // 是否是事件
        final int event = header.getEvent().code();
        if (event > 0) {
            control |= (short) (event << EVENT_SHIFT_BITS);
        }

        // id类型
        final short idType = header.getId().getIdType();
        control |= (short) (idType << ID_SHIFT_BITS);

        return control;
    }

    // 解码请求ID
    private static Id getRequestId(final byte[] requestId, final int idType) {
        if (idType == OBJECT_ID) {
            return new ObjectId(requestId);
        }
        throw new IllegalStateException("Undefined Id type");
    }

}
