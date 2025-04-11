

package org.top.java.netty.example.http2.tiles;

import static io.netty.buffer.Unpooled.unreleasableBuffer;
import static org.top.java.netty.example.http2.Http2ExampleUtil.toByteBuf;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches the images to avoid reading them every time from the disk.
 */

/**
 * 缓存图像以避免每次从磁盘读取。
 */
public final class ImageCache {

    public static ImageCache INSTANCE = new ImageCache();

    private final Map<String, ByteBuf> imageBank = new HashMap<String, ByteBuf>(200);

    private ImageCache() {
        init();
    }

    public static String name(int x, int y) {
        return "tile-" + y + "-" + x + ".jpeg";
    }

    public ByteBuf image(int x, int y) {
        return imageBank.get(name(x, y));
    }

    private void init() {
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 20; x++) {
                try {
                    String name = name(x, y);
                    ByteBuf fileBytes = unreleasableBuffer(toByteBuf(getClass()
                            .getResourceAsStream(name)).asReadOnly());
                    imageBank.put(name, fileBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
