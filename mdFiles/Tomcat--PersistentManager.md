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

当然，“FileStore”是Tomcat提供的，负责把session保存到文件系统。我们也可以使用Tomcat的JDBCStore，它可以把session保存到数据库。举一反三，我们还可以自行写一个“NetWorkStore”，把session通过网络保存到其他计算机中。


## 实现


#### 持久化的时机
先看PersistentManager的processExpires()方法，它的processExpires()方法与默认的StandardManager的很相似，只是多出了 `processPersistenceChecks`方法：
```bash
public void processPersistenceChecks() {
  processMaxIdleSwaps();
  processMaxActiveSwaps();
  processMaxIdleBackups();
}
```
先看processMaxIdleSwaps方法(省略部分代码)：
```bash
protected void processMaxIdleSwaps() {

  if (!isStarted() || maxIdleSwap < 0)
    return;
    
  Session sessions[] = findSessions();
  long timeNow = System.currentTimeMillis();
  
  // Swap out all sessions idle longer than maxIdleSwap
  if (maxIdleSwap >= 0) {
    for (int i = 0; i < sessions.length; i++) {
       StandardSession session = (StandardSession) sessions[i];
       synchronized (session) {
         if (!session.isValid())
           continue;
           
         int timeIdle = //计算空闲时间
           (int) ((timeNow - session.getLastAccessedTime()) / 1000L);
         if (timeIdle > maxIdleSwap && timeIdle > minIdleSwap) {
           ...
           swapOut(session);
         }
       }
    }
  }
    
}
```
processMaxIdleSwaps()方法做的就是遍历当前所有的session，如果session是有效的，并且空闲时间超过了指定值maxIdleSwap，就调用swapOut()方法把它“置换”出去。

processMaxActiveSwaps()方法的代码就不展示了，它做的是当active的session数目太多时(超过minIdleSwap)，换出若干个session直到满足条件。

processMaxIdleBackups()方法做的是遍历当前所有的session，如果session是有效的，并且空闲时间超过了指定值maxIdleBackup，就调用writeSession()方法将session“备份”。这里只调用writeSession()方法把session写到磁盘，并没有调用相关的remove()方法，session还存在于内存。


#### 持久化的操作

###### swapOut()方法
```bash
protected void swapOut(Session session) throws IOException {
  if (store == null || !session.isValid())
    return;
    
  ((StandardSession)session).passivate(); 
  writeSession(session);
  super.remove(session);
  session.recycle();
}
```
1. passivate()方法主要做的是处理这个session的attributes，其中每个attribute如果是HttpSessionActivationListener，那么调用它的sessionWillPassivate()方法。
2. writeSession()做的是调用FileStore的save()方法，把session保存在文件系统。
3. remove()方法把这个session从当前Manager的ConcurrentHashMap中删除。


###### FileStore的save()方法
典型的ObjectOutputStream的用法。
```bash
public void save(Session session) throws IOException {

  // 根据sessionID创建文件，默认是“xxid.session”
  File file = file(session.getIdInternal());
  if (file == null)
     return;
     
  FileOutputStream fos = null;
  ObjectOutputStream oos = null;
  try {
    fos = new FileOutputStream(file.getAbsolutePath());
    oos = new ObjectOutputStream(new BufferedOutputStream(fos));
  }
  catch (IOException e) {
    if (oos != null) {
      try {
        oos.close();
      }catch (IOException f){;}
    }
    throw e;
  }

  try {
    ((StandardSession)session).writeObjectData(oos);
  }
  finally {
    oos.close();
  }
}
```

writeObjectData()调用了writeObject()方法

###### StandardSession的writeObject()方法
```bash
protected void writeObject(ObjectOutputStream stream) throws IOException {

  stream.writeObject(new Long(creationTime));
  stream.writeObject(new Long(lastAccessedTime));
  stream.writeObject(new Integer(maxInactiveInterval));
  stream.writeObject(new Boolean(isNew));
  stream.writeObject(new Boolean(isValid));
  stream.writeObject(new Long(thisAccessedTime));
  stream.writeObject(id);
  
  ...
  
  // Serialize the attribute count and the Serializable attributes
  int n = saveNames.size();
  stream.writeObject(new Integer(n));
  for (int i = 0; i < n; i++) {
    stream.writeObject((String) saveNames.get(i));
    try {
      stream.writeObject(saveValues.get(i));
    }
    catch (NotSerializableException e) {;}
  }
}
```

#### session的读取
我们把session换出去了，就要把它换回来啊。

Tomcat通过FileStore的load()方法读取被持久化的session，关键代码是：
```bash
public Session load(String id) throws ClassNotFoundException, IOException {
  
  // Open an input stream to the specified pathname, if any
  File file = file(id);
  ...
  FileInputStream fis = null;
  ObjectInputStream ois = null;
  
  fis = new FileInputStream(file.getAbsolutePath());
  BufferedInputStream bis = new BufferedInputStream(fis);
  ois = new ObjectInputStream(bis);
  ...
  try {
    StandardSession session =
      (StandardSession) manager.createEmptySession();
    session.readObjectData(ois);
    session.setManager(manager);
    return (session);
  }
  finally {
    // Close the input stream
    ...
  }
```

还有就是Tomcat什么时候读取session？
(1) PersistentManager在processExpires()的时候，会调用FileStore的processExpires()方法，这个方法会读取每一个已被持久化的session到内存，判断这个session是否expired，如果是，调用session的expire()方法，并删除保存在磁盘的session文件。
(2) PersistentManager在查找session的时候，如果在当前active的session中找不到，就会尝试调用swapIn(id)方法(它会调用FileStore的load())。