---
title: JVM--内存的垃圾回收
categories: Java笔记
---

这篇文章用于整理JVM内存的垃圾回收的知识点。
<!--more-->

---

带着以下3个问题去看垃圾回收这块的知识：

1. 哪些内存需要回收？
2. 什么时候回收？
3. 如何回收？


垃圾回收机制主要是对java堆和方法区的内存回收。


（那java堆和方法区的内存中那些对象可以回收呢？）

## 可达性分析
通过一系列称为“GC Roots”的对象作为起始点，从这些节点开始向下搜索，搜索所走过的路径称为引用链，当一个对象到GC Roots没有任何引用链相连，则这个对象是不可用的。
可作为GC Roots的对象有：

1. 虚拟机栈（帧栈的本地变量）中引用的对象
2. 方法区中类静态属性引用的对象
3. 方法区中常量引用的对象
4. 本地方法栈中JNI引用的对象


## JVM堆内存的分代

<img src="http://img.blog.csdn.net/20171107094619084">



#### 年轻代（Young Generation）

年轻代又划分为3个区：Eden、Survivor0（幸存0区）、Survivor1（幸存1区）。
年轻代空间的特点：

 * 大多数新建的对象都位于Eden区
 * 当Eden区被对象填满时，就会执行Minor GC。并把所有存活下来的对象转移到其中一个survivor区。
 * 一般地,幸存0区和幸存1区中总有一个是空的,当其中一个区被填满时就会再进行Minor GC,就这样还能存活的对象就会在幸存0区和幸存1区之间来回切换。
 * 经过多次GC周期后，仍然存活下来的对象会被转移到年老代内存空间。(一般需要设定一个存活阈值,可以通过参数 -XX:MaxTenuringThreshold 来设置)
 

#### 老年代（Old Generation）
处于该代的java对象都是在年轻代久经考验而存活的对象，一般都是生命周期较长的对象。当年老代的空间被占满时，则不会进行Minor GC，而会触发Major GC/Full GC，回收整个堆内存。


#### 永久代（Perm Generation）
永久代包含了JVM需要的应用元数据，这些元数据描述了在应用里使用的类和方法。注意，永久代不是Java堆内存的一部分。
永久代同样包含了Java SE库的类和方法。永久代的对象在full GC时进行垃圾收集。


## 垃圾收集算法

#### 标记-清除算法
<img src="http://img.blog.csdn.net/20171107104539708">

标记-清除算法（Mark-Sweep），采用从根集合进行扫描，对存活的对象对象标记，标记完毕后，再扫描整个空间中未被标记的对象，进行回收，如上图所示。
<font color=green>优点：</font>

* 不需要进行对象的移动，并且仅对不存活的对象进行处理，在存活对象比较多的情况下极为高效。

<font color=red>缺点：</font>

1. 标记和清除2个过程的效率都不高
2. 会产生内存碎片


#### 复制算法
<img src="http://img.blog.csdn.net/20171107105459419">

复制算法（Copying）将可用内存分为相等的2块，每次只使用其中一块：从根集合扫描，并将存活对象复制到一块新的，没有使用过的空间中。
<font color=green>优点：</font>

 * 当存活的对象比较少时，极为高效（内存分配时也不用考虑内存碎片等情况）

<font color=red>缺点：</font>

 * 内存缩小为原来的一半，代价太高了。


#### 标记-整理算法
<img src="http://img.blog.csdn.net/20171107110126154">

标记-整理算法采用标记-清除算法一样的方式进行对象的标记，但在清除时不同，在回收不存活的对象占用的空间后，会将所有的存活对象往左端空闲空间移动，并更新对应的指针。标记-整理算法是在标记-清除算法的基础上，又进行了对象的移动，因此成本更高，但是却解决了内存碎片的问题。


<font color=orange>年轻代的特点是对象存活率低，因此使用复制算法，而老年代对象存活率高，使用标记-清除或标记-整理算法。</font>

下图展示了minor GC的执行过程：
<img src="http://img.blog.csdn.net/20171107111123400">



## 内存分配与回收策略


