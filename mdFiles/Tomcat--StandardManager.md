---
title: Tomcat是如何对Session管理的
categories: Tomcat源码
---

本文将介绍Tomcat是如何对Session管理的。

<!--more-->

---


## Manager
对Session的管理,Tomcat是通过 Manager 接口及其实现类来完成的。
StandardManager是 Tomcat默认的Session管理类,它继承于ManagerBase。


在ManagerBase中，有一个ConcurrentHashMap用于保存当前active的session。
```bash
protected Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();
```
ManagerBase 里面实现的关于session 的增删查改方法就省略了。


#### 对session是否有效的检查
1. Tomcat中有一个BackgroundProcessor线程，会定时自顶向下地执行容器的backgroundProcess()方法(包括ManagerBase的backgroundProcess()方法)。
2. 在 ManagerBase的backgroundProcess()方法,它调用了processExpires()方法，processExpires()将保存在当前manager的当前active的session遍历一遍，对每一个session都调用isValid()方法。
3. isValid()方法通过计算当前时间和这个session的上一次访问时间的时间差(未被访问的时间)，与 maxInactiveInterval比较,如果超过maxInactiveInterval,就调用session的expire()方法将这个session置为失效。

(1) 获取当前active的session，是通过ManagerBase的findSessions()方法：
```bash
/**
 * Return the set of active Sessions associated with this Manager.
*/
public Session[] findSessions() {
  return sessions.values().toArray(new Session[0]);
}
```

(2) Session的expire()方法：
```bash
public void expire(boolean notify) {
  // Check to see if expire is in progress or has previously been called
  if (expiring || !isValid)
    return;
     
  synchronized (this) {
    if (expiring || !isValid)
      return;
      
    if (manager == null)
      return; 
      
    // 先把 expiring 置为 true!
    expiring = true;
    
    ...(省略部分与Session Listener相关的代码，用于“报告”这个session将要被销毁)
    
    setValid(false);
    
    ...(省略这部分代码，用于统计session的相关数据，如session的平均存活时间)
    
    // 调用ConcurrentHashMap的remove方法
    manager.remove(this);
    
    expiring = false;
    
    ...(省略这部分代码，用于解除与这个session关联的attribute)
    
   }
 }
```
