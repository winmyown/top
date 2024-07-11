# JMH 预备知识

> 为啥需要JMH，直接用end-start测耗时不行吗
> 


```java
public static void main(String[] args) {
    long start=System.currentTimeMillis();
    // logic process
    long end=System.currentTimeMillis();
    System.out.println(end-start);
    
}
```
看似简单确受多方面的影响：
- 系统硬件和操作系统
- javac编译器
- jvm