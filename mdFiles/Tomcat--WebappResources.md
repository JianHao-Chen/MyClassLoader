---
title: Tomcat的webapp资源
categories: Tomcat源码
---

本文将介绍Tomcat是如何表示及操作webapp资源的。

<!--more-->

---

## webapp资源的相关数据结构

在StandardContext里面有一个field :
```bash
private transient DirContext resources = null;
```
在StandardContext启动的时候,会创建一个ProxyDirContext对象(然后赋值给resources引用),用于表示这个Webapp的资源。
这个ProxyDirContext主要有以下属性：
```bash
ResourceCache cache = null; // 作为资源的缓存

// 这是一个FileDirContext对象,代表这个Webapp的根目录上下文(如...webapps\examples)
// 还保存一些setting的值(是否使用缓存、缓存的总大小、缓存每个Object的大小)
DirContext dirContext;
```

ResourceCache有以下属性：
```bash
// CacheEntry代表一个具体的资源
CacheEntry[] cache = new CacheEntry[0];

// Max size of resources which will have their content cached.
protected int cacheMaxSize = 10240; // 10 MB

// Current cache size in KB.
protected int cacheSize = 0;

// Number of accesses to the cache.
protected long accessCount = 0;

// Number of cache hits.
protected long hitsCount = 0;
...
```


## 对资源的查找

对资源的查找，入口就是ProxyDirContext的lookup()方法。例如，WebappClassLoader在查找本地仓库时，会通过如下代码查找：
```bash
String fullPath = repositories[i] + path;
Object lookupResult = resources.lookup(fullPath);
```
resources就是ProxyDirContext的引用。

#### ProxyDirContext的lookup()方法

```bash
public Object lookup(String name){

  /* 1. 先在缓存中查找 */
  CacheEntry entry = cacheLookup(name);
  if (entry != null) {
    if (!entry.exists)
      throw notFoundException;
    if (entry.resource != null)
      return entry.resource;
    else
      return entry.context;
  }
  
  /*  2. 缓存中找不到，再在访问磁盘  */
  
  // dirContext是FileDirContext对象的引用，dirContext的lookup()方法，会做以下事情:
  // 1) 检查名字为name的文件是否存在、可读，以及是否正在访问这个WebApp的root以为的路径。
  //    检查通过的话，会得到File对象。
  // 2) 如果这个File对象代表的是文件夹那么返回一个FileDirContext对象。
  //    如果这个File对象代表的是文件，那么返回一个FileResource对象。
  Object object = dirContext.lookup(parseName(name));
  ...
}
```


#### ProxyDirContext的cacheLookup()方法
省略部分代码
```bash
protected CacheEntry cacheLookup(String name) {

  // 保存了nonCacheable数组,在cacheLookup()方法里面先判断,对于
  // 这2个folder下面的资源是不缓存的。cacheLookup()方法会直接退出。
  // String[] nonCacheable = { "/WEB-INF/lib/", "/WEB-INF/classes/" };
  for (int i = 0; i < nonCacheable.length; i++) {
    if (name.startsWith(nonCacheable[i])) return (null);
  }
  
  // 开始在cache里面查找, cache是ResourceCache对象。
  CacheEntry cacheEntry = cache.lookup(name);
  
  // 在cache里面找不到就生成一个cacheEntry并把新的Entry放入cache
  if (cacheEntry == null) {   
    cacheEntry = new CacheEntry();
    cacheEntry.name = name;
    // 把新的Entry加入到缓存中
    cacheLoad(cacheEntry);
  }
  else {
    // 需要检查这个cacheEntry是否有效
    if (!validate(cacheEntry)) {
      if (!revalidate(cacheEntry)) {
        // 如果已经失效，就把这个Entry从缓存中移除
        cacheUnload(cacheEntry.name);
        return (null);
      }
      else {
        // 还有效，update这个Entry的timestamp
        cacheEntry.timestamp = System.currentTimeMillis() + cacheTTL;
      }
    }
  }
  
  return (cacheEntry);
}
```


#### ResourceCache的lookup()方法

