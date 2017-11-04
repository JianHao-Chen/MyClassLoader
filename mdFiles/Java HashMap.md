---
title: Java的HashMap
categories: Java笔记
---

之前就看过JDK 1.7的HashMap的代码，后来听说JDK 1.8对HashMap做了优化，特此看一下，顺便做点笔记。
<!--more-->

---

## HashMap数据结构

JDK7中HashMap采用的是位桶+链表的方式，即我们常说的散列链表的方式，而JDK8中采用的是位桶+链表/红黑树的方式。当某个位桶的链表的长度达到某个阀值的时候，这个链表就将转换成红黑树。
<img src="http://img.blog.csdn.net/20171104110925318">

## 内部实现
在分析HashMap的内部实现时，会尽可能的同时拿JDK 1.7和1.8的比较。

#### 实现存储

###### JDK 1.7

使用的是Entry数组

```
transient Entry<K,V>[] table;
```
每一个Entry保存一个键值对：
```
static class Entry<K,V> implements Map.Entry<K,V> {
    final K key;
    V value;
    Entry<K,V> next;
    int hash;
}
```

###### JDK 1.8

使用的是Node数组

```
transient Node<K,V>[] table;

// 当桶(bucket)上的结点数大于这个值时会转成红黑树
static final int TREEIFY_THRESHOLD = 8; 

// 当桶(bucket)上的结点数小于这个值时树转链表
static final int UNTREEIFY_THRESHOLD = 6;
```
Node也是保存键值对的：
```
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;
}
```
看起来，这个Node与数据结构“树”没有关系啊。
别急，继续看。在HashMap里面还有一个内部类：
```
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
    TreeNode<K,V> parent;  // red-black tree links
    TreeNode<K,V> left;
    TreeNode<K,V> right;
    TreeNode<K,V> prev;    // needed to unlink next upon deletion
    boolean red;
}
```
而这个 `LinkedHashMap.Entry<K,V>`是继承了`HashMap.Node<K,V>`的。
所以我们可以先“推断”：Node<K,V>[] table里面的元素，可以是普通的(即链表结构)Node，也可能是实现红黑树的TreeNode结构。


#### Hash算法
Hash算法是HashMap的精髓，应该好好看一下。

###### JDK 1.7
有2个函数
```
final int hash(Object k) {
    int h = 0;
    h ^= k.hashCode();

    h ^= (h >>> 20) ^ (h >>> 12);
    return h ^ (h >>> 7) ^ (h >>> 4);
}

static int indexFor(int h, int length) {
    return h & (length-1);
}
```

###### JDK 1.8
只有1个函数：
```
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```


(1) `indexFor()`函数
看起来只是一个散列值和数组长度做一个"与"操作。其实，里面也是有玄机的。

* HashMap的数组长度要取2的整次幂，所以(数组长度-1)的值为 二进制的 "11111..."
* 假设(数组长度-1)的值为 二进制的"11111...0"，那么index = h & (length-1)的结果的最后一位一定只能为0，也就是index的值永远不会等于不能被2整除的数，即这个数组的可用空间就只有一半。
* 可用空间减半直接导致冲突发生的概率大增。

(2) `hash()`函数
JDK 1.8的hah()函数主要是hashCode()的高16位异或低16位，这样是为了高低位都参与到Hash的计算中。JDK 1.7的其实原理差不多。
找了一张网上流传的图片：
<img src="http://img.blog.csdn.net/20171104135817483">


#### put方法的实现

###### JDK 1.7

```
public V put(K key, V value) {
    if (table == EMPTY_TABLE) {
        inflateTable(threshold);
    }
    if (key == null)
        return putForNullKey(value);
    int hash = hash(key);
    int i = indexFor(hash, table.length);
    for (Entry<K,V> e = table[i]; e != null; e = e.next) {
        Object k;
        if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
            V oldValue = e.value;
            e.value = value;
            e.recordAccess(this);
            return oldValue;
        }
    }

    modCount++;
    addEntry(hash, key, value, i);
    return null;
}
```
这里我们可以看到，通过计算得到key对应table的哪一个index(也就是找到对应的桶)。
如果找到相同key(即hash值相等，并且equal()方法的结果也相等)，那么就替换它。否则把这个键值对插入到这个链表。
其中，插入的算法在addEntry()里面：
```
void addEntry(int hash, K key, V value, int bucketIndex) {
    if ((size >= threshold) && (null != table[bucketIndex])) {
        resize(2 * table.length);
        hash = (null != key) ? hash(key) : 0;
        bucketIndex = indexFor(hash, table.length);
    }

    createEntry(hash, key, value, bucketIndex);
}
    
void createEntry(int hash, K key, V value, int bucketIndex) {
    Entry<K,V> e = table[bucketIndex];
    table[bucketIndex] = new Entry<>(hash, key, value, e);
    size++;
}    
```
如果size大于threshold时，会发生扩容。threshold等于capacity*load factor


