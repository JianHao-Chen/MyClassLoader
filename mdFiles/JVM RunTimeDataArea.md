---
title: JVM--运行时数据区域
categories: Java笔记
---

这篇文章整理一些关于JVM 运行时数据区域的知识。
<!--more-->

---

## 运行时数据区域
JVM运行时的内存空间的组织，逻辑上又划分为多个区，这些区的生命周期和它是否线程共享有关。
<img src="http://img.blog.csdn.net/20171106111901171">

#### <font color=green>**程序计数器**</font>
也叫PC寄存器（Program Counter Register），是一块较小的内存空间，它可以看作是当前线程所执行的字节码的行号指示器。

 * 每一条JVM线程都有自己的PC寄存器
 * 在任意时刻，一条JVM线程只会执行一个方法的代码。该方法称为该线程的当前方法（Current Method）
 * 如果该方法是java方法，那PC寄存器保存JVM正在执行的字节码指令的地址
 * 如果该方法是native，那PC寄存器的值是undefined。
 * 此内存区域是唯一一个在Java虚拟机规范中没有规定任何OutOfMemoryError情况的区域。
 

#### <font color=green>**Java虚拟机栈**</font>
与PC寄存器一样，java虚拟机栈（Java Virtual Machine Stack）也是线程私有的。每一个JVM线程都有自己的java虚拟机栈，这个栈与线程同时创建，它的生命周期与线程相同。
虚拟机栈描述的是Java方法执行的内存模型：每个方法被执行的时候都会同时创建一个栈帧（Stack Frame）用于存储局部变量表、操作数栈、动态链接、方法出口等信息。每一个方法被调用直至执行完成的过程就对应着一个栈帧在虚拟机栈中从入栈到出栈的过程。
这部分的细节，留到讲解“字节码执行引擎”部分再说。
<font color=red>异常情况</font>
(1) StackOverflowError：
 （当线程请求分配的栈深度超过JVM允许的最大深度时抛出）
(2) OutOfMemoryError：
 如果JVM Stack可以动态扩展，但是在尝试扩展时无法申请到足够的内存去完成扩展，或者在建立新的线程时没有足够的内存去创建对应的虚拟机栈时抛出。

#### <font color=green>**Java堆**</font>
在JVM中，堆（heap）是可供各条线程共享的运行时内存区域，几乎所有的对象实例以及数组都在这里分配内存。
Java堆是内存收集器管理的主要区域，因此被称为“GC堆”（Garbage Collection Heap）。
<font color=red>异常情况</font>
OutOfMemoryError：
堆中没有内存完成实例分配，并且堆也无法再扩展时，抛出一个OutOfMemoryError异常。


#### <font color=green>**方法区**</font>
方法区也是各个线程共享的内存区域，它存储已被虚拟机加载的类信息、常量、静态变量。
我列出了会保存在方法区的以下信息：

* 该类型的全限定名。如java.io.FileOutputStream
* 该类型的直接超类的全限定名。如java.io.OutputStream
* 该类型是类类型还是接口类型。
* 该类型的访问修饰符(public、abstract、final)。
* 任何直接超接口的全限定名的有序列表。如java.io.Closeable,  java.io.Flushable。
* 该类型的常量池。比如所有类型、方法和字段的符号。基本数据类型的直接数值等。
* 字段信息。（字段名、字段的类型、字段的修饰符）
* 方法信息。（方法名、返回类型、参数的数量和类型、修饰符、字节码）
* 类静态变量。
* 指向ClassLoader类的引用
* 指向Class类的引用
**对于每一个被装载的类型，虚拟机都会相应的为它创建一个java.lang.Class类的实例，而且虚拟机还必须以某种方式把这个实例和存储在方 法区中的类型信息关联起来。 这就使得我们可以在程序运行时查看某个加载进内存的类的当前状态信息。也就是反射机制实现的根本。**
* 方法表
**为了能快速定位到类型中的某个方法。JVM对每个装载的类型都会建立一个方法表，用于存储该类型对象可以调用的方法的直接引用，这些方法就包括从超类中继承来的。**

<font color=red>异常情况</font>
OutOfMemoryError： 如果方法区的内存空间不能满足内存分配请求，那Java虚拟机将抛出一个OutOfMemoryError异常。


#### <font color=green>**本地方法栈**</font>
Java虚拟机可能会使用到传统的栈来支持native方法（使用Java语言以外的其它语言编写的方法）的执行，这个栈就是本地方法栈（Native Method Stack）
