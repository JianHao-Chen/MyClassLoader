---
title: Tomcat是如何将数据写到客户端的
categories: Tomcat源码
---

我们在自定义的Servlet中，会使用如下代码将数据返回到客户端：
```bash
PrintWriter out = response.getWriter();
out.println("XXX");
```
那么，这段数据需要经历什么才能到达客户端呢？

<!--more-->

---

## 门面设计(题外话)
在用户写的Servlet的doGet()、doPost()方法中,参数为RequestFacade、ResponseFacade。它们有着Request、Response对象的引用(connector包下)。这里用到了门面设计模式。主要是为了让用户的Servlet访问到Request、Response的数据而对于他们的一些敏感数据就隔离开来。


## CoyoteWriter(继承于PrintWriter)

代码中“response.getWriter()”，得到的是CoyoteWriter对象。

#### CoyoteWriter对象的创建

1) CoyoteAdapter的service()方法，调用了connector的createResponse()：
```bash
...
// Create objects
request = (Request) connector.createRequest();
request.setCoyoteRequest(req);
response = (Response) connector.createResponse();
response.setCoyoteResponse(res);
...
```
2) Connector的createResponse()方法：
```bash
public Response createResponse() {
  Response response = new Response();
  response.setConnector(this);
  return (response);
}
```
3) Response的setConnector()方法：
```bash
public void setConnector(Connector connector) {
  this.connector = connector;
  outputBuffer = new OutputBuffer();
  outputStream = new CoyoteOutputStream(outputBuffer);
  writer = new CoyoteWriter(outputBuffer);
}
```

我们可以得到以下结论：
1. CoyoteWriter对象创建的时机是 CoyoteAdapter的service()方法里面，更确切的说是connector创建在容器中使用的request、response的时候。
2. CoyoteWriter对象创建时，有一个outputBuffer作为参数，当调用CoyoteWriter的写方法时，都会写到这个outputBuffer中。


#### PrintWriter的写操作
在文章开头的问题，就是对PrintWriter(实际上是CoyoteWriter)的println()方法的分析：
```bash
PrintWriter out = response.getWriter();
out.println("XXX");
```

PrintWriter的println()方法调用的是print()方法，print()方法又调用了write()方法：
```bash
public void write(String s, int off, int len) {
...
  try {
    ob.write(s, off, len);
  } catch (IOException e) {
    error = true;
  }
  
}
```
其中ob对象就是OutputBuffer类型的，它就是这个CoyoteWriter所关联的Buffer类。

【注意】这里调用CoyoteWriter写的内容，从整个HTTP Response来看，就是 Body 部分。



#### response的finishResponse()方法
当返回到CoyoteAdapter.service()方法时,会调用 response的finishResponse()方法。
finishResponse()方法只有一行，就是调用 OutputBuffer的close()方法 (这个OutputBuffer保存着我们的response body呢)

OutputBuffer的close()方法也比较简单，主要有3件事：
```bash
public void close()throws IOException {
  ...
  coyoteResponse.setContentLength(bb.getLength());
  ...
  doFlush(false);
  ...
  coyoteResponse.finish();
}
```
这里coyoteResponse就是与Http11NioProcessor关联的那一个，并不是用于容器的那一个。


【补充】
在这里，补充一下HTTP Response的格式：

| http/版本|空格|状态码|空格|message|回车|换行|
| ------------- |:-------------:|:-------------:| -----:|
|HTTP/1.1||200||OK|

| 头部字段名|：|值|回车|换行|
| ------------- |:-------------:|:-------------:| -----:|
|Content-Type |:|text/html|||
|回车 |换行||||



#### OutputBuffer的doFlush()方法
doFlush()方法分为2步：

1) 将HTTP Response的状态行和Header写到 socket的buffer
2) 将HTTP Response的 Body 部分写到 socket的buffer

先说说1)的具体过程：
调用coyoteResponse的sendHeaders()，把HTTP Response的状态行和Header写到 InternalNioOutputBuffer 的 buf[]数组中。

 * coyoteResponse有一个hook属性，是一个指向Http11NioProcessor的引用。
 * Http11NioProcessor的outputBuffer属性就是 InternalNioOutputBuffer 对象。

