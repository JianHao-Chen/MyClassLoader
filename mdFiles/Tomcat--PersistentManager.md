---
title: Tomcat的Session持久化
categories: Tomcat源码
---

本文将分析Tomcat是如何对Session持久化的。

<!--more-->

---

## 背景
Session的主要数据被存储在服务器内存中，而服务器会为每个在线用户创建一个Session对象，当在线用户很多时，例如同时有几万或是几十万在线的情况下，Session内存的开销将会十分巨大，会影响Web服务器性能。而Session的钝化机制刚好可解决此问题。Session钝化机制的本质就在于把服务器中不经常使用的Session对象暂时序列化到系统文件系统或是数据库系统中，当被使用时反序列化到内存中，整个过程由服务器自动完成。 


## 配置

在 `conf\context.xml` 中使用如下标签：
```bash
<Manager className="org.apache.catalina.session.PersistentManager">
  <Store className="org.apache.catalina.session.FileStore"/>
</Manager>
```
然后Tomcat启动时,会创建此PersistentManager并把它与StandardContext关联起来。


## 实现

先看PersistentManager的processExpires()方法，它的processExpires()方法与默认的StandardManager的很相似，只是多出了 `processPersistenceChecks`方法：
```bash
public void processPersistenceChecks() {
  processMaxIdleSwaps();
  processMaxActiveSwaps();
  processMaxIdleBackups();
}
```
这个方法所调用的3个方法分别做的是：
1. 空闲时间太长的session被换出
2. active的session数目太多被换出

