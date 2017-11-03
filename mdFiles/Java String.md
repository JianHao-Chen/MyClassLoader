---
title: Java的String
categories: Java笔记
---

这篇文章整理一些关于Java String的问题。
<!--more-->

---

## 问题1：String之常量池问题
先看下面题目：
```
String s1 = new String("s1") ;
String s2 = new String("s1") ;
```
<font color=red>**前面三条语句分别创建了几个对象？**</font> </br>

<font color=blue>3个, 编译期Constant Pool中创建1个,运行期heap中创建2个。</font>
</br>

再来下面代码：
```
String s1 = new String("sss111");
s1=s1.intern();
String s2 = "sss111";
System.out.println(s1 == s2);  //true
```
这里`s1==s2`是返回true的。其中关键在String的intern()方法。
这个方法的作用是：
<font color=gren>“如果常量池中存在当前字符串, 就会直接返回当前字符串. 如果常量池中没有此字符串, 会将此字符串放入常量池中后, 再返回它的引用”。</font>
</br>

## 问题2：String类为什么是final的
```
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    
    /** The value is used for character storage. */
    private final char value[];
```

#### 1.从安全的角度
(1) 确保它们不会在子类中改变语义。
String类是final类，这意味着不允许任何人定义String的子类。如果有一个String的引用，它引用的一定是一个String对象，而不可能是其他类的对象。

(2) String一旦被创建是不能被修改的
因为 java 设计者将 String 设计为可以共享的! 这个在源码中的注释有提及。
既然String是不可变的，自然就不存在并发环境下数据不一致的情况。

#### 2. 从效率的角度
设计成final，JVM不用对相关方法在虚函数表中查询，而直接定位到String类的相关方法上，提高了执行效率。 
</br>

## 问题3：String和final String
```
String a = "hello2";
final String b = "hello";
String c = "hello";
         
System.out.println(a==(b+2));   // true
System.out.println(a==(c+2));   // false
```
这里的知识点包括了final关键字。

用final声明的变量为常量。所以，`(b+2)`与`"hello"+2`(hello2)是一样的，
而`c+2`会返回一个新的String对象(存储在堆里)。

