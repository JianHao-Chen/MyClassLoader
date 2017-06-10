---
title: Java同步器 AQS的介绍
categories: Java笔记
---

本文将介绍Java的队列同步器 AbstractQueuedSynchronizer。

<!--more-->

---

## 什么是同步器？同步器的作用？
* 队列同步器 AbstractQueuedSynchronizer，是用来构建锁或其他同步组件的基础框架。
* AQS 使用一个int成员变量表示同步状态，通过内置的FIFO队列来完成资源获取线程的排队工作。
* 同步器自身没有实现任何的同步接口，它仅仅是定义了若干同步状态获取和释放的方法来供自定义同步组件使用
* 同步器既支持独占式地获取同步状态，也支持共享式的获取同步状态，可以用于实现不同的同步组件（ReentrantLock、ReentrantReadWriteLock、CountDownLatch）

<font color=red>**同步器和锁的关系：**</font>
1. 锁是面向使用者的，它定义了使用者与锁交换的接口（比如可以允许2个线程并行访问），隐藏了实现细节；
2. 同步器面向的是锁的实现者，它简化了锁的实现方式，屏蔽了同步状态管理、线程的排队、等待与唤醒等底层操作。


## 同步器的接口
同步器的设计是基于模板方法的，所以使用者需要继承同步器并重写指定的方法，随后将同步器组合在自定义同步组件的实现中，并调用同步器提供的模板方法，而这些模板方法将会调用使用者重写的方法。

重写同步器指定的方法时，需要使用同步器提供的如下3个方法来访问或修改同步状态。
* getState() ： 获取当前同步状态
* setState() ： 设置当前同步状态
* compareAndSetState() ： 使用CAS设置当前状态，该方法能保证状态设置的原子性。


#### 同步器可重写的方法
```bash
/*
*  独占式获取同步状态，实现该方法需要查询当前状态并判断同步状态是否符合预期，然后进行CAS设置同步状态
*/
protected boolean tryAcquire(int arg)


/*
*  独占式释放同步状态，等待获取同步状态的线程将有机会获取同步状态
*/
protected boolean tryRelease(int arg)


/*
*  共享式获取同步状态，返回>=0的值，表示获取成功，否则获取失败
*/
protected int tryAcquireShared(int arg)


/*
*  共享式释放同步状态
*/
protected boolean tryReleaseShared(int arg)


/*
*  当前同步器是否在独占模式下被线程占用，一般该方法表示是否被当前线程所独占
*/
protected boolean isHeldExclusively()
```

#### 同步器提供的模板方法
```bash
/*
*  独占式获取同步状态，如果当前线程获取同步状态成功，则由该方法返回，否则将会进入同步队列等待，该方法将会调用重写的
*  tryAcquire(int arg)方法
*/
public final void acquire(int arg)


/*
*  与acquire(int arg)相同，但是该方法响应中断，当前线程未获取到同步状态而进入同步队列中，如果当前线程被中断，则该
*  方法会抛出InterruptedException并返回
*/
public final void acquireInterruptibly(int arg)


/*
*  在acquireInterruptibly(int arg)基础上加上超时限制，如果当前线程在超时时间内没有获取到同步状态，那么会返回
*  false，否则返回true
*/
public final boolean tryAcquireNanos(int arg, long nanosTimeout)


/*
*  独占式的释放同步状态，该方法会释放同步状态之后，将同步队列中第一个节点包含的线程唤醒
*/
public final boolean release(int arg)


/*
*  共享式的获取同步状态，如果当前线程未获取到同步状态，将会进入同步队列等待，与独占式获取的主要区别是在同一时刻
*  可以有多个线程获取到同步状态
*/
public final void acquireShared(int arg)


/*
*  与acquireShared(int arg)相同，该方法响应中断
*/
public final void acquireSharedInterruptibly(int arg)


/*
*  在acquireSharedInterruptibly(int arg)基础上增加了超时限制
*/
public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)


/*
*  共享式的释放同步状态
*/
public final boolean releaseShared(int arg) 


/*
*  获取等待在同步队列上的线程集合
*/
public final Collection<Thread> getQueuedThreads()
```
以上的模板方法可以分为3类：独占式获取和释放同步状态、共享式获取和释放同步状态、查询同步队列中等待的线程。



## 自定义同步组件--独占锁
下面将以一个独占锁的示例来了解同步器的工作原理。这里独占锁就是在同一时刻只能有一个线程获取到锁，而其他获取锁的线程只能处于同步队列中等待，只有获取到锁的线程释放了锁，后继的线程才能获取到锁。
```bash
public class Mutex implements Lock{

  // 静态内部类，自定义同步器
  private static class Sync extends AbstractQueuedSynchronizer {
  
    // 是否处于占用状态
    protected boolean isHeldExclusively() {
      return getState() == 1;
    }

     // 当状态为0时获取锁
    public boolean tryAcquire(int acquires) {
      if (compareAndSetState(0, 1)) {
        setExclusiveOwnerThread(Thread.currentThread());
        return true;
      }
      return false;
    }

     // 释放锁，将状态设置为0
    protected boolean tryRelease(int releases) {
      if (getState() == 0) 
        throw new IllegalMonitorStateException();
      setExclusiveOwnerThread(null);
      setState(0);
      return true;
    }

     // 返回一个Condition，每个condition都包含一个condition队列
    Condition newCondition() { return new ConditionObject(); }
   }

   // 仅需要将操作代理到Sync上即可
   private final Sync sync = new Sync();
   public void lock()                { sync.acquire(1); }
   public boolean tryLock()          { return sync.tryAcquire(1); }
   public void unlock()              { sync.release(1); }
   public Condition newCondition()   { return sync.newCondition(); }
   public boolean isLocked()         { return sync.isHeldExclusively(); }
   public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
   
   public void lockInterruptibly() throws InterruptedException {
     sync.acquireInterruptibly(1);
   }
   public boolean tryLock(long timeout, TimeUnit unit)
       throws InterruptedException {
     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
   }
 }
```








