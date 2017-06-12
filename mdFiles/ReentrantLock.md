---
title: Java的ReentrantLock
categories: Java笔记
---

Java的ReentrantLock(重入锁),顾名思义，就是支持重进入的锁，它表示该锁能够支持一个线程对资源的重复加锁。除此之外，该锁的还支持获取锁时的公平和非公平性选择。
本文将从实现角度分析Java的ReentrantLock。

<!--more-->

---

前面我们自定义了同步组件 Mutex ，现在我们考虑如下情景：
当一个线程调用Mutex的lock()方法获取锁之后，如果再次调用lock()方法，则该线程将会被自己所阻塞，原因是Mutex在实现`tryAcquire(int acquires)`方法时没有考虑占有锁的线程再次获取锁的场景，而在调用`tryAcquire(int acquires)`方法时返回了false，导致该线程被阻塞。

总结为一句话：**Mutex是一个不支持重进入的锁。**


## ReentrantLock重进入的实现

重进入是指任意线程在获取到锁之后能够再次获取该锁而不会被锁所阻塞，该特性的实现需要解决以下两个问题：
1. 线程再次获取锁。
锁需要去识别获取锁的线程是否为当前占据锁的线程，如果是，则再次成功获取。
2. 锁的最终释放。
线程重复n次获取了锁，随后在第n次释放该锁后，其他线程能够获取到该锁。锁的最终释放要求锁对于获取进行计数自增，计数表示当前锁被重复获取的次数，而锁被释放时，计数自减，当计数等于0时表示锁已经成功释放。


ReentrantLock是通过组合自定义同步器来实现锁的获取与释放。

#### 非公平的实现（默认的）

```bash
final boolean nonfairTryAcquire(int acquires) {
  final Thread current = Thread.currentThread();
  int c = getState();
  if (c == 0) {
    if (compareAndSetState(0, acquires)) {
      setExclusiveOwnerThread(current);
      return true;
    }
  }
  else if (current == getExclusiveOwnerThread()) {
    int nextc = c + acquires;
    if (nextc < 0) // overflow
      throw new Error("Maximum lock count exceeded");
    setState(nextc);
    return true;
  }
  return false;
}
```
该方法增加了再次获取同步状态的处理逻辑：
通过判断当前线程是否为获取锁的线程来决定获取操作是否成功，如果是获取锁的线程再次请求，则将同步状态值进行增加并返回true，表示获取同步状态成功。

对应的释放同步状态的方法：
```bash
protected final boolean tryRelease(int releases) {
  int c = getState() - releases;
  if (Thread.currentThread() != getExclusiveOwnerThread())
    throw new IllegalMonitorStateException();
  boolean free = false;
  if (c == 0) {
    free = true;
    setExclusiveOwnerThread(null);
  }
  setState(c);
  return free;
}
```
如果该锁被获取了n次，那么前(n-1)次tryRelease(int releases)方法必须返回false，而只有同步状态完全释放了，才能返回true。可以看到，该方法将同步状态是否为0作为最终释放的条件，当同步状态为0时，将占有线程设置为null，并返回true，表示释放成功。



#### 公平的实现

公平性与否是针对获取锁而言的，如果一个锁是公平的，那么锁的获取顺序就应该符合请求的绝对时间顺序，也就是FIFO。

回顾前面介绍的nonfairTryAcquire(int acquires)方法，对于非公平锁，只要CAS设置同步状态成功，则表示当前线程获取了锁，而公平锁则不同：
```bash
protected final boolean tryAcquire(int acquires) {
  final Thread current = Thread.currentThread();
  int c = getState();
  if (c == 0) {
    if (isFirst(current) && compareAndSetState(0, acquires)) {
      setExclusiveOwnerThread(current);
      return true;
    }
  }
  else if (current == getExclusiveOwnerThread()) {
    int nextc = c + acquires;
    if (nextc < 0)
      throw new Error("Maximum lock count exceeded");
    setState(nextc);
    return true;
  }
  return false;
}
```
该方法与nonfairTryAcquire(int acquires)比较，唯一不同的位置为判断条件多了`isFirst(Thread current)`方法，即加入了同步队列中当前节点是否前驱节点的判断，如果该方法返回true，则表示当前线程是最早请求获取锁的，因此可以获取锁，否则需要等待前驱线程获取并释放锁之后才能继续获取锁。





