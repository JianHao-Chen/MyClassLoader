---
title: Java同步器 AQS的实现
categories: Java笔记
---

本文将从实现角度分析同步器是如何完成线程同步的，主要包括：同步队列、独占式同步状态获取与释放、共享式同步状态获取与释放、超时获取同步状态等同步器的核心数据结构与模板方法。

<!--more-->

---

同步器依赖内部的同步队列（一个FIFO双向队列）来完成同步状态的管理：
* 当前线程获取同步状态失败时，同步器会把当前线程以及等待状态等信息构造成为一个节点(Node)并将其加入同步队列，同时阻塞当前线程。
* 当同步状态释放时，会把首节点中的线程唤醒，使其再次尝试获取同步状态。

## 同步队列的节点
同步队列的节点(Node)是AbstractQueuedSynchronizer的内部类，用于保存获取同步状态失败的线程引用、等待状态以及前驱和后继结点。
```bash
static final class Node {

  /** Marker to indicate a node is waiting in shared mode */
  static final Node SHARED = new Node();
  
  /** Marker to indicate a node is waiting in exclusive mode */
  static final Node EXCLUSIVE = null;
  
/*
* 由于在同步队列中等待的线程等待超时或者被中断，需要从同步队列
* 中取消等待，节点进入该状态将不会变化 
*/
  static final int CANCELLED =  1;
  
/*
* 后继结点的线程处于等待状态，而当前节点的线程如果释放了同步状态或者被取消，将会通知后继结点，使后继节点的线程得以运行 
*/
  static final int SIGNAL = -1;
  
/*
* 节点在等待队列中，节点线程等待在CONDITION上，当其他线程对CONDITION
* 调用了signal()方法后，该节点将会从等待队列中转移到同步队列中，
* 加入到对同步状态的获取中
*/
  static final int CONDITION = -2;
       
  static final int PROPAGATE = -3;

  /* 等待状态 ， 初始值为 0 */
  volatile int waitStatus;

  /* 前驱节点，当节点加入同步队列时被设置(尾部添加) */
  volatile Node prev;
  
  /* 后继节点 */
  volatile Node next;
        
  volatile Thread thread;

  /* 
  *  等待队列中的后继节点。
  *  因为CONDITION只能在独占式下使用，如果在共享模式下，这个field是
  *  不用的，于是这个字段可以作为一个 SHARED 常量，也就是说节点类型
  *  (独占和共享)和等待队列中的后继节点共用一个字段 
  */
  Node nextWaiter;
```

## 同步队列的结构

同步器包含2个节点类型的引用，一个指向头节点，另一个指向尾节点。
```bash
private transient volatile Node head;

private transient volatile Node tail;
```

同步队列的结构如下图所示：
<img src="http://img.blog.csdn.net/20170611174422600">
图中的compareAndSetTail(Node expect, Node update)方法是一个基于CAS的设置尾节点的方法。
```bash
private static final Unsafe unsafe = Unsafe.getUnsafe();
private static final long tailOffset;
...

static {
  try {
    ..
    tailOffset = unsafe.objectFieldOffset
      (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
    ..
  }
  catch (Exception ex) { throw new Error(ex); }
}

...

private final boolean compareAndSetTail(Node expect, Node update) {
  return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
}
```


## 独占式同步状态获取与释放

#### 同步器的acquire(int arg)方法
通过调用同步器的 acquire(int arg) 方法可以获取同步状态，这个方法对中断不敏感。
这个方法可以用于实现Lock接口的lock()方法。
```bash
public final void acquire(int arg) {
  if (!tryAcquire(arg) &&
    acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
    selfInterrupt();
}
```
该方法的执行逻辑是：
1. 调用自定义的同步器实现的tryAcquire(int arg)方法，该方法保证线程安全的获取同步状态
2. 如果同步状态获取失败，进入addWaiter(Node node)方法将节点加入到同步队列的尾部
3. 调用acquireQueued(Node node, int arg)方法，使得该节点以“死循环”的方式获取同步状态。如果获取不到就阻塞节点中的线程。


