---
title: Tomcat的热部署
categories: Tomcat源码
---

Tomcat的热部署就是对JSP或Java类进行了修改在不重启Tomcat的前提下能让修改生效。
听起来很高大上，其实并不神秘。

<!--more-->

---

## 配置
在 server.xml 里面, Context标签 中的 “reloadable”变量设置为 true,那么这个classloader的仓库里面的资源发生改变, 资源会被reload。
这里“仓库”指的是“/WEB-INF/lib/”下的jar和“/WEB-INF/classes/”下的class文件。

## 检查资源是否改变
Tomcat中有一个backgroundProcessor线程，用于从StandardEngine开始，自顶向下执行每个容器的backgroundProcess()方法。

当WebappLoader的backgroundProcess()方法被调用时，将会调用WebappClassLoader的modified()方法,检查每一个resources的lastModifiedDate。
```bash
public boolean modified() {

  // Checking for modified loaded resources
  // paths是string[]，用于保存需要检查更新状态的资源的名单
  int length = paths.length;
  
  for (int i = 0; i < length; i++) {
    long lastModified = ((ResourceAttributes) resources.getAttributes(paths[i])).
                         getLastModified();
    if (lastModified != lastModifiedDates[i]) {
      return (true);
    }
  }
  
  // Check if JARs have been added or removed
  ....(省略)
}
```

如果仓库中有任何一个文件被改动了,就调用StandardContext的reload()方法。

## reload()方法
StandardContext的reload()方法逻辑还是很清晰的(省略了log，try catch等部分)：
```bash
public synchronized void reload() {

  // Stop accepting requests temporarily
  setPaused(true);
  
  stop();
  
  start();
  
  setPaused(false);
}
```
可以将reload()方法分3部分分析。

#### 暂时停止接受请求 (setPaused()方法)
这一步通过2方面的“合作”来完成：
1. setPaused()方法会把将StandardContext的 paused字段置为true。
2. 在StandardContextValve的invoke()方法里面，会对当前的Context是否暂停进行判断,如果是(这个处理请求的线程)就进入睡眠。
```bash
boolean reloaded = false;
while (context.getPaused()) {
  reloaded = true;
  try {
    Thread.sleep(1000);
  }
  catch (InterruptedException e) {;}
}
```
可以看到，在reload()方法的最后，才把paused字段置为false，这样这个处理请求的线程才会继续向下执行。

#### 停止这个StandardContext (stop()方法)
这里有以下几个步骤：
1. 设置这个Context的available标志为 false
2. 调用这个Context下的所有子容器(StandardWrapper)的stop()方法
3. 执行这个Context所关联的manager的stop()方法,包括expire所有sessions,并且把有效的session持久化。
4. 执行这个Context所关联的pipeline的stop(),解除pipeline上所有的Valve。(Context的start()方法会导致相关配置文件的重新读取解析,Valve的配置可能会变动)。
5. 执行resources的stop()方法，并将resources引用置null,令相关对象被回收。
6. 执行WebappLoader的stop()方法。

其中第6步分为2部分：
1. 调用WebappClassLoader的stop()方法。
2. 将WebappClassLoader的引用classLoader置为null，令WebappClassLoader可以被回收。

WebappClassLoader的stop()方法做了以下2件事：
1. 通过clearReferences()清除引用，包括：
 * Jdbc驱动的引用
 * 线程的引用
 * ThreadLocal的引用
 * .....
2. 将File[]数组 files、JarFile[]数组 jarFiles、缓存resourceEntries(HashMap)、资源DirContext resources等数据都清除。
 

#### 启动这个StandardContext (start()方法)
启动StandardContext下的各个组件。
特别要注意的是，WebappLoader会新建一个WebappClassLoader，并且需要重新设置Repositories。
