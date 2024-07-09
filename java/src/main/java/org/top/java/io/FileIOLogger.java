package org.top.java.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class FileIOLogger {
    private static final Logger logger = LoggerFactory.getLogger(FileIOLogger.class);

    //@org.openjdk.jmh.annotations.Benchmark
    //public void measureName(org.openjdk.jmh.infra.Blackhole bh) {
    //
    //
    //}

    public static void main(String[] args) {
        logger.info("Thread {} starts reading file", Thread.currentThread().getName());
        try (FileInputStream fis = new FileInputStream("input.txt")) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                // 处理数据
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Thread {} finished reading file", Thread.currentThread().getName());
    }
}

