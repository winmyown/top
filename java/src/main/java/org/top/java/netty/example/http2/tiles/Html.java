

package org.top.java.netty.example.http2.tiles;

import static io.netty.util.CharsetUtil.UTF_8;

import java.util.Random;

/**
 * Static and dynamically generated HTML for the example.
 */

/**
 * 静态和动态生成的HTML示例。
 */
public final class Html {

    public static final String IP = System.getProperty("ip", "127.0.0.1");

    public static final byte[] FOOTER = "</body></html>".getBytes(UTF_8);

    public static final byte[] HEADER = ("<!DOCTYPE html><html><head lang=\"en\"><title>Netty HTTP/2 Example</title>"
            + "<style>body {background:#DDD;} div#netty { line-height:0;}</style>"
            + "<link rel=\"shortcut icon\" href=\"about:blank\">"
            + "<meta charset=\"UTF-8\"></head><body>A grid of 200 tiled images is shown below. Compare:"
            + "<p>[<a href='https://" + url(Http2Server.PORT) + "?latency=0'>HTTP/2, 0 latency</a>] [<a href='http://"
            + url(HttpServer.PORT) + "?latency=0'>HTTP/1, 0 latency</a>]<br/>" + "[<a href='https://"
            + url(Http2Server.PORT) + "?latency=30'>HTTP/2, 30ms latency</a>] [<a href='http://" + url(HttpServer.PORT)
            + "?latency=30'>HTTP/1, 30ms latency</a>]<br/>" + "[<a href='https://" + url(Http2Server.PORT)
            + "?latency=200'>HTTP/2, 200ms latency</a>] [<a href='http://" + url(HttpServer.PORT)
            + "?latency=200'>HTTP/1, 200ms latency</a>]<br/>" + "[<a href='https://" + url(Http2Server.PORT)
            + "?latency=1000'>HTTP/2, 1s latency</a>] [<a href='http://" + url(HttpServer.PORT)
            + "?latency=1000'>HTTP/1, " + "1s latency</a>]<br/>").getBytes(UTF_8);

    private static final int IMAGES_X_AXIS = 20;

    private static final int IMAGES_Y_AXIS = 10;

    private Html() {
    }

    private static String url(int port) {
        return IP + ":" + port + "/http2";
    }

    public static byte[] body(int latency) {
        int r = Math.abs(new Random().nextInt());
        // The string to be built contains 13192 fixed characters plus the variable latency and random cache-bust.
        // 要构建的字符串包含13192个固定字符以及可变的延迟和随机的缓存清除。
        int numberOfCharacters = 13192 + stringLength(latency) + stringLength(r);
        StringBuilder sb = new StringBuilder(numberOfCharacters).append("<div id=\"netty\">");
        for (int y = 0; y < IMAGES_Y_AXIS; y++) {
            for (int x = 0; x < IMAGES_X_AXIS; x++) {
                sb.append("<img width=30 height=29 src='/http2?x=")
                .append(x)
                .append("&y=").append(y)
                .append("&cachebust=").append(r)
                .append("&latency=").append(latency)
                .append("'>");
            }
            sb.append("<br/>\r\n");
        }
        sb.append("</div>");
        return sb.toString().getBytes(UTF_8);
    }

    private static int stringLength(int value) {
        return Integer.toString(value).length() * IMAGES_X_AXIS * IMAGES_Y_AXIS;
    }
}
