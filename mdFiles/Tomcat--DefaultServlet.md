---
title: Tomcat是如何处理静态资源的
categories: Tomcat源码
---

DefaultServlet是Tomcat提供的一个用于处理静态资源的Servlet。
本文将分析对于静态资源的2种返回方式。

<!--more-->

---

## 第一种：将文件内容copy到output stream

在DefaultServlet的serveResource()方法里面，有以下代码：
```bash
if (!checkSendfile(request, response, cacheEntry, contentLength, null))
  copy(cacheEntry, renderResult, ostream);
```
这里，checkSendfile()方法用于判断是否使用Sendfile功能，如果条件不满足,就把内容copy到output stream。

【注意】
这里output stream是HttpServletResponse的OutputStream。

#### copy()方法
```bash
protected void copy(CacheEntry cacheEntry, InputStream is,ServletOutputStream ostream)
  throws IOException {
  ...
  if (cacheEntry.resource != null) {
    byte buffer[] = cacheEntry.resource.getContent();
    if (buffer != null) {
      ostream.write(buffer, 0, buffer.length);
      return;
    }
  }
  ...
}
```



## 第二种：使用 Sendfile功能

#### checkSendfile()方法
对于满足使用Sendfile功能的条件：
 * sendfileSize>0 && length > sendfileSize (sendfileSize默认是48KB , length是文件大小) 
 * entry.attributes.getCanonicalPath() != null (这里的path是文件的绝对路径)
 * ...省略其他
 
如果条件满足,就在request里面添加以下属性,并且返回 true：
```bash
request.setAttribute("org.apache.tomcat.sendfile.filename",entry.attributes.getCanonicalPath());

// Range range参数,可以用于指定发送文件的开始、结束位置
if (range == null) {
  request.setAttribute("org.apache.tomcat.sendfile.start", new Long(0L));
  request.setAttribute("org.apache.tomcat.sendfile.end", new Long(length));
} else {
  request.setAttribute("org.apache.tomcat.sendfile.start", new Long(range.start));
  request.setAttribute("org.apache.tomcat.sendfile.end", new Long(range.end + 1));
}
```
否则返回 false;



#### 对新加的属性的处理：

1. 从 response.finishResponse()开始,最后调用到Http11NioProcessor的prepareResponse()方法,里面会处理 sendfile属性

  ```bash
  // Sendfile support
  if (this.endpoint.getUseSendfile()) {
    String fileName = (String) request.getAttribute("org.apache.tomcat.sendfile.filename");
    if (fileName != null) {
      // No entity body sent here
      outputBuffer.addActiveFilter(outputFilters[Constants.VOID_FILTER]);
      contentDelimitation = true;
      sendfileData = new NioEndpoint.SendfileData();
      sendfileData.fileName = fileName;
      sendfileData.pos = ((Long) request.getAttribute("org.apache.tomcat.sendfile.start")).longValue();
      sendfileData.length = ((Long) request.getAttribute("org.apache.tomcat.sendfile.end")).longValue() - sendfileData.pos;
    }
}
  ```
2. Response的状态行(HTTP协议版本号， 状态码， 状态消息)的生成和写入SocketChannel的byteBuffer.  这一步与平常的一样。

3. 本来是应该将response中的content写入SocketChannel的byteBuffer。但是这是response中的content为空,因为会留到后面再写。

4. 当从adapter.service(request, response)返回到Http11NioProcessor,通过Poller完成Sendfile功能

  ```bash
  // Do sendfile as needed: add socket to sendfile and end
  if (sendfileData != null && !error) {
    KeyAttachment ka = (KeyAttachment)socket.getAttachment(false);
  ka.setSendfileData(sendfileData);
  sendfileData.keepAlive = keepAlive;
  SelectionKey key = socket.getIOChannel().keyFor(socket.getPoller().getSelector());
  //do the first write on this thread, might as well
  openSocket = socket.getPoller().processSendfile(key,ka,true,true);
  break;
}
 ```
5. Poller完成Sendfile功能:

 ```bash
//省略部分代码
SendfileData sd = attachment.getSendfileData();
File f = new File(sd.fileName);
sd.fchannel = new FileInputStream(f).getChannel();
NioChannel sc = attachment.getChannel();
WritableByteChannel wc =(WritableByteChannel)sc.getIOChannel();
long written = sd.fchannel.transferTo(sd.pos,sd.length,wc);
 ```

## FileChannel的transferTo

<font color=red>FileChannel的transferTo方法会利用操作系统的sendfile系统调用来将磁盘文件输出到网络。</font>

#### 原理分析
sendfile系统调用,用到了Linux中所谓的"零拷贝"特性。
"零拷贝"不是指完全不用copy,而是不用CPU参与。

1. 使用read和write方式的时候，将文件输出到网络所涉及的操作：

 ```bash
  read(file, tmp_buf, len);
  write(socket, tmp_buf, len);
 ```
 1. 系统调用read导致了从用户空间到内核空间的上下文切换。DMA模块从磁盘中读取文件内容，并将其存储在内核空间的缓冲区内，完成了第1次复制。
 2. 数据从内核空间缓冲区复制到用户空间缓冲区，之后系统调用read返回，这导致了从内核空间向用户空间的上下文切换。此时，需要的数据已存放在指定的用户空间缓冲区内(参数tmp_buf)，程序可以继续下面的操作。
 3. 系统调用write导致从用户空间到内核空间的上下文切换。数据从用户空间缓冲区被再次复制到内核空间缓冲区，完成了第3次复制。不过，这次数据存放在内核空间中与使用的socket相关的特定缓冲区中，而不是步骤一中的缓冲区。
 4. 系统调用返回，导致了第4次上下文切换。第4次复制在DMA模块将数据从内核空间缓冲区传递至协议引擎的时候发生，这与我们的代码的执行是独立且异步发生的。
</br>

2. 使用Sendfile系统调用，将文件输出到网络所涉及的操作:
sendfile系统调用的引入，不仅减少了数据复制，还减少了上下文切换的次数。
 1. sendfile系统调用导致文件内容通过DMA模块被复制到某个内核缓冲区，之后再被复制到与socket相关联的缓冲区内。
 2. 当DMA模块将位于socket相关联缓冲区中的数据传递给协议引擎时，执行第3次复制。

 
3. 特别的,如果网络适配器支持聚合操作特性,那么Sendfile所涉及的操作:
 1. sendfile系统调用导致文件内容通过DMA模块被复制到内核缓冲区中。 
 2. 数据并未被复制到socket关联的缓冲区内。取而代之的是，只有记录数据位置和长度的描述符被加入到socket缓冲区中。DMA模块将数据直接从内核缓冲区传递给协议引擎，从而消除了遗留的最后一次复制。
 

 


