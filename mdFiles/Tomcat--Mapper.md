---
title: Tomcat的Mapper
categories: Tomcat源码
---

Mapper 提供请求路径的路由映射，根据某个请求路径通过计算得到相应的Servlet（Wrapper）.
下面将介绍Mapper的具体实现。

<!--more-->

---

## 结构层次

#### 定义在 Mapper里面的内部类
本文分析的是Tomcat 6版本的，新版本的Tomcat在此的代码有些不同。

###### MapElement
```bash
MapElement {
  String name = null;     //名字
  Object object = null;   //具体的容器，例如是host对象，context对象或者wrapper对象
}
```

###### Host
```bash
Host extends MapElement{
  ContextList contextList = null;
}
```

###### ContextList
```bash
ContextList{
  Context[] contexts = new Context[0];
  int nesting = 0;
}
```

###### Context
```bash
Context extends MapElement{
  public String path = null;   //path
  public String[] welcomeResources = new String[0];   //welcome的数据
  public Wrapper defaultWrapper = null;               //默认的wrapper
  public Wrapper[] exactWrappers = new Wrapper[0];    //对wrapper的精确的map
  public Wrapper[] wildcardWrappers = new Wrapper[0]; //基于通配符的map
  public Wrapper[] extensionWrappers = new Wrapper[0];//基于扩展名的map
}
```


#### Mapper的定义
```bash
protected Host[] hosts = new Host[0];       // 对host的map信息
protected String defaultHostName = null;    // engine使用的默认的host名字
protected Context context = new Context();  //context的map信息
```


#### Mapper数据的读入

在Tomcat的各个容器(Engin、Host、Context)初始化的最后,会将自己注册到JMX,随后在Connector组件启动时,通过mapperListener.init(),会分别从JmxMBeanServer查询出各个容器,从而分别调用mapper的addXX()方法,来形成从Host到Wrapper的各级容器的快照。

1) 作为Connector的成员,在Connector对象初始化时创建Mapper对象，还有创建了MapperListener对象。
```bash
protected Mapper mapper = new Mapper();

protected MapperListener mapperListener = new MapperListener(mapper, this);
```

2) 在Connector的start()方法调用 mapperListener.init()，其中包含了：
```bash
registerEngine();
registerHost();     -->     mapper.addHost()
registerContext();  -->     mapper.addContext()
registerWrapper();  -->     mapper.addWrapper()
```
addHost()方法： 
创建新的Mapper.$Host对象,并保存到hosts数组中,hosts数组的元素是按照名字排序的。

addContext() ： 
先根据hostName来查找要添加到的mappedHost对象,然后这个mappedHost对象有一个ContextList,其实也就是一个MappedContext对象数组，然后接着就根据当年context的名字，创建一个新的MappedContext对象根据context的path的排序加入到contextList数组里面。

addWrapper() ：
与addContext()相似，只是每个Context对象有着Wrapper[]数组：exactWrappers、wildcardWrappers、extensionWrappers。还有defaultWrapper。主要是根据需要匹配的path做出不同的选择：
1. 如果path是通配符类型的(/*)，那么将对应的Wrapper加入到当前Context的wildcardWrappers里面。
2. 如果path是扩展名类型的(*.)，那么将对应的Wrapper加入到当前Context的extensionWrappers里面。
3. 如果path是默认类型的(/)，那么将对应的Wrapper赋值给Context的defaultWrapper。
4. 最后path是精确类型的，那么将对应的Wrapper加入到当前Context的exactWrappers里面。



#### Mapper的使用--请求路径的路由映射

入口 ： Mapper的map(MessageBytes host,MessageBytes uri,MappingData mappingData)方法。
示例输入 ： (localhost , /examples/index.html , mappingData)
mappingData用于保存这次 mapping的结果。

map()方法主要调用了internalMap()方法，直接介绍这个internalMap()方法：
```bash
private final void internalMap(CharChunk host, CharChunk uri,MappingData mappingData)
  throws Exception {
  
  Context[] contexts = null;
  Context context = null;
  
  /*
  *  1. 找到相应的host,并取出这个host的contextList,保存在mappingData
  */
  Host[] hosts = this.hosts;
  int pos = findIgnoreCase(hosts, host);
  if ((pos != -1) && (host.equalsIgnoreCase(hosts[pos].name))) {
    mappingData.host = hosts[pos].object;
    contexts = hosts[pos].contextList.contexts;
  }

  /*
  *  2. 找到相应的context(即在contexts[]数组中找到name为"examples"的一项),
  *     保存在context变量且保存在mappingData
  */
  ...
  context = contexts[pos];
  mappingData.context = context.object;
  mappingData.contextPath.setString(context.name);
  
  /*
  *  3. 找出相应的wrapper
  *     (1) 按照精确路径来找(即context的exactWrappers)
  *     (2) 按照通配路径(/*)来找(即context的exactWrappers)
  *     (3) 按照扩展路径(*.)来找(即context的extensionWrappers)
  *     (4) 都找不到才交给defaultWrapper处理
  */
  ....
}
```