InternalNioOutputBuffer通过 sendStatus()方法，把状态行的字节数组写入自己的buf[]数组里面：
```bash
public void sendStatus() {
  // 通过System.arraycopy()方法，写入“HTTP/1.1”的字节数组
  write(Constants.HTTP_11_BYTES);
  buf[pos++] = Constants.SP;
  
  // Write status code
  int status = response.getStatus();
  switch (status) {
  case 200:
        write(Constants._200_BYTES);
        break;
  case 400:
        write(Constants._400_BYTES);
        break;
  case 404:
        write(Constants._404_BYTES);
        break;
  default:
        write(status);
  }
  buf[pos++] = Constants.SP;
  
  
  // Write message
  ...
}
```

同样道理，对header的写入如下：
```bash
...
MimeHeaders headers = response.getMimeHeaders();
...
int size = headers.size();
for (int i = 0; i < size; i++) {
  outputBuffer.sendHeader(headers.getName(i), headers.getValue(i));
}
outputBuffer.endHeaders();
```
outputBuffer(即InternalNioOutputBuffer)的sendHeader()方法：
```bash
public void sendHeader(MessageBytes name, MessageBytes value) {
  write(name);
  buf[pos++] = Constants.COLON;
  buf[pos++] = Constants.SP;
  write(value);
  buf[pos++] = Constants.CR;
  buf[pos++] = Constants.LF;
}
```

2)的具体过程：
接下来就是将已经保存在InternalNioOutputBuffer的buf[]数组data写入socket关联的buffer里面。

通过调用InternalNioOutputBuffer的addToBB()方法，这个方法就是对Java Nio的 ByteBuffer的写入：
```bash
private synchronized void addToBB(byte[] buf, int offset, int length) throws IOException {

  while (length > 0) {
    int thisTime = length;
    if (socket.getBufHandler().getWriteBuffer().position() ==
        socket.getBufHandler().getWriteBuffer().capacity()
        || socket.getBufHandler().getWriteBuffer().remaining()==0) {
        flushBuffer();
    }
    if (thisTime > socket.getBufHandler().getWriteBuffer().remaining()) {
      thisTime = socket.getBufHandler().getWriteBuffer().remaining();
    }
    socket.getBufHandler().getWriteBuffer().put(buf, offset, thisTime);
    length = length - thisTime;
    offset = offset + thisTime;
    total += thisTime;
  }
  NioEndpoint.KeyAttachment ka = (NioEndpoint.KeyAttachment)socket.getAttachment(false);
  if ( ka!= null ) ka.access();
}
```

到此，HTTP Response的状态行和Header已经写入socket的buffer了。
接着，就是处理HTTP Response的Body部分。

前面说过，Body部分正保存在OutputBuffer中，OutputBuffer调用coyoteResponse的doWrite()方法，这个方法又会调用InternalNioOutputBuffer的doWrite()方法，最后还是调用前面提到过的addToBB()方法：
```bash
public int doWrite(ByteChunk chunk, Response res) 
  throws IOException {
  int len = chunk.getLength();
  int start = chunk.getStart();
  byte[] b = chunk.getBuffer();
  addToBB(b, start, len);
  return chunk.getLength();
}
```


#### coyoteResponse的finish()方法
此时，一个完整的HTTP Response已经写入到socket关联的buffer里面了，coyoteResponse的finish()方法就负责将这个buffer的data发送到客户端。

coyoteResponse的finish()方法，会通过Http11NioProcessor调用 InternalNioOutputBuffer的flushBuffer()方法：
```bash
protected void flushBuffer() throws IOException {
  ...
  if (socket.getBufHandler().getWriteBuffer().position() > 0) {
    socket.getBufHandler().getWriteBuffer().flip();
    writeToSocket(socket.getBufHandler().getWriteBuffer(),true, false);
  }
}
```

对于ByteBuffer的写入后的读取，使用flip()方法这一步，是NIO Buffer的基础。

在看writeToSocket()方法之前，先说说NioSelectorPool的事情。
InternalNioOutputBuffer中有这么一个field：
```bash
protected NioSelectorPool pool;
```
这个field是由Http11NioProcessor的process()方法为它赋值的(在读取socket数据之前)。
```bash
public SocketState process(NioChannel socket) throws IOException {
  ...
  this.socket = socket;
  inputBuffer.setSocket(socket);
  outputBuffer.setSocket(socket);
  inputBuffer.setSelectorPool(endpoint.getSelectorPool());
  outputBuffer.setSelectorPool(endpoint.getSelectorPool());
  ...    
}
```

这个NioSelectorPool就是为了处理当向客户端写Response网络状态不好时的情况。因此，这个writeToSocket()方法通过NioSelectorPool的write()方法写Response，至于这里NioSelectorPool的具体逻辑，已经另外写了一篇博文来介绍。

到此，Http Response已经成功写到socket。





 




