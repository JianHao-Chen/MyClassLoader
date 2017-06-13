---
title: Java的读写锁 ReentrantReadWriteLock
categories: Java笔记
---

读写锁在同一时刻可以允许多个读线程访问，但是在写线程访问时，所有的读线程和其他写线程均被阻塞。读写锁维护了一对锁，一个读锁和一个写锁，通过分离读锁和写锁，使得并发性相比一般的排他锁有了很大提升。

<!--more-->

---

## 读写状态的设计
读写锁同样依赖自定义同步器来实现同步功能，而读写状态就是其同步器的同步状态。

回想ReentrantLock中自定义同步器的实现，同步状态表示锁被一个线程重复获取的次数，而读写锁的自定义同步器需要在同步状态（一个整型变量）上维护多个读线程和一个写线程的状态，使得该状态的设计成为读写锁实现的关键。

如果在一个整型变量上维护多种状态，就一定需要“按位切割使用”这个变量，读写锁将变量切分成了两个部分，高16位表示读，低16位表示写，划分方式如下图所示：
<img src="http://img.blog.csdn.net/20170613103956052">

我们通过查看ReentrantReadWriteLock的内部类Sync的部分源码，分析上述的int变量在Sync中是如何表示及维护的：
```bash
static abstract class Sync extends AbstractQueuedSynchronizer {

  /*
  * Read vs write count extraction constants and functions.
  * Lock state is logically divided into two shorts: The lower
  * one representing the exclusive (writer) lock hold count,
  * and the upper the shared (reader) hold count.
  */
  static final int SHARED_SHIFT   = 16;
  static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
  static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
  static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
  
  /** Returns the number of shared holds represented in count  */
  static int sharedCount(int c)      { return c >>> SHARED_SHIFT; }
  /** Returns the number of exclusive holds represented in count  */
  static int exclusiveCount(int c)   { return c & EXCLUSIVE_MASK; }
  
  ....
}
```
可以看到，是通过位运算来获取当前读写锁的状态。


## 写锁的获取与释放
写锁是一个支持重进入的排它锁。

获取写锁的逻辑：
* 如果当前线程已经获取了写锁，则增加写状态。
* 如果当前线程在获取写锁时，读锁已经被获取（读状态不为0）或者该线程不是已经获取写锁的线程，则当前线程进入等待状态。

#### 获取写锁
```bash
protected final boolean tryAcquire(int acquires) {
  Thread current = Thread.currentThread();
  int c = getState();
  int w = exclusiveCount(c);
  if (c != 0) {
    // 如果 c!=0 && w==0，表示 shared count != 0(读锁已获取)
    if (w == 0 || current != getExclusiveOwnerThread())
      return false;
    if (w + exclusiveCount(acquires) > MAX_COUNT)
      throw new Error("Maximum lock count exceeded");
  }
  if ((w == 0 && writerShouldBlock(current)) || !compareAndSetState(c, c + acquires))
    return false;
  setExclusiveOwnerThread(current);
  return true;
}
```
该方法除了重入条件（当前线程为获取了写锁的线程）之外，增加了一个读锁是否存在的判断。

#### 释放写锁
```bash
protected final boolean tryRelease(int releases) {
  int nextc = getState() - releases;
  if (Thread.currentThread() != getExclusiveOwnerThread())
    throw new IllegalMonitorStateException();
  if (exclusiveCount(nextc) == 0) {
    setExclusiveOwnerThread(null);
    setState(nextc);
    return true;
  } else {
    setState(nextc);
    return false;
  }
}
```
写锁的释放与ReentrantLock的释放过程基本类似，每次释放均减少写状态，当写状态为0时表示写锁已被释放，从而等待的读写线程能够继续访问读写锁，同时前次写线程的修改对后续读写线程可见。



## 读锁的获取与释放
读锁是一个支持重进入的共享锁，它能够被多个线程同时获取。

获取读锁的逻辑：
* 在没有其他写线程访问（或者写状态为0）时，读锁总会被成功地获取，而所做的也只是（线程安全的）增加读状态。如果当前线程已经获取了读锁，则增加读状态。
* 如果当前线程在获取读锁时，写锁已被其他线程获取，则进入等待状态。




#### 获取读锁

获取读锁的实现从Java 5到Java 6变得复杂许多，主要原因是新增了一些功能，例如getReadHoldCount()方法，作用是返回当前线程获取读锁的次数。读状态是所有线程获取读锁次数的总和，而每个线程各自获取读锁的次数只能选择保存在ThreadLocal中，由线程自身维护，这使获取读锁的实现变得复杂。

以下是经过部分删减的代码：
```bash
...
for (;;) {
  int c = getState();
  int nextc = c + (1 << 16);
  if (nextc < c)
    throw new Error("Maximum lock count exceeded");
  if (exclusiveCount(c) != 0 && owner != Thread.currentThread())
    return -1;
  if (compareAndSetState(c, nextc))
    return 1;
}
...
```
在tryAcquireShared(int unused)方法中，如果其他线程已经获取了写锁，则当前线程获取读锁失败，进入等待状态。如果当前线程获取了写锁或者写锁未被获取，则当前线程（线程安全，依靠CAS保证）增加读状态，成功获取读锁。

#### 释放读锁

读锁的每次释放（线程安全的，可能有多个读线程同时释放读锁）均减少读状态，减少的值是（1<<16）。
以下是经过部分删减的代码：
```bash
protected final boolean tryReleaseShared(int unused) {
  ......
  for (;;) {
    int c = getState();
    int nextc = c - SHARED_UNIT;        // SHARED_UNIT就是 1 << 16
    if (compareAndSetState(c, nextc))
      return nextc == 0;
  }
}
```
