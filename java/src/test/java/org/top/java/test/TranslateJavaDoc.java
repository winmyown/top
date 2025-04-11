package org.top.java.test;

import com.google.common.hash.Hashing;
import io.github.pigmesh.ai.deepseek.core.DeepSeekClient;
import io.github.pigmesh.ai.deepseek.core.chat.ChatCompletionResponse;
import reactor.core.publisher.Flux;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class TranslateJavaDoc {

    /**
     * 存储翻译结果
     */
    private static final Map<Integer, String> resultMap = new ConcurrentHashMap<>();

    private static final ExecutorService executor= Executors.newFixedThreadPool(10);
    private  static final int maxRetries=3;
    private static final long baseRetryDelay=1000;

    private static DeepSeekClient tencent;
    private static DeepSeekClient deepseek;
    private static DeepSeekClient vol;
    private static DeepSeekClient silicon;

    static {
        // tencent = DeepSeekClient.builder()
        //         .baseUrl("https://api.lkeap.cloud.tencent.com/v1")
        //         .model("deepseek-v3")
        //         .openAiApiKey("")
        //         .build();
        // deepseek = DeepSeekClient.builder()
        //         // .baseUrl("https://api.lkeap.cloud.tencent.com/v1")
        //         .model(ChatCompletionModel.DEEPSEEK_CHAT.getValue())
        //         .openAiApiKey("")
        //         .build();
        vol = DeepSeekClient.builder()
                .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                .model("ep-20250209140725-wj587")
                .openAiApiKey("")
                .build();
        // silicon = DeepSeekClient.builder()
        //         .baseUrl("https://api.siliconflow.com/v1")
        //         .model("deepseek-ai/DeepSeek-V3")
        //         .openAiApiKey("")
        //         .build();
    }

    enum Option {
        //替换 将英文注释替换成中文
        REPLACE,
        //追加 在英文注释后面追加中文注释
        APPEND
    }

    public static void main(String[] args) throws Exception {
        System.out.println(new Date());
        String filePath="/Users/develop/winmyown/top/java/src/main/java/org/top/java/netty/source/util/old/Recycler.java";
        directory(filePath,Option.APPEND);
        System.exit(0);
    }

    /**
     * 支持文件夹 递归处理
     */
    public static void directory(String path,Option option){
        Path startPath = Paths.get(path);

        try {
            Files.walk(startPath)
                    .filter(Files::isRegularFile) // 只打印文件（排除文件夹）
                    .forEach(filepath->translateFile(filepath.toString(),option));
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            executor.shutdown();
        }
    }

    public static String translateFile(String filePath,Option option) {
        // TODO: 1. 取源码
        String content=getContent(filePath);
        // TODO: 2. 用正则表达式取出所有注释
        // TODO: 3. 放入线程池中调用Deepseek翻译，将结果存储到resultMap
        regexJavaDoc(content);
        // TODO: 4. 全部替换
        String result=replaceOrAppendComments(content,option);
        writeFile(filePath,result);
        System.out.println("###############"+filePath+" translation completed"+"###############");
        return "ok";
    }

    public static String getContent(String filePath)  {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filePath);
        } catch (IOException e) {
            System.out.println("Error reading file: " + filePath);
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
            if (ignore(comment)){
                continue;
            }
            System.out.println( comment);
            Integer key=getKey(comment);
            if (!resultMap.containsKey(key)){
                putPool(comment, futures);
            }

            //System.out.println( matcher.group());
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    }

    public static boolean ignore(String comment){
        if ("//".equals(comment)) return true;
        return false;
    }

    public static void putPool(String comment, List<CompletableFuture<Void>> futures ){
        Integer key= getKey(comment);
        CompletableFuture<Void> future = CompletableFuture
                .supplyAsync(() -> invokeDeepSeekWithRetry(comment), executor)
                .thenAccept(result -> resultMap.put(key, result));
        futures.add(future);
    }
    public static Integer getKey(String comment){
        return Hashing.murmur3_32().hashUnencodedChars(comment).asInt();
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

    public static String replaceOrAppendComments(String content,Option option){
        Pattern pattern = Pattern.compile(
                "(^\\s*)(/\\*.*?\\*/|//.*?$)",  // 匹配多行注释或单行注释
                Pattern.DOTALL | Pattern.MULTILINE  // 标志位
        );
        Matcher matcher = pattern.matcher(content);
        String comment;
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String indent = matcher.group(1);    // 缩进部分，例如 4 个空格
            comment = matcher.group(2);          // 注释本体
            // comment=matcher.group();
            Integer key=getKey(comment);
            String replace=resultMap.get(key);
            try {
                String safeReplace = replace.replace("\\", "\\\\").replace("$", "\\$");
                if (option==Option.APPEND){
                    matcher.appendReplacement(result, indent+comment+"\n"+indent+safeReplace);
                }else{
                    matcher.appendReplacement(result, safeReplace);
                }
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("error comment:"+comment+" #replace:"+replace);
            }

        }
        matcher.appendTail(result);
        System.out.println(result);
        return result.toString();
    }
    private static void writeFile(String filePath, String content)  {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        } catch (IOException e) {
            System.out.println("Error writing to file: " + filePath);
        }
    }


}
