---
title: 类实例的传递、返回和类实例属性的访问
categories: Java笔记
---

提出问题：
1. 类实例被传递、返回时，是以reference类型的。而相应的字节码是怎样的呢？
2. 访问类实例的属性(field)时，相应的字节码又是是怎样的呢？

<!--more-->

---

## 类实例的传递、返回
先看Java代码：
```bash
class MyObj{
}

public class Test {
  MyObj example() {
    MyObj o = new MyObj();
    return silly(o);
  }
  
  MyObj silly(MyObj o) {
    if (o != null) 
      return o;
    else 
      return o;
  }
}
```
example() , silly()的字节码为:
```bash
// example()方法
 0:     new     #18             // class MyObj
 3:     dup
 4:     invokespecial #20       // Method MyObj."<init>":()V
 7:     astore_1
 8:     aload_0
 9:     aload_1
10:     invokevirtual #21       // Method silly:(LMyObj;)LMyObj;
13:     areturn

// silly()方法
0: 	aload_1                   // get parameter "MyObj o"
1: 	ifnull    6               // if o != null , go to 6
4: 	aload_1
5: 	areturn
6: 	aload_1                   
7: 	areturn
```
1. aload指令负责把本地变量(类型是引用类型，如果类型是int类型的，则用iload指令)送到栈顶。
对于前四个本地变量可以采用aload_0,aload_1,aload_2,aload_3(它们分别表示第0,1,2,3个引用变量)这种不带参数的简化命令形式。对于第4以上的本地变量将使用aload命令这种形式，在它后面给一参数，以表示是对第几个(从0开始)本类型的本地变量进行操作。
2. astore指令负责把栈顶的值存入本地变量(类型是引用类型，如果类型是int类型的，则用iload指令)。
如果是把栈顶的值存入到前四个本地变量的话，采用的是astore_0,astore_1，astore_2，astore_3(它们分别表示第0,1,2,3个本地引用变量)这种不带参数的简化命令形式。如果是把栈顶的值存入到第四个以上本地变量的话，将使用astore命令这种形式，在它后面给一参数，以表示是把栈顶的值存入到第几个(从0开始)本地变量中。
3. areturn指令从当前方法返回对象引用(把返回值放在栈中，以便它的调用方法取得它)。





## 访问类实例的属性(field)
先看Java代码：
```bash
class A{
  int i; // An instance variable
  int getIt() {
    return i;
  }
  void setIt(int value) {
    i = value;
  }
}
```
上面的get() , set()方法的字节码为：
```bash
// get()方法
0: aload_0
1: getfield  #30    // Field i:I
4: ireturn

// set()方法
0: aload_0
1: iload_1
2: putfield  #30    // Field i:I
5: return
```
对类实例的属性的访问，通过以下2个指令完成：
1. getfield
获取指定类的实例域，并将其值压入栈顶
2. putfield
用栈顶的值为指定的类的实例域赋值

例如:
代码的	“i = value;” 字节码为上面的 “putfield  #30  // Field i:I ”，这是通过常量池来对field的引用。



## 访问类的属性(static field)
先看Java代码：
```bash
class B{
  static int T;
  int getT(){
    return T;
  }
  void setT(int t){
    T = t;
  }
}
```
上面的get() , set()方法的字节码为：
```bash
// getT()方法
0: getstatic  #37        // Field T:I
3: ireturn

// setT()方法
0: iload_1
1: putstatic  #37        // Field T:I
4: return
```
对类属性的访问，通过以下2个指令完成：
1. getstatic
获取指定类的静态域，并将其值压入栈顶
2. putstatic
用栈顶的值为指定的类的静态域赋值


【常量池】
常量池就是该类所用常量的一个有序集合，包括直接常量（String，integer和floating point常量）和对其他类型、字段和方法的符号引用。池中的数据项就像数组一样是通过索引访问的。因为常量池存储了相应类型所用到的所有类型、字段和方法的符号引用，所以它在Java程序的动态连接中起着核心作用。
