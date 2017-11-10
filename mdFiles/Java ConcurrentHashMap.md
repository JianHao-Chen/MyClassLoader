---
title: Java的ConcurrentHashMap
categories: Java笔记
---
由于在多线程环境下使用HashMap，会有很多问题。当然可以将HashMap加锁(如synchronized)。显然这是效率很低的。
那么ConcurrentHashMap通过什么方式提高效率呢？本文就是通过ConcurrentHashMap的源码分析其中的机制。

<!--more-->

---

#### <font color="red">存在问题1</font>

由于使用HashTable容器使用synchronized(我们用HashMap手动加锁也是一样)来保证线程安全，这就意味着对HashTable的读写都锁住了整个数组。
这里问题就出现在:
**获取的锁是锁住整个HashTable的！**

#### <font color="green">解决办法</font>

将ConcurrentHashMap分成若干个小部分，然后为每个小部分配上锁。也就是大家平时所说的 <font color="purple">**锁分段技术**</font>。
ConcurrentHashMap的结构如下：
<img src="http://img.blog.csdn.net/20171110192454125">


#### <font color="brown">具体实现</font>
ConcurrentHashMap保存着Segment数组：
```
final Segment<K,V>[] segments;
```
而这个Segment其实就是一个小的HashTable：
```
static final class Segment<K,V> extends ReentrantLock implements Serializable {
    transient volatile int count;
    transient int modCount;
    transient int threshold;
    transient volatile HashEntry<K,V>[] table;
    final float loadFactor;
}
```

ConcurrentHashMap相应的get、put方法如下：
```
public V get(Object key) {
    int hash = hash(key.hashCode());
    return segmentFor(hash).get(key, hash);
}


public V put(K key, V value) {
    if (value == null)
        throw new NullPointerException();
    int hash = hash(key.hashCode());
    return segmentFor(hash).put(key, hash, value, false);
}
```
我们看到都是先根据key计算出hash值,然后调用segmentFor方法：
```
final Segment<K,V> segmentFor(int hash) {
    return segments[(hash >>> segmentShift) & segmentMask];
}
```
这个方法就是通过将hash值向右无符号右移segmentShift位，然后和segmentMask进行与操作，从而确定segments数组中具体哪一个segment。

<font color="pink">**补充**</font>：

1. 当需要rehash的时候，也只在某个segment中，也就是不会影响其他的分段。
2. 当需要更新计数器时，不用锁定整个 ConcurrentHashMap。



#### <font color="red">存在问题2</font>
即使我们采取了分段，但是当多个线程访问到同一个segment的情况还是有可能发生的。
只要有一个线程需要写操作，那是必须独占这个segment的锁。这一点我们都理解。
但是如果多个读线程呢？在HashTable中，即使全是读线程，也要同步！这里就是效率低的原因之一。

#### <font color="green">解决办法</font>
在实际应用中，对ConcurrentHashMap的访问大部分是读操作，要是我去设计，可能是使用“读写锁”。
但是，这个还不够好，ConcurrentHashMap的做法是：<font color="tan">**降低读操作加锁的频率**</font>。


#### <font color="brown">具体实现</font>
先看看Segment读和写的代码：
```
V get(Object key, int hash) {
    if (count != 0) { // read-volatile
        HashEntry<K,V> e = getFirst(hash);
        while (e != null) {
            if (e.hash == hash && key.equals(e.key)) {
                V v = e.value;
                if (v != null)
                    return v;
                return readValueUnderLock(e); // recheck
            }
            e = e.next;
        }
    }
    return null;
}


V put(K key, int hash, V value, boolean onlyIfAbsent) {
    lock();
    try {
        int c = count;
        if (c++ > threshold) // ensure capacity
            rehash();
            HashEntry<K,V>[] tab = table;
            int index = hash & (tab.length - 1);
            HashEntry<K,V> first = tab[index];
            HashEntry<K,V> e = first;
            while (e != null && (e.hash != hash || !key.equals(e.key)))
                e = e.next;

            V oldValue;
            if (e != null) {
                oldValue = e.value;
                if (!onlyIfAbsent)
                    e.value = value;
            }
            else {
                oldValue = null;
                ++modCount;
                tab[index] = new HashEntry<K,V>(key, hash, first, value);
                count = c; // write-volatile
            }
            return oldValue;
        } finally {
            unlock();
        }
}
```

现在我们假设有一个读线程一个写线程并发访问同一个Segment中的HashEntry链表：

#### <font color="navy">情景一</font>：写线程只改变某一个HashEntry的值
其实，这种情况就是**不改变HashEntry链表的结构**。

