package org.top.java.util;

import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CollectionTest {
    public static void main(String[] args) {
        String input = "Year: 2023, Next: 2024";
        Pattern pattern = Pattern.compile("(\\d{4})");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            String replacement = String.valueOf(year + 1); // 年份+1
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        System.out.println(sb.toString()); // 输出: Year: 2024, Next: 2025
    }
}
