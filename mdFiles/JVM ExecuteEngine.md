---
title: JVM--字节码执行引擎
categories: Java笔记
---

这篇文章用于整理JVM字节码执行引擎的知识点。
<!--more-->

---

## 运行时栈帧结构
栈帧是用于支持虚拟机进行方法调用和方法执行的数据结构，它是虚拟机运行时数据区中虚拟机栈（Virtual Machine Stack）的栈元素。

 * 栈帧存储了方法的局部变量表、操作数栈、动态连接、方法的返回地址 和一些额外的附加信息。
 * 每一个方法从调用开始到执行完成的过程，都对应着一个栈帧在虚拟机栈里面从入栈到出栈的过程。
 * 栈帧中需要多大的局部变量表和多深的操作数栈在编译代码的过程中已经完全确定，并写入到方法表的Code属性中。
 * 在活动的线程中，位于当前栈顶的栈帧才是有效的，称之为当前帧，与这个栈帧相关联的方法称为当前方法。执行引擎运行的所有字节码指令只针对当前栈帧进行操作。

栈帧的结构如下图：

<img src="http://img.blog.csdn.net/20171106155456023">

#### 局部变量表
局部变量表是一组变量值存储空间，用于存储<font color=blue>方法参数和方法内部定义的局部变量。</font>
在Java程序编译为.class文件时，就在方法的Code属性的max_locals数据项确定了该方法所需要的局部变量的最大容量。


#### 操作数栈
操作数栈(Operand Stack)也叫操作栈，它的最大深度也是在编译时写到Code属性的max_stacks数据项。
当一个方法刚开始执行的时候，这个方法的操作数栈是空的，在方法的执行过程中，会有各种字节码指令往操作数中写入和提取内容，也就是出栈/入栈操作。操作数栈中元素的数据类型必须与字节码指令的序列严格匹配2，这由编译器在编译器期间进行验证，同时在类加载过程中的类检验阶段的数据流分析阶段要再次验证。另外我们说Java虚拟机的解释引擎是基于栈的执行引擎，其中的栈指的就是操作数栈。


#### 动态连接
每个栈帧都包含一个指向运行时常量池中该栈帧所属方法的引用，持有该引用是为了支持方法调用过程中的动态连接。

#### 方法返回地址
方法当一个方法被执行后，有两种方式可以退出这个方法。 
第一种方式是执行引擎遇到任意一个方法返回的字节码指令，这时候可能会有返回值传递给上层的方法调用者（调用当前方法的方法称为调用者），是否有返回值和返回值的类型将遇到何种方法返回指令来决定，这种退出方法的方式称为正常完成出口。
另外一种退出方式是：在方法执行过程中遇到异常，并且这个异常没有在方法体内得到处理，无论是JVM内部产生的异常，还是代码中使用athrow字节码指令产生的异常，只要在本方法的异常表中没有搜索到匹配的异常处理器，就会导致方法退出。这种方式被称为异常退出出口。此方式不会给上层调用者产生任何返回值。
无论采用哪一种退出方式，在方法退出后，都会返回到方法被调用的位置，程序才能继续执行。方法返回时可能要在栈帧中保存一些信息，用来帮助恢复它的上层方法的执行状态。一般来说，方法正常退出之后，调用者的PC计数器的值就可以作为返回地址。栈帧中很可能会保存这个计数器值，而方法异常退出后，返回地址就要通过异常处理器表来确定，栈帧一般不保存这部分信息。
方法退出实际上就是把当前栈帧出栈的操作：因此退出时可能执行的操作：恢复上层方法局部变量表和操作数栈，把返回值压入调用者栈帧的操作数栈中，调整PC计数器的值以指向指令后面的一条指令。

---

## 方法调用
方法调用并不等于方法执行，方法调用阶段唯一的任务是<font color=brown>确定被调用方法的版本（即调用哪一个方法）</font>，暂时还不涉及方法内部的具体运行过程。
Class文件的编译过程不包含传统编译中的连接步骤，一切方法调用在Class文件里面存储的都是符号引用，而不是方法在实际运行时内存布局中的入口地址（即直接引用）。这个特性给Java带来更强大的动态扩展的能力，也使Java方法调用过程变得复杂起来，需要在类加载期间，甚至到运行期间才能确定目标方法的直接引用。

#### 解析
所有方法调用中的目标方法在Class文件里面都是一个常量池中的符号引用，在类加载的解析阶段，会将其中的一部分符号引用转化为直接引用，这种解析能成立的前提是：<font color=orange>方法在编译期可知，在运行期不变。</font>
这类方法调用称为 <font color=green>**解析（Resolution）**</font>。

Java虚拟机提供5条方法调用字节码指令：

 * invokestatic:调用静态方法
 * invokespecial:调用构造器，私有方法和父类方法
 * invokevirtual:调用虚方法
 * invokeinterface:调用接口方法
 * invokedynamic：先在运行时动态解析出该方法，然后执行。

