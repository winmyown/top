<style>
.hljs-built_in, .hljs-builtin-name, .hljs-literal, .hljs-meta, .hljs-number, .hljs-params, .hljs-type {
    color: blue;
}
.hljs {
    display: block;
    overflow-x: auto;
    padding: 16px;
    color: #abb2bf;
    background: blue;
}
/* 代码块内的关键字样式 */
pre code .java .hljs-keyword {
    color: blue; /* 关键字颜色 */
    font-weight: bold; /* 加粗 */
}

/* 代码块内的字符串样式 */
pre code .hljs-string {
    color: blue; /* 字符串颜色 */
}

/* 代码块内的注释样式 */
pre code .hljs-comment {
    color: blue; /* 注释颜色 */
    font-style: italic; /* 斜体 */
}

/* 代码块内的函数名样式 */
pre code .hljs-function {
    color: blue; /* 函数名颜色 */
}

/* 内置对象和类型样式 */
pre code .hljs-built_in,
pre code .hljs-type {
    color: blue; /* 内置对象和类型颜色 */
    font-weight: bold; /* 加粗 */
}
/* 关键字样式 */
pre code .keyword, pre code .hljs-keyword {
    color: #d73a49; /* 关键字颜色 */
    font-weight: bold; /* 加粗 */
}

/* 字符串样式 */
pre code .string, pre code .hljs-string {
    color: #6a8759; /* 字符串颜色 */
}

/* 注释样式 */
pre code .comment, pre code .hljs-comment {
    color: #757575; /* 注释颜色 */
    font-style: italic; /* 斜体 */
}

/* 函数样式 */
pre code .function, pre code .hljs-function {
    color: #dc8c34; /* 函数颜色 */
}

/* 内置对象和类型样式 */
pre code .built_in, pre code .type, pre code .hljs-built_in, pre code .hljs-type {
    color: #b58900; /* 内置对象和类型颜色 */
    font-weight: bold; /* 加粗 */
}
</style>

## IO包及BIO相关 关键类

```java
import java.io.*;

File file;
FileInputStream fileInputStream;
FileOutputStream fileOutputStream;
FileReader fileReader;
FileWriter fileWriter;
BufferedReader bufferedReader;
BufferedWriter bufferedWriter;
PrintWriter printWriter;
DataInputStream dataInputStream;
DataOutputStream dataOutputStream;
ObjectInputStream objectInputStream;
ObjectOutputStream objectOutputStream;

public static void main(String[] args) {
    
}
```