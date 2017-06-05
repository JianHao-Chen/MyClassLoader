---
title: Java NIO
categories: Java笔记
---

提出问题：
什么是Java NIO？这个估计不能只用几句话就完整地描述。

<!--more-->

---


## 介绍NIO
NIO包（java.nio.*）引入了四个关键的抽象数据类型，它们共同解决传统的I/O类中的一些问题。

1. Buffer：它是包含数据且用于读写的线形表结构。其中还提供了一个特殊类用于内存映射文件的I/O操作。
2. Charset：它提供Unicode字符串影射到字节序列以及逆影射的操作。
3. Channels：包含socket，file和pipe三种管道，它实际上是双向交流的通道。
4. Selector：它将多元异步I/O操作集中到一个或多个线程中（它可以被看成是Unix中select（）函数或Win32中WaitForSingleEvent（）函数的面向对象版本）。

#### 缓冲区(Buffer)
缓冲区主要用来作为从通道发送或者接收数据的容器。缓冲区实质上是一个数组。通常它是一个字节数组，但是也可以使用其他种类的数组。但是一个缓冲区不仅仅是一个数组。缓冲区提供了对数据的结构化访问，而且还可以跟踪系统的读/写进程。 

对于每一种基本 Java 类型都有一种缓冲区类型： 

* ByteBuffer
* CharBuffer
* ShortBuffer
* IntBuffer
* LongBuffer
* FloatBuffer
* DoubleBuffer

#### 字符编码(Charset)
向ByteBuffer中存放数据涉及到两个问题：字节的顺序和字符转换。ByteBuffer内部通过ByteOrder类处理了字节顺序问题，但是并没有处理字符转换。事实上，ByteBuffer没有提供方法读写String。
Java.nio.charset.Charset处理了字符转换问题。它通过构造CharsetEncoder和CharsetDecoder将字符序列转换成字节和逆转换。

#### 通道(Channel)
Channel用于在字节缓冲区和位于通道另一侧的实体（通常是一个文件或套接字）之间有效地传输数据。