#### 同步器的addWaiter(Node node)方法
```bash
private Node addWaiter(Node mode) {
  Node node = new Node(Thread.currentThread(), mode);
  // Try the fast path of enq; backup to full enq on failure
  Node pred = tail;
  if (pred != null) {
    node.prev = pred;
    if (compareAndSetTail(pred, node)) {
      pred.next = node;
      return node;
    }
  }
  enq(node);
  return node;
}
```
addWaiter()方法通过使用compareAndSetTail(Node expect, Node update)方法来确保节点被线程安全添加。

再看enq(Node node)方法：
```bash
private Node enq(final Node node) {
  for (;;) {
    Node t = tail;
    if (t == null) { // Must initialize
      if (compareAndSetHead(new Node()))
        tail = head;
    } else {
      node.prev = t;
      if (compareAndSetTail(t, node)) {
        t.next = node;
        return t;
      }
    }
  }
}
```
在enq(final Node node)方法里面，同步器通过死循环来保证节点的正确添加，即在死循环中，只有通过CAS将节点设置为尾节点之后，当前线程才能从该方法返回。


#### 同步器的acquireQueued(final Node node, int arg)方法
```bash
final boolean acquireQueued(final Node node, int arg) {
  boolean failed = true;
  try {
    boolean interrupted = false;
    for (;;) {
      final Node p = node.predecessor();
      if (p == head && tryAcquire(arg)) {
        setHead(node);
        p.next = null; // help GC
        failed = false;
        return interrupted;
      }
      if (shouldParkAfterFailedAcquire(p, node) &&
          parkAndCheckInterrupt())
            interrupted = true;
    }
  } finally {
      if (failed)
        cancelAcquire(node);
  }
}
```
进入acquireQueued(final Node node, int arg)方法时，表示节点已经加入到同步队列中，在此方法里面，当前线程在“死循环”中尝试获取同步状态，而只有前驱节点是头节点才能尝试获取同步状态，原因有2：
1. 头节点是成功获取到同步状态的节点，而头节点的线程释放了同步状态之后，将会唤醒其后继节点，后继节点的线程被唤醒后需要检查自己的前驱节点是否头节点。
2. 维护同步队列的FIFO原则。在该方法中，节点自旋获取同步状态的行为如下图所示：
<img src="http://img.blog.csdn.net/20170611221517677">
由于非首节点线程前驱节点出队或者被中断而从等待状态返回，随后检查自己的前驱是否头节点，如果是，尝试获取同步状态。

再看shouldParkAfterFailedAcquire(Node pred, Node node) 方法：
```bash
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
  int s = pred.waitStatus;
  if (s < 0)
    /*
    * This node has already set status asking a release
    * to signal it, so it can safely park.
    */
    return true;
  if (s > 0) {
    /*
    * Predecessor was cancelled. Skip over predecessors and
    * indicate retry.
    */
    do {
      node.prev = pred = pred.prev;
    } while (pred.waitStatus > 0);
    pred.next = node;
  } else {
      /*
      * Indicate that we need a signal, but don't park yet. Caller will need to
      * retry to make sure it cannot acquire before parking.
      */
      compareAndSetWaitStatus(pred, 0, Node.SIGNAL);
    }
  return false;
}
```
这个方法首先获取当前节点的前驱节点的状态，然后判断：
1. 如果状态值 < 0 (即 Node.SIGNAL 或 Node.CONDITION 或 Node.PROPAGATE)
那么就返回true，这就意味着当前线程将会被阻塞。
2. 如果状态值 > 0 (即 Node.CANCELLED)
前驱节点被取消了，跳过状态为CANCELLED的前驱节点(可能会多个)，并返回false，使得在外面的方法acquireQueued()中会重试获取同步状态。
3. 如果状态值 = 0 (即 初始状态)
将前驱节点的状态设置为 Node.SIGNAL ，然后返回 false，使得在外面的方法acquireQueued()中会重试获取同步状态。


#### 同步器的release(int arg)方法
通过调用同步器的release(int arg)方法，可以释放同步状态，该方法在释放了同步状态之后，会唤醒其后继节点（进而使后继节点重新尝试获取同步状态）。

```bash
public final boolean release(int arg) {
  if (tryRelease(arg)) {
    Node h = head;
    if (h != null && h.waitStatus != 0)
      unparkSuccessor(h);
      return true;
  }
  return false;
}
```
其中方法unparkSuccessor(Node node)就是用于选择当前节点的后继节点(如果存在)，然后调用LockSupport的unpark(Thread thread)方法唤醒后继节点。


