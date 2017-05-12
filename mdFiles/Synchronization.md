---
title: synchronized 对应的字节码
categories: JDK源码
---

根据synchronized关键字的用法不同，生成的字节码也有不同哦。

<!--more-->

---

先看下面的Java代码：
```bash
void onlyMe(Foo f) {
  synchronized(f) {
    doSomething();
  }
}
```
方法onlyMe(Foo)的字节码为：
```bash
descriptor: (Llearn/compile/Foo;)V
flags:
Code:
  stack=2, locals=3, args_size=2
0: aload_1               // Push f
1: dup                   // Duplicate it on the stack
2: astore_2              // Store duplicate in local variable 2
3: monitorenter          // Enter the monitor associated with f
4: aload_0               // Holding the monitor, pass this and...
5: invokevirtual #16     // ...call doSomething()V
8: aload_2               // Push local variable 2 (f)
9: monitorexit           // Exit the monitor associated with f
10: goto 16              // Complete the method normally
13: aload_2              // Push local variable 2 (f)
14: monitorexit          // Be sure to exit the monitor!
15: athrow	
16: return               // Return in the normal case
Exception table:
  from    to  target type
    4    10    13   any
   13    15    13   any
```
可以看到，这种使用synchronized的方式，会导致JVM使用monitorenter、monitorexit指令。



再看另一种使用synchronized的方式：
```bash
synchronized void theOnly(){
  doSomething();
}
```
theOnly()方法的字节码为：
```bash
descriptor: ()V
flags: ACC_SYNCHRONIZED
Code:
  stack=1, locals=1, args_size=1
  0: aload_0
  1: invokevirtual #16      // Method doSomething:()V
  4: return
```
这里synchronized的实现，没有用 monitorenter / monitorexit指令，而方法的 flags 为 ACC_SYNCHRONIZED，具体的加锁、释放锁将由JVM隐式进行。