```bash
public CacheEntry lookup(String name) {

  // 获取当前的CacheEntry数组
  // cache 是 ResourceCache下的CacheEntry[]数组
  CacheEntry[] currentCache = cache;
  // 在当前CacheEntry数组中查找给定名字的 Entry，find()方法用的是二分查找。
  int pos = find(currentCache, name);
  
  if ((pos != -1) && (name.equals(currentCache[pos].name)))
    cacheEntry = currentCache[pos];
  ....
  return cacheEntry;
}
```


#### ProxyDirContext的cacheLoad()方法
省略了部分代码，并加入自己的注释。
```bash
protected void cacheLoad(CacheEntry entry) {
  
  // 获取对应资源的属性
  // dirContext是FileDirContext的对象
  Attributes attributes = dirContext.getAttributes(entry.name);
  entry.attributes = (ResourceAttributes) attributes;
  
  // 获取代表对应资源的对象
  // 根据找到的对象是一个文件夹还是一个具体的文件,对应entry的resource或context
  Object object = dirContext.lookup(name);
  if (object instanceof DirContext)
    entry.context = (DirContext) object;
  else if (object instanceof Resource)
    entry.resource = (Resource) object;
  
  // 获取资源的content
  if( //先作以下判断,只有符合才会去读取content：
      // (a)对应的资源是文件而不是文件夹
      // (b)这个资源关联的cache entry中content为Null,即资源从来未被读取content
      // (c)这个资源的大小 >0 并且 < 512KB (cacheObjectMaxSize默认是512)
      (entry.resource != null) &&
      (entry.resource.getContent() == null) &&
      (entry.attributes.getContentLength() >= 0) &&
      (entry.attributes.getContentLength() <cacheObjectMaxSize * 1024)
    ){
      int length = (int) entry.attributes.getContentLength();
      // entry的大小是 (resource.size()+1),以 KB为单位
      entry.size += (entry.attributes.getContentLength() / 1024);
      
      InputStream is = entry.resource.streamContent();
      int pos = 0;
      byte[] b = new byte[length];
      
      while (pos < length) {
        int n = is.read(b, pos, length - pos);
        if (n < 0) break;
        pos = pos + n;
      }
      entry.resource.setContent(b);
    }
  
  // 将这个entry加载入cache(ResourceCache对象) 
  synchronized (cache) {
    if (
        (cache.lookup(name) == null) &&
        (cache.allocate(entry.size)) ){  // 是否有足够的空间加入这个entry
        
        cache.load(entry);  // 把entry加入到CacheEntry[]数组
    }
  }
}
```


## 移除失效缓存

#### 检查，判断entry是否失效
在前面ProxyDirContext的cacheLookup()方法中，调用了validate()，revalidate()方法进行检查Entry的有效性。

其中validate()仅仅是检查一下entry的各个field进而判断这个entry的有效性：
```bash
protected boolean validate(CacheEntry entry) {
  if (((!entry.exists)
        || (entry.context != null)
        || ((entry.resource != null)
            && (entry.resource.getContent() != null)))
        && (System.currentTimeMillis() < entry.timestamp)) {
        return true;
  }
  return false;
}
```

即使validate()方法返回false，还需要调用revalidate()再次验证这个entry是否真的失效了。

revalidate通过检查缓存中 ResourceAttributes的LastModified Date和ContentLength与现在文件的比较,不符合就返回false(文件已被删除也返回false);
```bash
protected boolean revalidate(CacheEntry entry) {
  ...
  long lastModified = entry.attributes.getLastModified();
  long contentLength = entry.attributes.getContentLength();
  ...
  Attributes attributes = dirContext.getAttributes(entry.name);
  long lastModified2 = attributes.getLastModified();
  long contentLength2 = attributes.getContentLength();
  ...
  return (lastModified == lastModified2) && (contentLength == contentLength2);
}
```

#### 从缓存中移除

ProxyDirContext的cacheUnload(String name)方法：
```bash
...
synchronized (cache) {
  boolean result = cache.unload(name);
  return result;
}
```


ResourceCache的unload(String name)方法：
```bash
// removeCache()方法使用System.arraycopy来移除数组中的一项
CacheEntry removedEntry = removeCache(name);
if (removedEntry != null) {
  cacheSize -= removedEntry.size;
  return true;
}
return false;
```