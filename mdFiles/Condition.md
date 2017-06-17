---
title: Condition接口
categories: Java笔记
---

任意一个Java对象，都拥有一组监视器方法（定义在java.lang.Object上），主要包括wait()、wait(long timeout)、notify()以及notifyAll()方法，这些方法与synchronized同步关键字配合，可以实现等待/通知模式。Condition接口也提供了类似Object的监视器方法，与Lock配合可以实现等待/通知模式，但是这两者在使用方式以及功能特性上还是有差别的。

<!--more-->

---

## Condition接口介绍


#### Object的监视器方法与Condition接口的对比

1. 等待队列
Object的monitor方法只有一个等待队列，而Condition可以有多个

2. 在等待状态中屏蔽中断
如果想要令当前线程释放锁进入等待状态，在等待状态中不相应中断。
这个功能在Object的monitor方法是不支持的！而使用Condition可以支持。

3. 带超时的等待状态
如果想要令当前线程释放锁进入等待状态，在将来的某个时候结束等待。
这个功能在Object的monitor方法是不支持的！而使用Condition可以支持。


#### Condition的使用
Condition定义了等待/通知两种类型的方法，当前线程调用这些方法时，需要提前获取到Condition对象关联的锁。Condition对象是由Lock对象（调用Lock对象的newCondition()方法）创建出来的，换句话说，Condition是依赖Lock对象的。

下面是Condition使用的例子：
```bash
Lock lock = new ReentrantLock();
Condition condition = lock.newCondition();

public void conditionWait() throws InterruptedException {
    lock.lock();
    try {
      condition.await();
    } finally {
        lock.unlock();
      }
} 
  
public void conditionSignal() throws InterruptedException {
    lock.lock();
    try {
      condition.signal();
    } finally {
        lock.unlock();
    }
}
```
如上面代码所示，一般都会将Condition对象作为成员变量。当调用await()方法后，当前线程会释放锁并在此等待，而其他线程调用Condition对象的signal()方法，通知当前线程后，当前线程才从await()方法返回，并且在返回前已经获取了锁。


#### Condition定义的（部分）方法

1. void await() throws InterruptedException
当前线程进入等待状态直到被通知(signal)或中断。

2. void awaitUninterruptibly()‘
当前线程进入等待状态直到被通知，这个方法对中断不敏感。

3. long awaitNanos(long nanosTimeout)
当前线程进入等待状态直到被通知、中断或超时

4. boolean awaitUntil(Date deadline)
当前线程进入等待状态直到被通知、中断或直到某个时间。如果没有到底指定的时间就被通知，返回 true。
否则表示到底指定时间，返回 false。

5. void signal()
唤醒一个等待在 Condition 上的线程，该线程从等待方法返回前，必须获得与 Condition 相关联的锁。

6. void signalAll()
唤醒所有等待在 Condition 上的线程，能够从等待方法返回的线程，必须获得与 Condition 相关联的锁。



## Condition接口的实现

我们分析的是 AQS 的内部类 ConditionObject，
每个Condition对象都包含着一个队列（以下称为等待队列），该队列是Condition对象实现等待/通知功能的关键。

下面将分析Condition的实现，主要包括：等待队列、等待和通知，下面提到的Condition如果不加说明均指的是ConditionObject。

#### 等待队列
等待队列是一个FIFO的队列，在队列中的每个节点都包含了一个线程引用，该线程就是在Condition对象上等待的线程，如果一个线程调用了Condition.await()方法，那么该线程将会释放锁、构造成节点加入等待队列并进入等待状态。事实上，节点的定义复用了同步器中节点的定义，也就是说，同步队列和等待队列中节点类型都是同步器的静态内部类AbstractQueuedSynchronizer.Node。

一个Condition包含一个等待队列，Condition拥有首节点（firstWaiter）和尾节点（lastWaiter）。当前线程调用Condition.await()方法，将会以当前线程构造节点，并将节点从尾部加入等待队列，等待队列的基本结构如下图所示：
<img src="http://img.blog.csdn.net/20170613174405443">

如图所示，Condition拥有首尾节点的引用，而新增节点只需要将原有的尾节点nextWaiter指向它，并且更新尾节点即可。上述节点引用更新的过程并没有使用CAS保证，原因在于调用await()方法的线程必定是获取了锁的线程，也就是说该过程是由锁来保证线程安全的。

在Object的监视器模型上，一个对象拥有一个同步队列和等待队列，而并发包中的Lock（更确切地说是同步器）拥有一个同步队列和多个等待队列，其对应关系如下图所示：
<img src="http://img.blog.csdn.net/20170613174419769">

