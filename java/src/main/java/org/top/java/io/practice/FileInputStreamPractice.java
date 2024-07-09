package org.top.java.io.practice;

import java.io.FileInputStream;
import java.io.IOException;

public class FileInputStreamPractice {

    public static void main(String[] args) {
        readFile();
    }

    public static void readFile() {
        try (FileInputStream fis = new FileInputStream("./java/src/main/java/org/top/java/io/practice/test.txt")) {
            byte[] bytes = new byte[4];
            int data;
            while ((data = fis.read(bytes)) != -1) {
                String str = new String(bytes,0,data,"UTF-8");
                System.out.print( str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
