---
title: Tomcat对Cookie的处理
categories: Tomcat源码
---

本文将介绍Tomcat对Cookie的处理的细节。

<!--more-->

---

## Cookie的介绍

Cookie实际上是一小段的文本信息。

#### 使用Cookie的原因
HTTP协议是无状态的协议,服务器无法从连接上跟踪会话。客户端请求服务器，如果服务器需要记录该用户状态，就使用response向客户端浏览器颁发一个Cookie。客户端浏览器会把Cookie保存起来。当浏览器再请求该网站时，浏览器把请求的网址连同该Cookie一同提交给服务器。服务器检查该Cookie，以此来辨认用户状态。

【注意】
Cookie功能需要浏览器的支持。如果浏览器不支持Cookie或者把Cookie禁用了，Cookie功能就会失效。

## Cookie的数据结构

