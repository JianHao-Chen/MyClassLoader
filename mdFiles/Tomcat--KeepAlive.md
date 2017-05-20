---
title: Tomcat对Keep-Alive的支持
categories: Tomcat源码
---

本文将分析Tomcat是如何支持Keep-Alive的。

<!--more-->

---

## Keep-Alive
**Keep-Alive**又称为persistent connection，或Http connection reuse。主要作用是用一个TCP连接来处理多个HTTP的请求和响应。
【好处】：
节省了创建和结束多个连接的时间,可以更专注于数据的传输,页面可以更快的渲染,同时也降低了服务器的负载。
【坏处】：
影响了性能,因为在处理暂停期间,本来可以释放的资源仍旧被占用。

浏览器默认是会在request的Header中加入：`connection = Keep-Alive`来表示浏览器是支持使用Keep-Alive的。


## Tomcat对Keep-Alive的支持

(1) 在处理请求阶段,解析解析请求行(RequestLine)，头部（Headers）之后，在Http11NioProcessor的prepareRequest()方法中对头部的一些属性进行处理：
```bash
protected void prepareRequest() {
  ...
  /*
  * 取出的头部key为"connection"的field，根据它的值是“close”还是
  * “Keep-Alive”，对变量keepAlive赋值
  */
  MimeHeaders headers = request.getMimeHeaders();
  MessageBytes connectionValueMB = headers.getValue("connection");
  if (connectionValueMB != null) {
    ByteChunk connectionValueBC = connectionValueMB.getByteChunk();
    if (findBytes(connectionValueBC, Constants.CLOSE_BYTES) != -1) 
      keepAlive = false;
    else if (findBytes(connectionValueBC,Constants.KEEPALIVE_BYTES) != -1)
      keepAlive = true;
  }
  ...
}
```

(2) Tomcat对keep alive的最大连接数有限制。在Http11NioProcessor中有成员变量：
```bash
protected int maxKeepAliveRequests = -1;
```
maxKeepAliveRequests会在Http11NioProcessor创建的时候，被赋默认值100。
在Http11NioProcessor的process()方法里面有关这个maxKeepAliveRequests的操作：
```bash
 public SocketState process(NioChannel socket)throws IOException {
   ...
   int keepAliveLeft = maxKeepAliveRequests;
   boolean keptAlive = false;
   while (!error && keepAlive && !comet) {
     try{
       ...(解析请求行、请求头)
     }
     catch(){;}
     ...
     if (maxKeepAliveRequests > 0 && --keepAliveLeft == 0)
       keepAlive = false;
     ...
     adapter.service(request, response);
     ...
   }
 }
```
process()方法对maxKeepAliveRequests处理的策略是：在timeout时间内有新的连接过来，keepAliveLeft会自动减1，直到为0，跳出while循环，退出process()方法，断掉TCP连接。

## keepAlive保存连接
先看当从process()方法退出时，回到Http11ConnectionHandler的process()方法：
```bash
SocketState state = processor.process(socket);
if (state == SocketState.LONG) {
  connections.put(socket, processor);
  socket.getPoller().add(socket);
}
else if (state == SocketState.OPEN) {
  release(socket, processor);
  socket.getPoller().add(socket);
}
else {
   release(socket, processor);
}
```
当请求数目还未达到maxKeepAliveRequests，从process()方法返回的SocketState应该是SocketState.LONG，随后，Http11ConnectionHandler会做2件事：
1. 把socket对应的processor保存起来
2. 把socket封装为PollerEvent,加到Poller的events队列。
Poller会负责处理PollerEvent-->                 把socket对应的SelectionKey的interestOps置为1，表示关注读事件。



## Tomcat中socket(或者说NioChannel)的关闭

(1) 情况1(keepAliveLeft==0)
keepAliveLeft初始值为maxKeepAliveRequests,随后每一次的request处理,都减1,当keepAliveLeft==0,Tomcat就会关闭这个链接。这种情况在前面已经提过，现在说说在退出process()方法后，回到SocketProcessor中的run()方法：
```bash
public void run() {
  ...
  // 将会得到closed==true
  boolean closed =
     handler.process(socket)==Handler.SocketState.CLOSED
 // 随后调用cancelledKey()方法
 socket.getPoller().cancelledKey(key, SocketStatus.ERROR, false);
 ...
}
```
通过Poller的cancelledKey()方法关闭连接，cancelledKey()方法会在后面再说。


(2) 情况2(keepalive的timeout，而且请求数目还未达到maxKeepAliveRequests)
Poller线程会在run()里面调用timeout()方法,用于检查每个连接是否timeout：
```bash
protected void timeout(int keyCount, boolean hasEvents) {
  ...
  Set<SelectionKey> keys = selector.keys();
  for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext(); ) {
    SelectionKey key = iter.next();
    try {
      ...
      long delta = now - ka.getLastAccess();
      long timeout = ka.getTimeout();
      boolean isTimedout = delta > timeout;
      ...
      if(isTimedout){
        key.interestOps(0);
        cancelledKey(key, SocketStatus.TIMEOUT,true);
      }
    }
  }
}
```


(3) 情况3(浏览器发送EOF过来)
前半部分就和普通的读取数据一样,直到读到EOF,然后从process()方法返回得到SocketState.CLOSED。对此，Http11ConnectionHandler对state == CLOSED的处理是与情况1一样。


## Poller的cancelledKey
```bash
public void cancelledKey(SelectionKey key, SocketStatus status, boolean dispatch) {

  try {
    if ( key == null ) return;//nothing to do
    KeyAttachment ka = (KeyAttachment) key.attachment();
    ...
    key.attach(null);
    // 处理connections中的此Processor，并把它放入缓存recycledProcessors
    if (ka!=null) handler.release(ka.getChannel());
    
 // 处理 SelectionKey
 // 先判断这个SelectionKey是否有效:
 // 一个SelectionKey自从创建以后,就保持有效,直到:
 //   (1) 它的cancel()方法被调用
 //   (2) 它的channel被关闭了
 //   (3) 它的selector被关闭了
 // 如果SelectionKey有效,就调用SelectionKey的cancel()方法
 // cancel()方法造成这个SelectionKey被加入到selector的cancelled-key集合
    if (key.isValid()) key.cancel();
    
    // 调用 SocketChannel的close()方法
    if (key.channel().isOpen()) 
    try {
      key.channel().close();
    }
    catch (Exception ignore){}
    
    // 还要调用SocketChannel所关联的Socket的close()方法
    try {
      if (ka!=null) 
        ka.channel.close(true);
    }catch (Exception ignore){}
    
    // 处理(清理)Sendfile
    // 需要关闭还在打开的文件通道
    if (
      ka!=null && 
      ka.getSendfileData()!=null && 
      ka.getSendfileData().fchannel!=null && 
      ka.getSendfileData().fchannel.isOpen()
    ) 
      ka.getSendfileData().fchannel.close();
  }
  catch (Throwable e) {
  }
}
```