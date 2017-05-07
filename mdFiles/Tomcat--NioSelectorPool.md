---
title: Tomcat的NioSelectorPool
categories: Tomcat源码
---
在Tomcat向client端写response时,网络状态不好时，就会用到NioSelectorPool了。
<!--more-->

---

## NioSelectorPool的write()方法

``` bash
public int write(ByteBuffer buf, NioChannel socket, long writeTimeout,MutableInteger lastWrite)
  throws IOException {

  SelectionKey key = socket.getIOChannel().keyFor(socket.getPoller().getSelector());
  if ( key == null ) throw new IOException("Key no longer registered");
  KeyReference reference = new KeyReference();
  KeyAttachment att = (KeyAttachment) key.attachment();

  int written = 0;
  boolean timedout = false;
  int keycount = 1;
  long time = System.currentTimeMillis(); //start the timeout timer
  try {
    while ( (!timedout) && buf.hasRemaining()) {
    if (keycount > 0) {
      int bufferSize = socket.sc.socket().getSendBufferSize();
      int cnt = socket.write(buf); //write the data
      lastWrite.set(cnt);
      if (cnt == -1)
        throw new EOFException();
      written += cnt;
      if (cnt > 0) {
        time = System.currentTimeMillis(); //reset our timeout timer
        continue; //we successfully wrote, try again without a selector
      }
    }

    try {
    /*
    *  如果是来到这里,是因为SocketChannel不能把数据写到TCP send缓冲队列里面,
    *  于是使用 BlockPoller 来监听可写事件的发生,并触发CountDownLatch的
    *  countDown()方法,使阻塞于写的线程被唤醒。
    */
      if ( att.getWriteLatch()==null || att.getWriteLatch().getCount()==0)
        att.startWriteLatch(1);//开始一个倒数计数器
        //将socket注册到辅Selector，这里poller就是BlockSelector线程
        poller.add(att,SelectionKey.OP_WRITE,reference);
        //阻塞，直至超时时间唤醒，或者在还没有达到超时时间，在BlockSelector中唤醒
        att.awaitWriteLatch(writeTimeout,TimeUnit.MILLISECONDS);
      }
      catch (InterruptedException ignore) {
        Thread.interrupted();
      }

      if ( att.getWriteLatch()!=null && att.getWriteLatch().getCount()> 0) {
        // go into here means : we got interrupted, but we haven't 
        // received notification from the poller.
        keycount = 0;
      }
      else {
      // latch的countdown()方法被调用了
      // 还没超时就唤醒，说明网络状态恢复，继续下一次循环，完成写socket
      keycount = 1;
      att.resetWriteLatch();
      }

      if (writeTimeout > 0 && (keycount == 0))
        timedout = (System.currentTimeMillis() - time) >= writeTimeout;

      }// while

      if (timedout)
        throw new SocketTimeoutException();

    }
    finally {
      poller.remove(att,SelectionKey.OP_WRITE);
      if (timedout && reference.key!=null) {
        poller.cancelKey(reference.key);
    }
    reference.key = null;
    }
    return written;
}
```

## KeyAttachment的WriteLatch

#### startWriteLatch()方法

``` bash
public void startWriteLatch(int cnt) {
  writeLatch = startLatch(writeLatch,cnt);
}
```

继续看startLatch方法:
``` bash
protected CountDownLatch startLatch(CountDownLatch latch, int cnt) {
  if ( latch == null || latch.getCount() == 0 ) {
    return new CountDownLatch(cnt);
  }
  else throw new IllegalStateException("Latch must be at count 0 or null.");
}
```
可以看到，最后是创建一个CountDownLatch并保持在KeyAttachment里面。


#### awaitWriteLatch()方法
``` bash
public void awaitWriteLatch(long timeout, TimeUnit unit)
  throws InterruptedException { 
  awaitLatch(writeLatch,timeout,unit);
}
```
继续看startLatch方法:
``` bash
protected void awaitLatch(CountDownLatch latch, long timeout, TimeUnit unit)   throws InterruptedException {
  if ( latch == null ) 
    throw new IllegalStateException("Latch cannot be null");
  latch.await(timeout,unit);
}
```
可以看到，最后是调用CountDownLatch的await()方法来实现线程的阻塞。



## BlockPoller的处理
BlockPoller线程的run()方法如下(省略部分代码):

``` bash
public void run() {
  while (run) {
    try {
      events();
      ...
      selector.select();
      ...
      Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
      while (run && iterator != null && iterator.hasNext()) {
        SelectionKey sk = iterator.next();
        KeyAttachment attachment = (KeyAttachment)sk.attachment();
        try {
          attachment.access();
          iterator.remove();
          sk.interestOps(sk.interestOps() & (~sk.readyOps()));
          if ( sk.isReadable() ) {
            countDown(attachment.getReadLatch());
          }
          if (sk.isWritable()) {
            countDown(attachment.getWriteLatch());
          }
      }
      catch (CancelledKeyException ckx) {
        if (sk!=null) sk.cancel();
        countDown(attachment.getReadLatch());
        countDown(attachment.getWriteLatch());
      }
    }//while
  }
  catch ( Throwable t ) {
    log.error("",t);
  }
}
```
可以看到，这个BlockPoller线程的处理过程是很典型的Java Nio的用法。其中特别的是对于通道是读事件就绪、写事件就绪，就调用attachment的ReadLatch、WriteLatch的countDown方法，从而唤醒正在阻塞的写线程。