从读线程的角度来看，当有另一个写线程操作同一个HashEntry时，有以下2种情况：

1. 写发生在读之后，这样读到的是正确的值。
2. 写发生在读之前，这样有可能读线程读不到更新后的值。这个属于并发中“可见性”的问题。

对此，ConcurrentHashMap是通过<font color="yellowgreen">volatile的工作机制</font>来处理这里的“可见性”问题。
看一下HashEntry的代码：
```
static final class HashEntry<K,V> {
    final K key;
    final int hash;
    volatile V value;
    final HashEntry<K,V> next;
}
```
这里将value值定义为`volatile`，Java 内存模型能够保证：<font color="scarlet">读线程读取的一定是更新后的值</font>！

(至于java内存模型如何保证，与volatile的工作机制不在本文的范围之内)。

这样，情景一就说完了，


#### <font color="navy">情景二</font>：写线程插入、删除链表中HashEntry
这种情况就是**改变HashEntry链表的结构**。

对此，ConcurrentHashMap的策略是<font color="yellowgreen">读线程遍历链表的过程中，写线程对链表的修改不影响读线程的继续遍历</font>。

上面代码有以下细节需要注意：

(1) get函数中，首先读取了count变量；而put函数在最后赋值了count变量。
而count变量是:
```
transient volatile int count;
```
所以，这里也是通过<font color="brown">Java 内存模型写线程对链表结构的改变，会被随后的读线程读到</font>。

(2) HashEntry中的next值定义为final
也就是HashEntry链表的中间和末尾都不允许增加、删除节点！
这一点能保证读线程继续遍历当前节点后面的所有节点是安全的，因为它们都不变！

到这里，你一定有疑问：<font color="red">不允许删除节点</font>？
写线程对链表增加节点，可以通过在链表头增加，来避免影响读线程的遍历。这个比较好理解。
但是，删除的时候，待删除的节点位于读线程当前访问到的节点之后呢？例如这种情况：
<img src="http://img.blog.csdn.net/20171110203545263">
假设读线程当前访问到B。而写线程需要删除C。怎么办呢？

Segment的remove方法如下：
```
V remove(Object key, int hash, Object value) {
    lock();
    try {
        int c = count - 1;
        HashEntry<K,V>[] tab = table;
        int index = hash & (tab.length - 1);
        HashEntry<K,V> first = tab[index];
        HashEntry<K,V> e = first;
        while (e != null && (e.hash != hash || !key.equals(e.key)))
            e = e.next;

        V oldValue = null;
        if (e != null) {
            V v = e.value;
            if (value == null || value.equals(v)) {
                oldValue = v;
                // All entries following removed node can stay
                // in list, but all preceding ones need to be
                // cloned.
                ++modCount;
                HashEntry<K,V> newFirst = e.next;
                for (HashEntry<K,V> p = first; p != e; p = p.next)
                    newFirst = new HashEntry<K,V>(p.key, p.hash,
                                                  newFirst, p.value);
                tab[index] = newFirst;
                count = c; // write-volatile
            }
        }
        return oldValue;
    } finally {
        unlock();
    }
}
```
策略就是:
节点C之前的节点都会复制一份,C之后的节点会继续保留着。
所以，删除后的链表结构为:
<img src="http://img.blog.csdn.net/20171110204010690">
<font color="red">注意：</font>由于每个节点都是从表头插入，所以新copy的节点的顺序是倒过来的。

<font color="pink">**补充**</font>：
在get函数中有调用readValueUnderLock函数的时候，就是当读到的HashEntry的value为null时。
什么时候会是null呢？

就是当<font color="red">**重排序发生**</font>的时候。注意在put函数里面，插入一个新HashEntry时：
```
// 插入
tab[index] = new HashEntry<K,V>(key, hash, first, value);

// HashEntry的构造函数
HashEntry(K key, int hash, HashEntry<K,V> next, V value) {
    this.key = key;
    this.hash = hash;
    this.next = next;
    this.value = value;
}
```
原来应该是先执行构造函数再赋值给tab[index]的。
但是因为重排序，使得先执行赋值`tab[index] = new HashEntry<K,V>(key, hash, first, value);`，再执行初始化中的`this.value = value;`。
那么，刚好在这2个步骤之间，读线程读取了这个HashEntry的value值。那就会得到null了。至于HashEntry的构造函数中其他3个赋值，由于它们都是`final`的，所以Java内存模型会保证这3个赋值在构造函数返回前就执行完！





