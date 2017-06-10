---
title: Java的Lock接口
categories: Java笔记
---

在Java SE 5 之前，Java程序是靠synchronized关键字实现锁功能的。后来，Lock接口出现了，它除了提供同步功能之外，还具备了synchronized不具备的如可中断的获取锁，超时获取锁等多种同步特性。

<!--more-->

---

#### Lock接口的特性
Lock接口提供的synchronized关键字所不具备的主要特性：
1. 尝试非阻塞地获取锁
当前线程尝试获取锁，如果这一时刻锁没有被其他线程获取到，则成功获取并持有锁
2. 能被中断地获取锁
与synchronized不同，获取不到锁的线程，即处于等待获取同步状态时，如果当前线程被中断，会立即返回，并抛出InterruptedException(相对地，当一个线程获取不到锁而阻塞在synchronized时，对这个线程进行中断，这个线程的中断标志位会被修改，但线程会依旧阻塞在synchronized上)
3. 超时获取锁
在指定的截止时间之前获取锁，如果截止时间到了依旧无法获取锁，则返回

#### Lock接口的使用
```bash
Lock lock = new ReentrantLock();
lock.lock();
try{
  ...
}
finally{
  lock.unlock();
}
```

#### Lock接口的API
(1) lock
```bash
/* 
*  获取锁，调用该方法当前线程将会获取锁，当锁获得以后，从该方法返回
*  如果不能获取到锁，当前线程阻塞
*/
void lock();
```

(2) lockInterruptibly
```bash
/*
*  可中断地获取锁，和lock()方法不同的是，该方法会响应中断，
*  即在等待同步状态的过程中，可以中断当前线程。
*/
void lockInterruptibly() throws InterruptedException;
```

(3) tryLock
```bash
/*
*  尝试非阻塞的获取锁，调用该方法后立即返回，
*  如果能获取则返回true，否则返回false
*/
boolean tryLock();
```
对于tryLock的经典用法：
```bash
Lock lock = ...;
if (lock.tryLock()) {
  try {
    // manipulate protected state
  } finally {
    lock.unlock();
  }
} else {
  // perform alternative actions
}
```
这种用法可以保证只有获取到锁才会去释放锁，如果没有获取到锁是不会释放锁的。

(4) tryLock(long time, TimeUnit unit)
```bash
/*
*  超时地获取锁，当前线程在以下3种情况下会返回：
*  <1> 当前线程在超时时间内获取到锁
*  <2> 当前线程在超时时间内被中断
*  <3> 超时时间结束，返回false
*/
boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
```

(5) unlock
```bash
/*
*  释放锁
*/
void unlock();
```

(6) newCondition
```bash
/*
*  获取等待通知组件，该组件和当前的锁绑定，当前线程只有获得了锁，
*  才能调用组件的wait()方法，而调用后，当前线程将释放锁
*/
Condition newCondition();
```