#### 进入等待队列(等待)
调用Condition的await()方法（或者以await开头的方法），会使当前线程进入等待队列并释放锁，同时线程状态变为等待状态。当从await()方法返回时，当前线程一定获取了Condition相关联的锁。await()方法的代码：
```bash
public final void await() throws InterruptedException {
  if (Thread.interrupted())
    throw new InterruptedException();
  Node node = addConditionWaiter();
  int savedState = fullyRelease(node);
  int interruptMode = 0;
  while (!isOnSyncQueue(node)) {
    LockSupport.park(this);
    if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
      break;
  }
  if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
    interruptMode = REINTERRUPT;
  if (node.nextWaiter != null)
    unlinkCancelledWaiters();
  if (interruptMode != 0)
    reportInterruptAfterWait(interruptMode);
}
```
await()方法做了以下几件事：
1. addConditionWaiter()方法就是在等待队列的尾部插入一个新建的Node节点，状态为 Node.CONDITION。
2. fullyRelease(Node node)方法释放同步状态并且唤醒后继节点的线程。
3. 通过isOnSyncQueue(Node node)方法判断如果当前节点不在同步队列里面，就进入while循环中。在循环中，会通过LockSupport的park()方法将当前节点的线程挂起。如果这个线程(在一段时间后)被唤醒，首先检查是否被中断，如果是就跳出while循环。
4. 后面就是获取同步状态(如果节点在同步队列)、清除在等待队列中被取消的节点(当前节点也是在等待队列中，被取消的)、处理中断。

我们看一下isOnSyncQueue(Node node)方法：
```bash
final boolean isOnSyncQueue(Node node) {
  /*
  * 情况一：
  * 如果节点的状态是 CONDITION，或者节点的前驱节点为null,
  * 表示该节点处于等待队列
  */
  if (node.waitStatus == Node.CONDITION || node.prev == null)
    return false;
  /*
  * 情况二：
  * 如果有后继节点，那么这个节点在同步队列里面
  */
  if (node.next != null) 
    return true;
  /*
  * 情况三：
  * node.prev != null，但该节点还没有处于同步队列里面。
  */
  return findNodeFromTail(node);    
}
```
情况三产生原因：
在transferForSignal(Node node)方法里面，会调用enq(final Node node)方法，这个方法在死循环中做了以下2件事：
1. 设置node.prev = tail
2. 通过CAS设置这个node为tail。

问题就是CAS操作可能会失败(不过由于这2个操作是在死循环中，最终还是会成功的)。
所以对于情况3，使用findNodeFromTail(Node node)方法，遍历同步队列查找这个node。

  进入等待队列的情形如下图所示：
<img src="http://img.blog.csdn.net/20170613180400249">


#### 移出等待队列(唤醒)
调用Condition的signal()方法，将会唤醒在等待队列中等待时间最长的节点（首节点），在唤醒节点之前，会将节点移到同步队列中。

移出等待队列的情形如下图所示：
<img src="http://img.blog.csdn.net/20170613180423968">

ConditionObject的signal方法：
```bash
public final void signal() {
  if (!isHeldExclusively())
    throw new IllegalMonitorStateException();
  Node first = firstWaiter;
  if (first != null)
    doSignal(first);
}
```
调用该方法的前置条件是当前线程必须获取了锁，可以看到signal()方法进行了isHeldExclusively()检查，也就是当前线程必须是获取了锁的线程。接着获取等待队列的首节点，然后运行doSignal(Node first)方法：
```bash
private void doSignal(Node first) {
  do {
    if ( (firstWaiter = first.nextWaiter) == null)
      lastWaiter = null;
      first.nextWaiter = null;
  } while (!transferForSignal(first) && (first = firstWaiter) != null);
}
```
doSignal()方法在while循环中调用transferForSignal(Node node)方法，可以看到每次都是取出等待队列的头节点，将它作为参数传入transferForSignal方法。
transferForSignal(Node node)方法用于将node节点转移到同步队列，看看transferForSignal(Node node)方法的实现：
```bash
final boolean transferForSignal(Node node) {
  // 如果不能成功设置这个节点的状态为0(初始状态)，
  // 那么说明这个节点已经被取消了，目前状态为 CANCELLED
  if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
    return false;
   
  // 下面是将节点加入到同步队列，并且把这个节点的前驱节点的状态
  // 设置为 Node.SIGNAL，令前驱节点释放同步状态时唤醒该节点。
  // 如果前驱节点已经被取消了或设置它的状态失败，我们直接唤醒这个
  // 节点的线程，(它会在之前的await()方法中的LockSupport.park(this)语句中醒来
  // 并且会重新获取同步状态)。
  Node p = enq(node);
  int c = p.waitStatus;
  if (c > 0 || !compareAndSetWaitStatus(p, c, Node.SIGNAL))
    LockSupport.unpark(node.thread);
  return true;
}
```