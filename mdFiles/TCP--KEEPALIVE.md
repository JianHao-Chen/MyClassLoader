---
title: TCP的TCP_KEEPALIVE选项
category: TCP/IP
---

本文将记录TCP的TCP_KEEPALIVE选项

<!--more-->

---

## KEEPALIVE机制
是TCP协议规定的TCP层（非应用层业务代码实现的）检测TCP本端到对方主机的TCP连接的连通性的行为。避免服务器在客户端出现各种不良状况时无法感知，而永远等在这条TCP连接上。

如下代码是设定该选项的检测行为的细节：
```bash
int keepAlive = 1;    // 非0值，开启keepalive属性

int keepIdle = 60;    // 如该连接在60秒内没有任何数据往来,则进行此TCP层的探测

int keepInterval = 5; // 探测发包间隔为5秒

int keepCount = 3;        // 尝试探测的次数.如果第1次探测包就收到响应了,则后2次的不再发

setsockopt(sockfd, SOL_SOCKET, SO_KEEPALIVE, (void *)&keepAlive, sizeof(keepAlive));
setsockopt(sockfd, SOL_TCP, TCP_KEEPIDLE, (void*)&keepIdle, sizeof(keepIdle));
setsockopt(sockfd, SOL_TCP, TCP_KEEPINTVL, (void *)&keepInterval, sizeof(keepInterval));
setsockopt(sockfd, SOL_TCP, TCP_KEEPCNT, (void *)&keepCount, sizeof(keepCount)); 
```
设置该选项后，如果60秒内在此套接口所对应连接的任一方向都没有数据交换，TCP层就自动给对方发一个保活探测分节(keepalive probe)。这是一个对方必须响应的TCP分节。它会导致以下三种情况：

1. 对方接收一切正常：以期望的ACK响应。60秒后，TCP将重新开始下一轮探测。
2. 对方已崩溃且已重新启动：以RST响应。套接口的待处理错误被置为ECONNRESET。
3. 对方无任何响应：比如客户端那边已经断网，或者客户端直接死机。以设定的时间间隔尝试3次，无响应就放弃。套接口的待处理错误被置为ETIMEOUT。 


## 与应用层相关
1. 阻塞模型
当TCP层检测到对端socket不再可用时，内核无法主动通知应用层出错，只有应用层主动调用read()或者write()这样的IO系统调用时，内核才会利用出错来通知应用层。 
2. 阻塞模型
select或者epoll会返回sockfd可读,应用层对其进行读取时，read()会报错。

我们在做服务器程序的时候，对客户端的保活探测基本上不依赖于这个TCP层的keepalive探测机制。
而是我们自己做一套应用层的请求应答消息，在应用层实现这样一个功能。 