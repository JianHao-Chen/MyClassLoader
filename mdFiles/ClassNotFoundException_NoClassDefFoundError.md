---
title: ClassNotFoundException和NoClassDefFoundError
categories: JDK源码
---

提出问题：
你知道ClassNotFoundException和NoClassDefFoundError的区别吗？

<!--more-->

---

## ClassNotFoundException
首先，它是一个Exception！

当程序尝试通过以下3种方法,用class的名字加载这个class:
1. Class.forName()
2. ClassLoader.findSystemClass()
3. ClassLoader.loadClass()
但是找不到这个class的定义,于是这个异常被抛出。

## NoClassDefFoundError
注意，这是一个 Error，说具体一点是一个 LinkageError 。
引用JDK文档对它的介绍：
> Thrown if the Java Virtual Machine or a <code>ClassLoader</code> instance tries to load in the definition of a class (as part of a normal method call or as part of creating a new instance using the <code>new</code> expression) and no definition of the class could be found.

对于ClassNotFoundException和NoClassDefFoundError，网上有以下说法：
> 方法 loadClass()抛出的是 java.lang.ClassNotFoundException异常；
方法 defineClass()抛出的是 java.lang.NoClassDefFoundError异常。

这个说法是否对呢？

我以下面的例子来探讨。

#### 
