package hr.picsona.extension;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Memory management class
 * Created by Goran on 8.5.2016..
 */
public class ByteArrayPool {

    private final static Logger LOGGER = Logger.getLogger(ByteArrayPool.class.getName());
    private final static int TIMEOUT_MS = 2;

    private static ByteArrayPool byteArrayPool;

    private BlockingQueue<byte[]> bytePoolQueue = new LinkedBlockingQueue<>();

    public static ByteArrayPool getInstance() {
        if (byteArrayPool == null) {
            byteArrayPool = new ByteArrayPool();
        }
        return byteArrayPool;
    }

    public byte[] getByteArray(int length) {
        if (bytePoolQueue.isEmpty()) {
            return new byte[length];
        }
        try {
            byte[] array = bytePoolQueue.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (array == null) {
                return new byte[length];
            }
            return array;
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            return new byte[length];
        }
    }

    public void recycleByteArray(byte[] array) {
        try {
            bytePoolQueue.offer(array, TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }
    }
}