###### JDK 1.8
```
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}

final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                boolean evict) {
                
    Node<K,V>[] tab; 
    Node<K,V> p; 
    int n, i;
    
    //如果当前map中无数据，执行resize方法。并且返回n
    if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
    //如果要插入的键值对要存放的这个位置刚好没有元素，那么把他封装成Node对象，放在这个位置上就完事了        
    if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
    //否则的话，说明这上面有元素
    else {
        Node<K,V> e; K k;
        //如果这个元素的key与要插入的一样，那么就替换一下，也完事。
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        //如果当前节点是TreeNode类型的数据，执行putTreeVal方法
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        //还是遍历这条链子上的数据，跟jdk7没什么区别
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    //判断链表长度是否大于TREEIFY_THRESHOLD(默认值为8)，大于的话把链表转换为红黑树，在红黑树中执行插入操作，否则进行链表的插入操作；
                    if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                        treeifyBin(tab, hash);
                        break;
                }
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        if (e != null) { // existing mapping for key
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }
    ++modCount;
    // 插入成功后，判断实际存在的键值对数量size是否超多了最大容量threshold，如果超过，进行扩容
    if (++size > threshold)
        resize();
    afterNodeInsertion(evict);
    return null;
}
```


#### resize 的实现

###### JDK 1.7
```
void resize(int newCapacity) {
    Entry[] oldTable = table;
    int oldCapacity = oldTable.length;
    if (oldCapacity == MAXIMUM_CAPACITY) {  //扩容前的数组大小如果已经达到最大(2^30)了
        threshold = Integer.MAX_VALUE;      //修改阈值为int的最大值(2^31-1)，这样以后就不会扩容了
        return;
    }

    Entry[] newTable = new Entry[newCapacity];  //初始化一个新的Entry数组
    transfer(newTable, initHashSeedAsNeeded(newCapacity));   //！！将数据转移到新的Entry数组里
    table = newTable;
    threshold = (int)Math.min(newCapacity * loadFactor, MAXIMUM_CAPACITY + 1);
}

void transfer(Entry[] newTable, boolean rehash) {
    int newCapacity = newTable.length;
    for (Entry<K,V> e : table) {
        while(null != e) {
            Entry<K,V> next = e.next;
            if (rehash) {
                e.hash = null == e.key ? 0 : hash(e.key);
            }
            int i = indexFor(e.hash, newCapacity);
            e.next = newTable[i];
            newTable[i] = e;
            e = next;
        }
    }
}
```
<font color=red>**注意：**</font>这里使用了单链表的头插入方式，旧链表迁移新链表的时候，如果在新表的数组索引位置相同，则链表元素会倒置。
而JDK1.8的resize()不会倒置

###### JDK 1.8

我们使用的是2次幂的扩展(指长度扩为原来2倍)，所以，元素的位置要么是在原位置，要么是在原位置再移动2次幂的位置。看下图可以明白这句话的意思，n为table的长度，图（a）表示扩容前的key1和key2两种key确定索引位置的示例，图（b）表示扩容后key1和key2两种key确定索引位置的示例，其中hash1是key1对应的哈希与高位运算结果。
<img src="http://img.blog.csdn.net/20171104171353997">

元素在重新计算hash之后，因为n变为2倍，那么n-1的mask范围在高位多1bit(红色)，因此新的index就会发生这样的变化：
<img src="http://img.blog.csdn.net/20171104171557558">

因此，我们在扩充HashMap的时候，不需要像JDK1.7的实现那样重新计算hash，只需要看看原来的hash值新增的那个bit是1还是0就好了，是0的话索引没变，是1的话索引变成“原索引+oldCap”，可以看看下图为16扩充为32的resize示意图：
<img src="http://img.blog.csdn.net/20171104171720669">
这个设计确实非常的巧妙，既省去了重新计算hash值的时间，而且同时，由于新增的1bit是0还是1可以认为是随机的，因此resize的过程，均匀的把之前的冲突的节点分散到新的bucket了。这一块就是JDK1.8新增的优化点。

```
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    if (oldCap > 0) {
        // 超过最大值就不再扩充了
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        // 没超过最大值，就扩充为原来的2倍
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                oldCap >= DEFAULT_INITIAL_CAPACITY)
            newThr = oldThr << 1; // double threshold
    }
    else if (oldThr > 0) // initial capacity was placed in threshold
        newCap = oldThr;
    else {               // zero initial threshold signifies using defaults
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }
    
    // 计算新的resize上限
    if (newThr == 0) {
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
            (int)ft : Integer.MAX_VALUE);
    }
    threshold = newThr;
    @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        
    table = newTab;
    if (oldTab != null) {
        // 把每个bucket都移动到新的buckets中
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;
                            // 原索引
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            // 原索引+oldCap
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
            }
        }
    }
    return newTab;
}
```

