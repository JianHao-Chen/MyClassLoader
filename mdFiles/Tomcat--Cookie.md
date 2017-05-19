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


## 一个Cookie的例子
在用户自定义的Servlet中，我们可以通过以下代码向浏览器发送一个Cookie：
```bash
String cookieName = "phone";
String cookieValue = "12345";
Cookie cookie = new Cookie(cookieName, cookieValue);
response.addCookie(cookie);
```
其中，response是一个ResponseFacade对象，这个对象在用户的Servlet中使用。它的addCookie()方法会再后面再说。


## 相关的数据结构

#### Cookie
前面例子的Cookie，是Tomcat中`javax.servlet.http`包下面的类，为什么我特地提一下它的包名呢？因为Tomcat中还有一个类叫 Cookies(它在`org.apache.tomcat.util.http`包下)，名字好像哦，后面我会说说它们之间的不同。

这个Cookie有以下主要的属性：
```bash
private String name;
private String value;
private String comment;
private String domain;
private int maxAge = -1;
private String path;
private int version = 0;
```
还有一些get()，set()方法，就不列举了。


#### Cookies
这个Cookies(在`org.apache.tomcat.util.http`包下)，它的主要属性如下：
```bash
ServerCookie scookies[]=new ServerCookie[INITIAL_SIZE];
int cookieCount=0;
MimeHeaders headers;    //CoyoteRequest的headers的引用
```

再看ServerCookie，它有如下属性：
```bash
private MessageBytes name = MessageBytes.newInstance();
private MessageBytes value = MessageBytes.newInstance();
private MessageBytes path = MessageBytes.newInstance();
private MessageBytes domain = MessageBytes.newInstance();
private MessageBytes comment=MessageBytes.newInstance();
private int maxAge = -1;
private int version = 0;
...
```
ServerCookie可以说是Cookie的底层版本,它使用MessageBytes表示各个field,即引用着同一个byte[]数组。

说了这些数据结构，其实作用不大，还是看看Tomcat对Cookie的操作的具体过程吧。


## Cookie的添加

新建一个Cookie对象并添加到Response。就是前面的例子，我们只需从response的addCookie()方法开始说。此处使用的Response(ResponseFacade)，它的addCookie()方法最后会调用到Response(用于容器)的addCookieInternal()方法：
```bash
public void addCookieInternal(final Cookie cookie, final boolean httpOnly) {
  ...
  final StringBuffer sb = generateCookieString(cookie, httpOnly);
  addHeader("Set-Cookie", sb.toString());
  cookies.add(cookie);
}
```
可以看到，这个方法做了以下3件事：
1. 生成这个Cookie对应的字符串,例如 "phone=123456"
2. 将这个字符串设置到Response的header里面
3. 将cookie对象保存在cookies(这是一个ArrayList<Cookie>)

而addHeader()方法会调用coyoteResponse的addHeader()，此时在CoyoteResponse的Header多了一项： `"Set-Cookie = phone=123456"`。


## 浏览器得到Response
浏览器得到的response,其header有这么一项：
`Set-Cookie: phone=123456`

如果浏览器再次访问服务端,Request的Header有：
`Cookie: phone=123456`



## 对Cookie的处理

(1) 在分析InternalNioInputBuffer这个类的时候说过,它解析HTTP请求的Headers部分并且把它们置于coyoteRequest的headers里面。

(2) CoyoteAdapter在把request交给Container之前,先对请求做一些处理,其中就包括对cookie的处理：

1. 在CoyoteAdapter的postParseRequest()方法里面，会调用parseSessionCookiesId()方法
2. parseSessionCookiesId()方法从coyoteRequest中取得Cookies(先把它叫serverCookies)，然后调用Cookies的processCookies(MimeHeaders headers)方法。这个方法就是从headers中解析出Cookie，并保存到serverCookies里面。
3. headers是MimeHeaders类型的,其中保存着MimeHeaderField类型的数组,每一个MimeHeaderField就是request header的一个键值对。
4. 首先在headers 中找出键值为"Cookie"的MimeHeaderField,做一些检查,然后就把Cookie添加到serverCookies。“添加”的具体是：在serverCookies下的ServerCookie[]数组中添加一项,然后设置这个新的ServerCookie的名字(如"phone") 和值(如"123456")


## 对Cookie的获取
在自定义的Servlet中，获取Cookie的代码如下：
```bash
Cookie[] cookies = request.getCookies();
```
下面对request.getCookies()的执行过程进行分析：
1. RequestFacade的getCookies()，调用了Request的getCookies()方法
2. getCookies()方法调用parseCookies()方法，我认为这个函数做的就是从“serverCookies”到Cookie的转换。

这个parseCookies()方法：
```bash
Cookies serverCookies = coyoteRequest.getCookies();
int count = serverCookies.getCookieCount();
if (count <= 0)
  return;
  
cookies = new Cookie[count];
int idx=0;
for (int i = 0; i < count; i++) {
  ServerCookie scookie = serverCookies.getCookie(i);
  try {
    Cookie cookie = new Cookie(scookie.getName().toString(),null);
    int version = scookie.getVersion();
    cookie.setVersion(version);
    cookie.setValue(unescape(scookie.getValue().toString()));
    cookie.setPath(unescape(scookie.getPath().toString()));
    ...
    cookies[idx++] = cookie;
  }
  catch(IllegalArgumentException e) {
    // Ignore bad cookie
  }
  if( idx < count ) {
    Cookie [] ncookies = new Cookie[idx];
    System.arraycopy(cookies, 0, ncookies, 0, idx);
    cookies = ncookies;
  }
}
```