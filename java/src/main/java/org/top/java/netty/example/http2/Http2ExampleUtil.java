package org.top.java.netty.example.http2;

import static io.netty.util.internal.ObjectUtil.checkNotNull;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Utility methods used by the example client and server.
 */

/**
 * 示例客户端和服务器使用的实用方法。
 */
public final class Http2ExampleUtil {

    /**
     * Response header sent in response to the http-&gt;http2 cleartext upgrade request.
     */

    /**
     * 响应头在http->http2明文升级请求的响应中发送。
     */
    public static final String UPGRADE_RESPONSE_HEADER = "http-to-http2-upgrade";

    /**
     * Size of the block to be read from the input stream.
     */

    /**
     * 从输入流中读取的块大小。
     */
    private static final int BLOCK_SIZE = 1024;

    private Http2ExampleUtil() { }

    /**
     * @param string the string to be converted to an integer.
     * @param defaultValue the default value
     * @return the integer value of a string or the default value, if the string is either null or empty.
     */

    /**
     * @param string 要转换为整数的字符串。
     * @param defaultValue 默认值
     * @return 字符串的整数值，如果字符串为null或空，则返回默认值。
     */
    public static int toInt(String string, int defaultValue) {
        if (string != null && !string.isEmpty()) {
            return Integer.parseInt(string);
        }
        return defaultValue;
    }

    /**
     * Reads an InputStream into a byte array.
     * @param input the InputStream.
     * @return a byte array representation of the InputStream.
     * @throws IOException if an I/O exception of some sort happens while reading the InputStream.
     */

    /**
     * 将InputStream读取为字节数组。
     * @param input 输入流。
     * @return 表示InputStream的字节数组。
     * @throws IOException 如果在读取InputStream时发生I/O异常。
     */
    public static ByteBuf toByteBuf(InputStream input) throws IOException {
        ByteBuf buf = Unpooled.buffer();
        int n = 0;
        do {
            n = buf.writeBytes(input, BLOCK_SIZE);
        } while (n > 0);
        return buf;
    }

    /**
     * @param query the decoder of query string
     * @param key the key to lookup
     * @return the first occurrence of that key in the string parameters
     */

    /**
     * @param query 查询字符串的解码器
     * @param key 要查找的键
     * @return 字符串参数中该键的第一次出现
     */
    public static String firstValue(QueryStringDecoder query, String key) {
        checkNotNull(query, "Query can't be null!");
        List<String> values = query.parameters().get(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }
}
