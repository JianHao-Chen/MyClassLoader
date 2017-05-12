---
title: 方法调用的字节码
categories: JDK源码
---

本文将探讨Java中方法调用的相关字节码，根据方法性质的不同，将会调用不同的字节码指令哦。

<!--more-->

---

## 实例方法的调用 -- invokevirtual指令

```bash
int addTwo(int i,int j){
  return i+j;
}
	
int add12And13(){
  return addTwo(12,13);
}
```
对应的字节码为：
```bash
// add12And13()方法:
0  aload_0              // Push local variable 0 (this)
1  bipush 12            // Push int constant 12
3  bipush 13            // Push int constant 13
5  invokevirtual #4     // Method Example.addtwo(II)I
8  ireturn              // Return int on top of operand stack;
                        // it is the int result of addTwo()
                        
// addTwo(int i,int j) 方法:
0  iload_1              //push i
1  iload_2              //push j
2  iadd                 // i+j
3  ireturn              // return (i+j)
```
1. add12And13()方法依次push reference “this”，constant 12，13，当addTwo()的frame创建以后，刚才push的3个值，会成为addTwo()的局部变量表中的初始值，因此我们看到addTwo()一开始的iload_1，iload_2,就是从局部变量表读取。

2. 当addTwo()返回时，他的返回值push到add12And13()的frame中的操作数栈。并且将控制权交给add12And13()。

3. invokevirtual #4，其中 #4是对实例方法的符号引用，保存在常量池。



## 初始化方法、私有方法、父类方法的调用 -- invokespecial指令
看以下Java代码：
```bash
public class showInvokeSpecial {

  private int privateMethod(int i){return 0;}
  public void callprivate(){
    privateMethod(1);
  }
  
  public void callInit(){
    new showInvokeSpecial();
  }
  
  public void callSuper(){
    super.toString();
  }
}
```

callInit()方法的字节码及常量池中有关的常量：
```bash
0: new           #1       // class learn/compile/showInvokeSpecial
3: invokespecial #19      // Method "<init>":()V
6: return
         
 constant pool:
  #1 = Class              #2    // showInvokeSpecial
  #2 = Utf8               showInvokeSpecial
   		
  #19 = Methodref         #1.#9    // showInvokeSpecial."<init>":()V
  #9 = NameAndType        #5:#6    // "<init>":()V
   		
  #5 = Utf8               <init>
  #6 = Utf8               ()V
```


callprivate()方法的字节码及常量池中有关的常量：
```bash
0: aload_0
1: iconst_1
2: invokespecial #21    // Method privateMethod:(I)I
5: pop
6: return
         
 constant pool:         
  #21 = Methodref          #1.#22         // showInvokeSpecial.privateMethod:(I)I
  #22 = NameAndType        #14:#15        // privateMethod:(I)I
  #14 = Utf8               privateMethod
  #15 = Utf8               (I)I
```

callSuper()方法的字节码及常量池中有关的常量：

```bash
0: aload_0
1: invokespecial #24        // Method java/lang/Object.toString:()Ljava/lang/String;
4: pop
5: return
         
constant pool:
  #24 = Methodref          #3.#25      // java/lang/Object.toString:()Ljava/lang/String;
  #3 = Class               #4          // java/lang/Object
  #4 = Utf8                java/lang/Object
  #25 = NameAndType        #26:#27     // toString:()Ljava/lang/String;
  #26 = Utf8               toString
  #27 = Utf8               ()Ljava/lang/String;
```

可以看到， 对于 初始化方法、私有方法、父类方法都是通过invokespecial指令调用的。
