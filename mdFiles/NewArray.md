---
title: 创建数组
categories: JDK源码
---

本文介绍几个与创建数组有关的字节码。

<!--more-->

---

## newarray 创建指定原始类型的数组

先看下面的Java代码：

```bash
void createBuffer() {
  int buffer[];
  int bufsz = 100;
  int value = 12;
  buffer = new int[bufsz];
  buffer[10] = value;
  value = buffer[11];
}
```
这段代码的字节码为：
```bash
 0:    bipush        100    // Push int constant 100 (bufsz)
 2:    istore_2             // Store bufsz in local variable 2	
 3:    bipush        12     // Push int constant 12 (value)
 5:    istore_3             // Store value in local variable 3
 6:    iload_2              // Push bufsz...
 7:    newarray  int        // ...and create new int array of that length
 9:    astore_1             // Store new array in buffer
10:    aload_1              // Push buffer
11:    bipush        10     // Push int constant 10
13:    iload_3              // Push value
14:    iastore              // Store value at buffer[10]
15:    aload_1              // Push buffer
16:    bipush        11     // Push int constant 11
18:    iaload               // Push value at buffer[11]...
19:    istore_3             // ...and store it in value
20:    return
```
* newarray  int 用于创建一个 int 类型的数组,数组长度的值取当前栈顶元素的值，并将其引用值压入栈顶
* iastore 用于把当前栈顶int元素的值保存在数组的指定索引位置
* iaload  用于将int型数组指定索引的值推送至栈顶



## anewarray 创建引用型数组

Java代码如下：

```bash
void createThreadArray() {
  Thread threads[];
  int count = 10;
  threads = new Thread[count];
  threads[0] = new Thread();
}
```
这段代码的字节码为：

```bash
 0:  bipush     10           // Push int constant 10
 2:  istore_2                // Initialize count to that
 3:  iload_2                 // Push count, used by anewarray
 4:  anewarray  #21          // Create new array of class Thread
 7:  astore_1                // Store new array in threads
 8:  aload_1                 // Push value of threads
 9:  iconst_0                // Push int constant 0
10:  new        #21          // Create instance of class Thread
13:  dup                     // Make duplicate reference...
14:  invokespecial #23       // ...for Thread's constructor
                             // Method java/lang/Thread."<init>":()V
17:  aastore                 // Store new Thread in array at 0
18:  return
```
* anewarray #21 用于创建一个 Thread 类型的数组,数组长度的值取当前栈顶元素的值，并将其引用值压入栈顶
* aastore 用于把当前栈顶引用类型元素保存在数组的指定索引位置


## multianewarray 创建指定类型和指定维度的多维数组

Java代码如下：

```bash
int[][][] create3DArray() {
  int grid[][][];
  grid = new int[10][5][];
  return grid;
}
```
这段代码的字节码为：
```bash
0:  bipush    10                // Push int 10 (dimension one)
2:  iconst_5                    // Push int 5 (dimension two)
3:  multianewarray #29,  2      // Class [[[I, a three-dimensional int array;
                                //  only create the first two dimensions
7:  astore_1                    // Store new array...
8:  aload_1                     // ...then prepare to return it
9:  areturn
```
注意：
创建指定类型和指定维度的多维数组（执行该指令时，操作栈中必须包含各维度的长度值），并将其引用值压入栈顶

