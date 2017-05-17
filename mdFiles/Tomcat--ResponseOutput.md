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
doFlush()方法主要做3件事：

1) 调用coyoteResponse的sendHeaders()，把HTTP Response的状态行和Header写到 InternalNioOutputBuffer 的 buf[]数组中。

 * coyoteResponse有一个hook属性，是一个指向Http11NioProcessor的引用。
 * Http11NioProcessor的outputBuffer属性就是 InternalNioOutputBuffer 对象。





