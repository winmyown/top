package org.top.java.test;

import com.google.common.hash.Hashing;
import io.github.pigmesh.ai.deepseek.core.DeepSeekClient;
import io.github.pigmesh.ai.deepseek.core.chat.ChatCompletionModel;
import io.github.pigmesh.ai.deepseek.core.chat.ChatCompletionResponse;
import reactor.core.publisher.Flux;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 并发版
 */
public class stage3 {

    private static final Map<Integer, String> resultMap = new ConcurrentHashMap<>();

    private static final ExecutorService executor= Executors.newFixedThreadPool(10);
    private  static final int maxRetries=3;
    private static final long baseRetryDelay=1000;

    private static DeepSeekClient tencent;
    private static DeepSeekClient deepseek;
    private static DeepSeekClient vol;
    private static DeepSeekClient silicon;

    static {
        tencent = DeepSeekClient.builder()
                .baseUrl("https://api.lkeap.cloud.tencent.com/v1")
                .model("deepseek-v3")
                .openAiApiKey("")
                .build();
        deepseek = DeepSeekClient.builder()
                // .baseUrl("https://api.lkeap.cloud.tencent.com/v1")
                .model(ChatCompletionModel.DEEPSEEK_CHAT.getValue())
                .openAiApiKey("")
                .build();
        vol = DeepSeekClient.builder()
                .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                .model("ep-20250209140725-wj587")
                .openAiApiKey("")
                .build();
        silicon = DeepSeekClient.builder()
                .baseUrl("https://api.siliconflow.com/v1")
                .model("deepseek-ai/DeepSeek-V3")
                .openAiApiKey("")
                .build();
    }


    public static void main(String[] args) throws Exception {
        System.out.println(new Date());
        String filePath="/Users/develop/winmyown/top/java/src/main/java/org/top/java/source/collection/HashSet.java";
        // TODO: 1. 取注释
        String content=getContent(filePath);
        // TODO: 2. 放入线程池中调用，并且设置重试次数和重试间隔，存储到map
        // TODO: 3. join 线程池，获取结果
        regexJavaDoc(content);
        // TODO: 4. 全部替换
        String result=replaceComments(content);
        writeFile(filePath,result);
        System.out.println(new Date());
        System.exit(0);
    }

    public static String getContent(String filePath) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    public static void regexJavaDoc(String content)
    {
        Pattern pattern = Pattern.compile(
                "(/\\*.*?\\*/|//.*?$)",  // 匹配多行注释或单行注释
                Pattern.DOTALL | Pattern.MULTILINE  // 标志位
        );
        Matcher matcher = pattern.matcher(content);
        String comment;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        while (matcher.find()) {

            comment=matcher.group();
            System.out.println( comment);
            putPool(comment, futures);

            //System.out.println( matcher.group());
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
    }

    public static void putPool(String comment, List<CompletableFuture<Void>> futures ){
        Integer key= Hashing.murmur3_32().hashUnencodedChars(comment).asInt();
        CompletableFuture<Void> future = CompletableFuture
                .supplyAsync(() -> invokeDeepSeekWithRetry(comment), executor)
                .thenAccept(result -> resultMap.put(key, result));
        futures.add(future);
    }

    public static String  invokeDeepSeekWithRetry(String comment){
        String prompt="  将这些java注释以原格式翻译，保留注释字符。仅输出翻译结果，无需解释，无需注释。";
        int time=1;
        while(time<maxRetries){
            try {
                String result=invoke(comment+prompt);
                System.out.println(result);
                return result;
            }catch (Exception e){
                e.printStackTrace();
            }
            waitForRetry(time);
        }
        throw new RuntimeException("Failed to invoke DeepSeek after max retries");
    }
    public static void waitForRetry(int time){
        try {
            Thread.sleep(baseRetryDelay*time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    public static String invoke(String request) {


        Flux<ChatCompletionResponse> flux = vol.chatFluxCompletion(request);
        List<ChatCompletionResponse> responses = flux.collectList().block();
        String result="";
        for (ChatCompletionResponse response : responses) {
            result += response.choices().get(0).delta().content();
        }
        // System.out.println(result);
        return result;
    }

    public static String replaceComments(String content){
        Pattern pattern = Pattern.compile(
                "(/\\*.*?\\*/|//.*?$)",  // 匹配多行注释或单行注释
                Pattern.DOTALL | Pattern.MULTILINE  // 标志位
        );
        Matcher matcher = pattern.matcher(content);
        String comment;
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            comment=matcher.group();
            Integer key=Hashing.murmur3_32().hashUnencodedChars(comment).asInt();
            String replace=resultMap.get(key);
            matcher.appendReplacement(result, replace);
            //System.out.println( matcher.group());
        }
        matcher.appendTail(result);
        System.out.println(result);
        return result.toString();
    }
    private static void writeFile(String filePath, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        }
    }


}
