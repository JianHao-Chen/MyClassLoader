---
title: Tomcat的WebappClassLoader
categories: Tomcat源码
---

Tomcat的WebappClassLoader用于为每个与之关联StandardContext加载资源。

<!--more-->

---

## WebappClassLoader的加载流程
1. 到缓存中获取，如果缓存中有直接返回
2. 在本地仓库加载,包括：
 * WEB-INF/classes里的类
 * WEB-INF/lib里的jar
3. 委托父装载器或者系统类装载器装载

## 使用WebappClassLoader的原因
1. 实现不同web app 的类隔离。
2. 通过自定义的缓存类提高速度。(每个由webappclassloader装载的类被视为资源，用ResourceEntry表示。)

## WebappClassLoader的实现

#### 结构
StandardContext中的 “loader” 为 WebappLoader。
WebappLoader中的 “classLoader” 为 WebappClassLoader

#### WebappClassLoader的start()方法

1. StandardContext的start() 调用 WebappLoader的start()
2. WebappLoader的start()方法做了以下几件事：
 * 创建 WebappClassLoader
 * 为WebappClassLoader设置 resources。这里resources就是WebappClassLoader的资源类
 * 为WebappClassLoader设置 repositories，主要是将folder(/WEB-INF/classes)和folder(/WEB-INF/lib)读入WebappClassLoader的资源类


#### WebappClassLoader的loadClass()方法

假设我们在某个WebApp的WEB-INF/web.xml里面，添加了HelloWorldExample的servlet标签，那么在对应的StandardContext的start()方法中，会读取并解析这个xml，其中StandardContext就会调用它所关联的WebappClassLoader的loadClass()方法加载名为“HelloWorldExample”的类。

这个方法的代码(加上自己的注释，省略log、try catch等部分代码)：
```bash
public synchronized Class loadClass(String name, boolean resolve)
  throws ClassNotFoundException {

  Class clazz = null;
  
  // 在自己的缓存中去找
  // findLoadedClass0()方法其实就在WebappClassLoader所维护的一个叫resourceEntries的HashMap中找
  // key为name的object，这个object的类型是ResourceEntry。ResourceEntry中有loadedClass引用，表示
  // 这个类的Class字节数组。
  clazz = findLoadedClass0(name);
  if (clazz != null){
    if (resolve) resolveClass(clazz);
    return (clazz);
  }
    
  // 检查JVM中的缓存
  clazz = findLoadedClass(name);
  if (clazz != null){
    if (resolve) resolveClass(clazz);
    return (clazz);
  }
  
 /*
  * Servlet规范指出，容器用于加载Web应用内Servlet的class loader,允许加载位于Web应用内的资源。
  * 但不允许重写java.*, javax.*以及容器实现的类。
  * 例如对于我们应用内提供的Servlet-api，应用服务器是不会加载的，因为容器已经自已加载过了。
  */
  // 尝试用系统的类装载器进行装载,防止Web应用程序中的类覆盖J2EE的类
  // 在我的机器上，这个system默认是sun.misc.Launcher$AppClassLoader
  clazz = system.loadClass(name);
  
  
  // 查找自己的 repositories
  // findClass()方法会调用findClassInternal()方法，等一下再介绍它的代码。
  clazz = findClass(name);
  if (clazz != null){
    if (resolve) resolveClass(clazz);
    return (clazz);
  }
  
  // 委托给父类加载器
  if (!delegateLoad) {    // delegateLoad默认是false
    ClassLoader loader = parent;
    clazz = loader.loadClass(name);
    ...
  }
  
  // 最后都没有找到的话
  throw new ClassNotFoundException(name);
}
```
对于WebappClassLoader的加载策略，现在是基本清楚了。继续看它查找自己repositories的代码(同样加上自己的注释，省略log、try catch等部分代码)：
```bash
protected Class findClassInternal(String name) throws ClassNotFoundException{
  // 对name进行检查，不允许以"java.*"和"javax.servlet.*"开头
  if (!validate(name))
    throw new ClassNotFoundException(name);

  String tempPath = name.replace('.', '/');
  String classPath = tempPath + ".class";      //此时得到classPath = HelloWorldExample.class
  
  // 找出代表HelloWorldExample.class文件的那个ResourceEntry
  ResourceEntry entry = findResourceInternal(classPath);
  if (entry == null)
    throw new ClassNotFoundException(name);
    
  // 如果这个entry是第一次得到的，它的loadedClass为空，需要调用defineClass()函数得到相应的Class对象。
  // 如果这个entry不是第一次得到的，那么它的loadedClass已经保存着相应的Class对象。
  // 返回HelloWorldExample.class的字节数组
  Class clazz = entry.loadedClass;
  if (clazz != null)
    return clazz;
  ...
  
  // 将Class的字节数组转换为 Class对象，并保存在entry的loadedClass中
  clazz = defineClass(name, entry.binaryContent, 0,entry.binaryContent.length,
                      new CodeSource(entry.codeBase, entry.certificates));
  entry.loadedClass = clazz;
  entry.binaryContent = null;
  ...
}
```

再看findResourceInternal()方法：
```bash
// resourceEntries是 HashMap,作为缓存
ResourceEntry entry = (ResourceEntry) resourceEntries.get(name);
if (entry != null)
  return entry;
  
// 缓存没有,就在自己的repositories里面找
for (i = 0; (entry == null) && (i < repositoriesLength); i++) {
  // 得到fullPath为 : " /WEB-INF/classes/HelloWorldExample.class "
  String fullPath = repositories[i] + path;
  
  // resources是DirContext对象(directory context的意思,目录上下文),代表这个Webapp的resources
  // 得到lookupResult为 FileResource 对象
  Object lookupResult = resources.lookup(fullPath);

  // 创建entry对象,并把资源的绝对路径保存在entry里面
  entry = findResourceInternal(files[i], path);

  // 得到attributes为FileResourceAttributes对象
  ResourceAttributes attributes = (ResourceAttributes) resources.getAttributes(fullPath);
  
  // 为resource所代表的文件打开FileInputStream,并把引用赋给了resource里面的 inputStream
  InputStream binaryStream = resource.streamContent();
  ...
}

// 读取文件内容, 并把字节数组的引用保存在entry
byte[] binaryContent = new byte[contentLength];
int pos = 0;
while (true) {
  int n = binaryStream.read(binaryContent, pos,binaryContent.length - pos);
  if (n <= 0) break;
    pos += n;
}
entry.binaryContent = binaryContent;


// 将这个entry保存在缓存里面
synchronized (resourceEntries) {
  ResourceEntry entry2 = (ResourceEntry) resourceEntries.get(name);
  if (entry2 == null)
    resourceEntries.put(name, entry);
  else
    entry = entry2;
  return entry;
}
```