#### 对象优先在Edon上分配
代码示例:
```
private final static int _1MB = 1024*1024;
    
/**
 * VM 参数： -verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails 
 *          -XX:SurvivorRatio=8
 *
 */
public static void main(String[] args) {
    byte[] testCase1,testCase2,testCase3,testCase4;
    testCase1 = new byte[2*_1MB];
    testCase2 = new byte[2*_1MB];
    testCase3 = new byte[2*_1MB];
    testCase4 = new byte[4*_1MB]; // 出现一次Minor GC
}
```
运行结果：
```
[GC[DefNew: 6708K->377K(9216K), 0.0036386 secs] 6708K->6521K(19456K), 0.0036788 secs] [Times: user=0.00 sys=0.02, real=0.01 secs] 
Heap
 def new generation   total 9216K, used 4801K [0x32a00000, 0x33400000, 0x33400000)
  eden space 8192K,  54% used [0x32a00000, 0x32e51fa0, 0x33200000)
  from space 1024K,  36% used [0x33300000, 0x3335e580, 0x33400000)
  to   space 1024K,   0% used [0x33200000, 0x33200000, 0x33300000)
 tenured generation   total 10240K, used 6144K [0x33400000, 0x33e00000, 0x33e00000)
   the space 10240K,  60% used [0x33400000, 0x33a00030, 0x33a00200, 0x33e00000)
 compacting perm gen  total 12288K, used 160K [0x33e00000, 0x34a00000, 0x37e00000)
   the space 12288K,   1% used [0x33e00000, 0x33e282d8, 0x33e28400, 0x34a00000)
    ro space 10240K,  44% used [0x37e00000, 0x3827a688, 0x3827a800, 0x38800000)
    rw space 12288K,  52% used [0x38800000, 0x38e54170, 0x38e54200, 0x39400000)
```
我们可以看到，Eden区有8MB，其中使用了4MB（这个是分配给了testCase4）。而老年代使用了6MB（分配给了testCase1、testCase2、testCase3）。
其中的过程是：前6MB的数据优先分配到eden区域，当再想分配4MB时，Eden区空间不足，从而发生Minor GC。GC期间，发现3个2MB的对象又不能全部放入Survivor区，所以只好通过分配担保机制提前转移到老年代去。


#### 大对象直接进入老年代
代码示例:
```
private final static int _1MB = 1024*1024;
/**
 * VM 参数： -verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails 
 *          -XX:SurvivorRatio=8 -XX:PretenureSizeThreshold=3145728
 *
 */
public static void main(String[] args) {
    byte[] allocation;
    allocation = new byte[4 * _1MB];
}

```
通过-XX:PretenureSizeThreshold=3145728，将阈值设置为3MB，从而使得超过3MB的对象直接分配在老年代。
运行结果如下：
```
Heap
 def new generation   total 9216K, used 891K [0x32a00000, 0x33400000, 0x33400000)
  eden space 8192K,  10% used [0x32a00000, 0x32adef58, 0x33200000)
  from space 1024K,   0% used [0x33200000, 0x33200000, 0x33300000)
  to   space 1024K,   0% used [0x33300000, 0x33300000, 0x33400000)
 tenured generation   total 10240K, used 4096K [0x33400000, 0x33e00000, 0x33e00000)
   the space 10240K,  40% used [0x33400000, 0x33800010, 0x33800200, 0x33e00000)
 compacting perm gen  total 12288K, used 160K [0x33e00000, 0x34a00000, 0x37e00000)
   the space 12288K,   1% used [0x33e00000, 0x33e28288, 0x33e28400, 0x34a00000)
    ro space 10240K,  44% used [0x37e00000, 0x3827a688, 0x3827a800, 0x38800000)
    rw space 12288K,  52% used [0x38800000, 0x38e54170, 0x38e54200, 0x39400000)
```
没有触发GC日志，而数据是直接进入老生代的


#### 长期存活的对象将进入老年代
代码示例:
```
 private final static int _1MB = 1024*1024;
    
/**
* VM 参数： -verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails 
*          -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=1
*
*/
public static void main(String[] args) {
    byte[] allocation1,allocation2,allocation3;
    allocation1 = new byte[_1MB / 4];
    // 什么时候进入老年代取决于 XX:MaxTenuringThreshold设置
    allocation2 = new byte[4 * _1MB];
    allocation3 = new byte[4 * _1MB];
    allocation3 = null;
    allocation3 = new byte[4 * _1MB];
}
```
运行结果如下：
```
[GC[DefNew: 4916K->633K(9216K), 0.0035512 secs] 4916K->4729K(19456K), 0.0036283 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
[GC[DefNew: 4893K->0K(9216K), 0.0010622 secs] 8989K->4729K(19456K), 0.0010966 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
Heap
 def new generation   total 9216K, used 4260K [0x32a00000, 0x33400000, 0x33400000)
  eden space 8192K,  52% used [0x32a00000, 0x32e28fd8, 0x33200000)
  from space 1024K,   0% used [0x33200000, 0x33200088, 0x33300000)
  to   space 1024K,   0% used [0x33300000, 0x33300000, 0x33400000)
 tenured generation   total 10240K, used 4728K [0x33400000, 0x33e00000, 0x33e00000)
   the space 10240K,  46% used [0x33400000, 0x3389e3d8, 0x3389e400, 0x33e00000)
 compacting perm gen  total 12288K, used 160K [0x33e00000, 0x34a00000, 0x37e00000)
   the space 12288K,   1% used [0x33e00000, 0x33e282c8, 0x33e28400, 0x34a00000)
    ro space 10240K,  44% used [0x37e00000, 0x3827a688, 0x3827a800, 0x38800000)
    rw space 12288K,  52% used [0x38800000, 0x38e54170, 0x38e54200, 0x39400000)

```
allocation1需要256KB内存，Survivor可以容纳。因为MaxTenuringThreshold=1，allocation1对象在第二次GC时进入老年代，新生代的from区已经变成0KB。

