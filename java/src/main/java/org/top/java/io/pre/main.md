# 前置基础
**思考问题**
> Java io 是如何被抽象出来的，为何这样操作处理
>
>1. 首先要了解计算机中io的基础知识。底层实现逻辑。
>
## linux/unix 中的文件
[**linux/unix 中的文件**](file.md)。
## io的分类
[**io的分类**](io.md)
## io模型

## 文件io,网络io的底层操作

## java中的路径、URL
```java
File file = new File("src/test.txt");
File file = new File(TestRelativePath.class.getResource("/test.txt").getFile());
File file = new File(Thread.currentThread().getContextClassLoader().getResource("test.txt").getFile());
File file = new File(getServletContext().getRealPath("/WEB-INF/classes/test.txt"));

File f = new File("src/com/lavasoft/res/a.txt");
InputStream in =ReadFile.class.getResourceAsStream("/com/lavasoft/res/a.txt");
```