## 独占式超时获取同步状态、可中断获取同步状态

在开始介绍超时获取同步状态之前，先介绍一下AQS的可中断的同步状态获取。因为超时获取是可中断获取的“加强版”。

#### 可中断获取同步状态

在Java 5之前，当一个线程获取不到锁而被阻塞在synchronized之外时，对该线程进行中断操作，此时该线程的中断标志位会被修改，但线程依旧会阻塞在synchronized上，等待着获取锁。

在Java 5中，同步器提供了`acquireInterruptibly(int arg)`方法，这个方法在等待获取同步状态时，如果当前线程被中断，会立刻返回，并抛出InterruptedException。

先看acquireInterruptibly(int arg)方法：
```bash
public final void acquireInterruptibly(int arg) throws InterruptedException {
  if (Thread.interrupted())
    throw new InterruptedException();
  if (!tryAcquire(arg))
    doAcquireInterruptibly(arg);
}
```
然后看doAcquireInterruptibly(int arg)方法：
```bash
private void doAcquireInterruptibly(int arg) throws InterruptedException {
  final Node node = addWaiter(Node.EXCLUSIVE);
  try {
    for (;;) {
      final Node p = node.predecessor();
      if (p == head && tryAcquire(arg)) {
        setHead(node);
        p.next = null; // help GC
        return;
      }
      if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
        break;
    }
  } catch (RuntimeException ex) {
      cancelAcquire(node);
      throw ex;
  }
  // Arrive here only if interrupted
  cancelAcquire(node);
  throw new InterruptedException();
}
```
可以看到，这个方法与独占式获取同步状态(屏蔽中断)中的acquireQueued(final Node node, int arg)方法是十分相似的，不同之处在于：
**这个方法在parkAndCheckInterrupt()检查完线程中断情况之后，会跳出这个“死循环”，对当前节点取消获取同步状态，抛出InterruptedException。**


#### 超时获取同步状态

通过调用同步器的`doAcquireNanos(int arg,long nanosTimeout)`方法可以超时获取同步状态，即在指定的时间段内获取同步状态，如果获取到同步状态则返回true，否则，返回false。该方法提供了传统Java同步操作（比如synchronized关键字）所不具备的特性。

```bash
private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
  long lastTime = System.nanoTime();
  final Node node = addWaiter(Node.EXCLUSIVE);
  try {
    for (;;) {
      final Node p = node.predecessor();
      if (p == head && tryAcquire(arg)) {
        setHead(node);
        p.next = null; // help GC
        return true;
      }
      if (nanosTimeout <= 0) {
        cancelAcquire(node);
        return false;
      }
      
      if (nanosTimeout > spinForTimeoutThreshold && shouldParkAfterFailedAcquire(p, node))
          LockSupport.parkNanos(this, nanosTimeout);
          long now = System.nanoTime();
          nanosTimeout -= now - lastTime;
          lastTime = now;
          if (Thread.interrupted())
            break;
    }
  } catch (RuntimeException ex) {
      cancelAcquire(node);
      throw ex;
    }
  // Arrive here only if interrupted
  cancelAcquire(node);
  throw new InterruptedException();
}
```
同样，这个方法与与可中断获取同步状态中的doAcquireInterruptibly(int arg)方法是十分相似的，不同之处在同步状态获取失败的处理上：

如果当前线程获取同步状态失败，则判断是否超时（nanosTimeout小于等于0表示已经超时），如果没有超时，重新计算超时间隔nanosTimeout，然后使当前线程等待nanosTimeout纳秒（当已到设置的超时时间，该线程会从LockSupport.parkNanos(Objectblocker,long nanos)方法返回）。

如果nanosTimeout小于等于spinForTimeoutThreshold（1000纳秒）时，将不会使该线程进行超时等待，而是进入快速的自旋过程。原因在于，非常短的超时等待无法做到十分精确，如果这时再进行超时等待，相反会让nanosTimeout的超时从整体上表现得反而不精确。因此，在超时非常短的场景下，同步器会进入无条件的快速自旋。





