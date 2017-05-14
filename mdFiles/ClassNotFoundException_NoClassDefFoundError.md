---
title: ClassNotFoundException和NoClassDefFoundError
categories: Java笔记
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



#### 产生ClassNotFoundException
产生ClassNotFoundException很容易，只要待加载的类在类加载器的查找路径中没有找到，就会抛出ClassNotFoundException。


#### 产生NoClassDefFoundError
先创建以下2个类：

```bash
public class A {
}

public class B extends A{
}
```
对于生成的A.class， B.class，我们只把B.class放到C盘根目录下， 然后使用自定义的类加载器加载B类：

```bash
public void testNoClassDefFoundError(){
  String classDataRootPath = "C:";
  FileSystemClassLoader fscl1 = new FileSystemClassLoader(classDataRootPath);
  String className = "B";
  
  try {
    Class<?> class1 = fscl1.loadClass(className);
  }
  catch (Exception e) {
    e.printStackTrace();
  }
}
```
FileSystemClassLoader会先在C盘根目录下查找B.class文件，找到并得到B.class的字节数组，然后会调用defineClass()方法生成相应的Class对象。
在defineClass()中需要加载B的父类A，会再次调用FileSystemClassLoader的loadClass()方法，但是这次会找不到A类了，从而抛出ClassNotFoundException。这会导致defineClass()方法抛出NoClassDefFoundError。


还有以下例子展示产生NoClassDefFoundError:
创建类B在Test包下：
```bash
package Test;

public class B {
}
```
把生成的B.class文件放在C盘根目录下，然后使用FileSystemClassLoader加载。同样，在defineClass()方法抛出异常：
```bash
Exception in thread "main" java.lang.NoClassDefFoundError: B (wrong name: Test/B)
    at java.lang.ClassLoader.defineClass1(Native Method)
    at java.lang.ClassLoader.defineClass(Unknown Source)
    at java.lang.ClassLoader.defineClass(Unknown Source)
    at Learning.Sample.FileSystemClassLoader.findClass(FileSystemClassLoader.java:53)
    ....
```

结论是，我认为这个说法是对的
> 方法 loadClass()抛出的是 java.lang.ClassNotFoundException异常；
方法 defineClass()抛出的是 java.lang.NoClassDefFoundError异常。
