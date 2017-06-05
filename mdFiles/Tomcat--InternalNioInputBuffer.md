---
title: Tomcat是如何读取解析HTTP请求的
categories: Tomcat源码
---

本文将简单展示Tomcat是如何读取解析HTTP请求的。

<!--more-->

---

Tomcat中有一个类叫 InternalNioInputBuffer。它主要提供：
1. 读取TCP数据的方法、保存数据的字节数组。
2. 解析请求行(RequestLine)，头部（Headers）。

## InternalNioInputBuffer的属性
InternalNioInputBuffer有以下主要属性：
```bash
protected Request request;      //与当前buffer关联的tomcat的request
protected MimeHeaders headers;  //用于保存所有的处理出来的header数据
protected byte[] buf;           //用于存数据的字节数组
protected int lastValid;        //表示最后一个可用数据的下标
protected int pos;              //当前读取到的下标  
protected int end;              //用于指向header在buffer的尾部的下标

protected NioSelectorPool pool; //当写response阻塞时，会用到它
protected InputBuffer inputStreamInputBuffer;
```
inputStreamInputBuffer是SocketInputBuffer实例，而SocketInputBuffer是InternalNioInputBuffer的内部类。
这个SocketInputBuffer的作用是：
代理当前对象的读取数据的方法，将方法暴露给外面的对象，而不是直接将InternalNioInputBuffer的读取数据的方法暴露给外面的对象。



## InternalNioInputBuffer的readSocket()方法
readSocket()方法的关键代码：
```bash
// 调用socket关联的nio的buf的clear方法，待会可以将socket里读取的数据写进去了
socket.getBufHandler().getReadBuffer().clear();
// 非阻塞的读数据,实际上调用的是SocketChannel的read()方法。
nRead = socket.read(socket.getBufHandler().getReadBuffer());
// 调用ByteBuffer的flip()方法
socket.getBufHandler().getReadBuffer().flip();
// 将ByteBuffer里面的数据转移到buf[]数组。
socket.getBufHandler().getReadBuffer().get(buf,pos,nRead);
```


## InternalNioInputBuffer的parseRequestLine()方法
这个方法就是用于解析请求行。
实现逻辑就是利用上面提到的readSocket()方法，将从socket读到的，已经保存在buf[]的字节数组进行分析。

【补充】
HTTP请求行的格式：

| 请求方法|空格|URL|空格|协议版本|回车|换行|
| ------------- |:-------------:|:-------------:| -----:|
|GET/POST等方法||example/helloworld.html||HTTP/1.1|


下面列出一些代码：

#### 解析方法名
```bash
// Reading the method name
// Method name is always US-ASCII
//
boolean space = false;
while (!space) {
  // Read new bytes if needed
  if (pos >= lastValid) {
    // 如果之前读取socket的数据不完整，fill方法里面会调用readSocket()方法读取剩下的数据。
    if (!fill(true, false)) //request line parsing
      return false;
  }
  // Spec says no CR or LF in method name
  if (buf[pos] == Constants.CR || buf[pos] == Constants.LF) {
    throw new IllegalArgumentException(sm.getString("iib.invalidmethod"));
  }
  
  if (buf[pos] == Constants.SP || buf[pos] == Constants.HT) {
    space = true;
    request.method().setBytes(buf, parsingRequestLineStart, pos - parsingRequestLineStart);
  }
  pos++;
}
```
其中，request.method()是一个MessageBytes类型的对象。关于MessageBytes，请看本文末尾的【补充】。

#### 方法名与URI之间的空格
主要是因为HTTP规范允许这里的空格有多个，或者是 HT。
```bash
// Spec says single SP but also be tolerant of multiple and/or HT
boolean space = true;
while (space) {
  // Read new bytes if needed
  if (pos >= lastValid) {
    if (!fill(true, false)) //request line parsing
      return false;
  }
  if (buf[pos] == Constants.SP || buf[pos] == Constants.HT) {
    pos++;
  } else {
      space = false;
    }
}
```
后面对 URI，协议版本的解析代码就不列举出来了。


## InternalNioInputBuffer的parseHeaders()方法
这个方法就是用于解析请求头。
实现逻辑就是利用上面提到的readSocket()方法，将从socket读到的，已经保存在buf[]的字节数组进行分析。

【补充】
HTTP请求头的格式：

| 头部字段名|：|值|回车|换行|
| ------------- |:-------------:|:-------------:| -----:|
|accept |:|text/html|||
|accept-language |:|en-US|||

至于解析请求头的代码，与解析请求行的相似，也不列出来了。


## 总结
当 HTTP请求被读取并解析完成后，会去调用prepareRequest()，对刚才得到的request进行一些处理，
例如：
检查 Headers里面头部字段名为"connection"的header，根据它的值为"keep-alive"还是"close"，设置这个Http11NioProcessor的field "keepAlive"，这个会影响到是否使用HTTP的keepAlive技术。

然后，把request交给CoyoteAdapter处理。


【补充】
MessageBytes用于表示HTTP请求的字节数组的子部分，例如这里的方法名。
MessageBytes内部有以下成员：
```bash
...
// Internal objects to represent array + offset, and specific methods
private ByteChunk byteC=new ByteChunk();
private CharChunk charC=new CharChunk();

// String
private String strValue;
...
```

值得注意的是，这个MessageBytes的setBytes()方法：
```bash
public void setBytes(byte[] b, int off, int len) {
  byteC.setBytes( b, off, len );
  type=T_BYTES;
  ...
}
```
其中，ByteChunk的setBytes()方法：
```bash
public void setBytes(byte[] b, int off, int len) {
  buff = b;
  start = off;
  end = start+ len;
  isSet=true;
}
```
可以说，ByteChunk保存了InternalNioInputBuffer的buf[]引用，并通过start，end来定位这个buf[]的一个子数组，而且ByteChunk中还有Charset类把字节数组转为String，Ascii类将字节数组转为int，long。




