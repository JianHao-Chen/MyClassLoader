---
title: Tomcat连接器
categories: Tomcat源码
---
这篇文章会分析Tomcat的Connector(只讨论它使用NIO的实现方式)。
<!--more-->

---

## NioEndpoint
NioEndpoint是Connector中负责接收处理socket的主要模块。它有几个组成部分,下面将介绍它们。

### ★ Acceptor
Acceptor用于接收到来的HTTP请求。它执行流程如下：

1. 接收请求
它调用ServerSocketChannel的accept()方法,得到SocketChannel对象：
2. 将得到的SocketChannel对象进行配置(NoBlocking、BufferSize、KeepAlive等等)，封装成一个NioChannel对象
3. 新建一个KeyAttachment对象，将它和NioChannel对象封装为PollerEvent对象,加入到Poller的events队列。
4. Acceptor继续监听新的HTTP请求。

<font color=red>问题：为什么需要这个KeyAttachment对象？</font><br>
<font color=blue>回答：</font>

讲解一下它有的几个主要属性就基本明白了:

*  NioChannel的引用：
socket和其相关的buffer的引用
*  lastAccess
记录这个socket的最近访问时间
*  writeLatch
用于当不能写response到socket时,将此socket注册到“副Selector”后,调用writeLatch的await()方法阻塞当前线程,直到BlockPoller线程处理SelectionKey的可写事件时,调用writeLatch的countDown()方法唤醒Worker线程继续向socket写数据。


### ★ Poller
Poller是NIO实现的主要线程。它执行流程如下：

1. 从events队列拿出PollerEvent对象，然后将此对象中的channel以OP_READ事件注册到Selector中
``` bash
socket.getIOChannel().register(
  key.getPoller().getSelector(), SelectionKey.OP_READ, key);
```
2. Selector执行select操作，并得到selected-key set
``` bash
socket.getIOChannel().register(
    key.getPoller().getSelector(), SelectionKey.OP_READ, key);
...
Iterator iterator = selector.selectedKeys().iterator()
```
3. 遍历并处理每一个selected-key
``` bash
while (iterator != null && iterator.hasNext()) {
	SelectionKey sk = (SelectionKey) iterator.next();
	KeyAttachment attachment = (KeyAttachment)sk.attachment();
    ...
	iterator.remove();
    processKey(sk, attachment);
}
```
处理每一个selected-key的逻辑都在processKey()方法里面：
``` bash
// 从KeyAttachment里取得NioChannel的引用。
NioChannel channel = attachment.getChannel();
// 将已经ready的事件从感兴趣的事件中移除，这一步很重要！
unreg(sk, attachment,sk.readyOps());
// 创建SocketProcessor对象,把它放入ThreadPoolExecutor运行。
SocketProcessor socketProcessor = new SocketProcessor(socket,status);
executor.execute(socketProcessor);
```

整个过程就是典型的NIO实现。

<font color=red>问题：在处理每个就绪的SelectionKey时，为什么需要将已经ready的事件从感兴趣的事件中移除？</font><br>
<font color=blue>回答：</font>

“移除”即unreg()方法的具体操作就是:
``` bash
sk.interestOps(sk.interestOps()& (~readyOps));
```
这里调用SelectionKey的interestOps()方法:
``` bash
SelectionKey interestOps(int ops);
```
这个方法将这个SelectionKey的interest集合设置为ops。

至于为什么要移除，这个在解释Java NIO中Selector的工作原理的时候回答这个问题更合适，现在不妨先记住 :<font color=green>
如果没有将已经ready的事件从感兴趣的事件中移除，那么这个Channel的这次有数据可读的事件依然保留着(即使这次我们已经知道了，已经交给SocketProcessor线程处理了).那么在selector的下一次select操作,这个Channel的这次有数据会被误以为又有新数据到达,又交给另一个SocketProcessor线程处理。</font>

### ★ Worker(也就是SocketProcessor)
用于处理poller传过来的socket,处理流程是:

1. 交给Http11ConnectionHandler处理
``` bash
// handler是Http11ConnectionHandler类型的对象
state = handler.process(socket);
```
2. 从Http11ConnectionHandler里面取出Http11NioProcessor对象，交给Http11NioProcessor处理
``` bash
processor = connections.remove(socket);
if (processor == null) {
	processor = recycledProcessors.poll();
}
if (processor == null) {
	processor = createProcessor();
}
...
SocketState state = processor.process(socket);
```
3. <font color=purple >**Http11NioProcessor的这个process方法真正开始处理socket :**</font><br>
(1) 它通过InternalNioInputBuffer完成从socket读取数据并解析成Request对象
(2) 它将Request对象和一个新建的Response对象交给CoyoteAdapter处理,CoyoteAdapter将会负责:

*    创建用于在容器的request和response
*    分派到相应的servlet并完成逻辑
*    将response通过socket发回client 

关于InternalNioInputBuffer和CoyoteAdapter会另写博客来记下它们的内部逻辑。

### ★ NioSelectorPool
关于NioSelectorPool,准备在另外一篇博文再说，现在先简单的说 :
Tomcat创建了另外一个Selector(先把这个Selector称为“副Selector”)。当网络状态不好时，将socket注册OP_WRITE事件到副Selector，由BlockPoller线程不断轮询这个副Selector，直到这个socket的写状态恢复了，Worker线程继续向socket写数据。