只要能被`invokestatic`和`invokespecial`指令调用的方法，都可以在解析阶段确定唯一的调用版本，符合这个条件的有“静态方法”、“私有方法”、“实例构造器”、“父类方法”。

再说一次：<font color=purple>解析调用是静态过程，在编译期就确定，在类加载的解析过程就把相关的符合引用转为直接引用。</font>

与之对应的有**分派（Dispatch）调用**。

#### 分派

1. 静态分派
其实就是Java语言中的“重载”（Overload）。我们只要记住以下2点：
 * 确定重载哪一个方法的时期在**编译阶段**
 * 编译器是通过变量的静态类型（即声明类型）而不是动态类型作为判断依据的

2. 动态分派
这里就是我们所熟悉的Java语言的“重写”（Override）。
先看以下例子：

```
public class DynamicDispatch {
    
    static abstract class Human{
        protected abstract void sayHello();
    }

    static class Man extends Human{

        @Override
        protected void sayHello() {
            System.out.println("Hello man!");
        }
    }

    static class Woman extends Human{

        @Override
        protected void sayHello() {
            System.out.println("Hello women!");
        }
    }

    public static void main(String[] args)
    {
        Human man = new Man();
        Human woman = new Woman();
        man.sayHello();
        woman.sayHello();
        man = new Woman();
        man.sayHello();
    }
}

// 结果：
Hello man!
Hello women!
Hello women!
```
接下来，我们看一下这段代码对应的字节码：

```
public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=2, locals=3, args_size=1
         0: new           #16                 // class test/DynamicDispatch$Man
         3: dup
         4: invokespecial #18                 // Method test/DynamicDispatch$Man."<init>":()V
         7: astore_1
         8: new           #19                 // class test/DynamicDispatch$Woman
        11: dup
        12: invokespecial #21                 // Method test/DynamicDispatch$Woman."<init>":()V
        15: astore_2
        16: aload_1
        17: invokevirtual #22                 // Method test/DynamicDispatch$Human.sayHello:()V
        20: aload_2
        21: invokevirtual #22                 // Method test/DynamicDispatch$Human.sayHello:()V
        24: new           #19                 // class test/DynamicDispatch$Woman
        27: dup
        28: invokespecial #21                 // Method test/DynamicDispatch$Woman."<init>":()V
        31: astore_1
        32: aload_1
        33: invokevirtual #22                 // Method test/DynamicDispatch$Human.sayHello:()V
        36: return
```
其中，调用`sayHello`方法的第17行和21行字节码是关键：

 * 都是使用`invokespecial`指令，而且都是引用常量池的第22项（Human类的sayHello方法）
 * 但是它们的运行结果却不同

`invokespecial`指令的多态查找过程是：

 1. 找到操作数栈顶的第一个元素所指向的对象的实际类型，记作C
 2. 如果在类型C中找到与常量中的描述符和简单名称都相符的方法，则进行访问权限校验，如果通过就返回这个方法的直接引用，查找过程结束；如果不通过，返回java.lang.IllegalAccessError异常。
 3. 否则，按照继承关系从下往上依次对C的各个父类进行第2步的搜索和验证
 4. 没有找到，抛出 java.lang.AbstractMethodError异常

这个过程通俗一点就是，先从C类里面寻找该方法，如果没有，就从C的父类里面找。！

由于`invokespecial`指令的第一步是在运行期间确定接收者的实际类型，所以2次的调用`invokespecial`指令把常量池中的类方法符号引用（Human.sayHello:()V）解析到不同的直接引用上，这个过程就是Java方法重写的本质。


## 基于栈的字节码解释执行引擎
以下面代码为例：

```
public int calc(){
    int a = 100;
    int b = 200;
    int c = 300;
    return (a+b)*c;
}
```
对应的字节码为：
```
public int calc();
    descriptor: ()I
    flags: ACC_PUBLIC
    Code:
      stack=2, locals=4, args_size=1
         0: bipush        100
         2: istore_1
         3: sipush        200
         6: istore_2
         7: sipush        300
        10: istore_3
        11: iload_1
        12: iload_2
        13: iadd
        14: iload_3
        15: imul
        16: ireturn
```
然后是几张图，描述虚拟机执行这段代码时，是如何使用操作数栈和局部变量表的：

(1) 执行偏移地址为0的时候：
<img src="http://img.blog.csdn.net/20171106185656932">
(2) 执行偏移地址为1的时候：
<img src="http://img.blog.csdn.net/20171106185704537">
(3) 执行偏移地址为11的时候：
<img src="http://img.blog.csdn.net/20171106185709240">
(4) 执行偏移地址为12的时候：
<img src="http://img.blog.csdn.net/20171106185713557">
(5) 执行偏移地址为13的时候：
<img src="http://img.blog.csdn.net/20171106185716969">
(6) 执行偏移地址为14的时候：
<img src="http://img.blog.csdn.net/20171106185720332">
(7) 执行偏移地址为16的时候：
<img src="http://img.blog.csdn.net/20171106185725360">
