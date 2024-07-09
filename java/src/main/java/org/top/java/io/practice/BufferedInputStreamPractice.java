package org.top.java.io.practice;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class BufferedInputStreamPractice {
    public static void main(String[] args) {
        readFile();
    }


    @Benchmark
    public void measureName(Blackhole bh) {
    }


    public static void readFile() {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream("./java/src/main/java/org/top/java/io/practice/test.txt"))) {
            int data;
            while ((data = bis.read()) != -1) {
                System.out.print((char) data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
