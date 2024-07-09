package org.top.java.io;


import java.util.TimeZone;

public class te {
    public static void main(String[] args) {
        //testasdf
        String currentDir = System.getProperty("user.dir");
        System.out.println("Current working directory: " + currentDir);
        String[] availableIDs = TimeZone.getAvailableIDs();
        for (String id : availableIDs) {
            System.out.println(id);
        }
    }
}
