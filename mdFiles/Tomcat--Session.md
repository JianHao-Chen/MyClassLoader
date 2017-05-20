---
title: Tomcat的Session
categories: Tomcat源码
---

本文将介绍Tomcat对Session的处理的细节。

<!--more-->

---

## Session的介绍

Session实际上是在服务器中保存客户端信息的对象。。

#### 与Cookies的关系
Session是另一种记录客户状态的机制，不同的是Cookie保存在客户端浏览器中，而Session保存在服务器上。
如果说Cookie机制是通过检查客户身上的“通行证”来确定客户身份的话，那么Session机制就是通过检查服务器上的“客户明细表”来确认客户身份。Session相当于程序在服务器上建立的一份客户档案，客户来访的时候只需要查询客户档案表就可以了。


## Session的创建
通常，我们可以使用如下代码创建一个Session(当然，这个request没有session的前提下)：
```bash
HttpSession session = request.getSession(true);
```
上面request是一个RequestFacade对象，它的getSession()方法会调用Request的doGetSession()方法，我们来看这个方法：
```bash
protected Session doGetSession(boolean create) {
  // There cannot be a session if no context has been assigned yet
  if (context == null)
    return (null);

  // Return the current session if it exists and is valid
  // session不为null并且有效,返回
  if ((session != null) && !session.isValid())
    session = null;
  if (session != null)
    return (session);
    
  
  // 取出Manager，Manager是用于管理session的，它与StandardContext关联。
  // 这里默认的是StandardManager，还有：
  // 1. PersistentManager 用于提供持久化session的功能
  // 2. DeltaManager 用于tomcat群集时session的传输
  Manager manager = null;
  if (context != null)
    manager = context.getManager();
  if (manager == null)
    return (null);      // Sessions are not supported
    
  // 如果requestedSessionId不为空，表示之前已经为这个用户创建了session，先尝试找出session。
  if (requestedSessionId != null) {
    try {
      session = manager.findSession(requestedSessionId);
    }
    catch (IOException e) {
      session = null;
    }
    if ((session != null) && !session.isValid())
      session = null;
    if (session != null) {
      session.access();
      return (session);
    }
  }

  ...
  
  // 创建一个StandardSession对象
  session = manager.createSession(null);
  
  // Creating a new session cookie based on that session
  // 先在 StandardContext中取出 boolean flag "use cookies for session ids",用于指示是否用cookie来保存session ID.(默认是)
  
  if ((session != null) && (getContext() != null)
       && getContext().getCookies()) {
       
    String scName = context.getSessionCookieName();
    if (scName == null) //默认是“JSESSIONID”
      scName = Globals.SESSION_COOKIE_NAME;
    
    // 创建 Cookie("JSESSIONID",session.getIdInternal())
    Cookie cookie = new Cookie(scName, session.getIdInternal());
    // 设置相关属性,如(maxAge=-1、path=StandardContext.getPath())
    configureSessionCookie(cookie);
    // 将Cookie添加到Response的Header里面,添加了:
    //   " Set-Cookie = JSESSIONID=XXX; Path=/XXX"
    response.addSessionCookieInternal(cookie, context.getUseHttpOnly());
  }
  
  if (session != null) {
    session.access();
    return (session);
  }
  else
    return (null);
}
```


## Session的访问
当浏览器再次访问时,会在Request的请求头带上  "Cookie : JSESSIONID=XXX"。

然后，Tomcat会从Request的请求头找出 Cookie这一项进行处理，这些处理就在CoyoteAdapter的parseSessionCookiesId()方法：
```bash
protected void parseSessionCookiesId(org.apache.coyote.Request req, Request request) {
  ...
  Cookies serverCookies = req.getCookies();
  ...
  // 默认下，sessionCookieName = JSESSIONID
  String sessionCookieName = getSessionCookieName(context);
  for (int i = 0; i < count; i++) {
    ServerCookie scookie = serverCookies.getCookie(i);
    if (scookie.getName().equals(sessionCookieName)) {
      ...
      request.setRequestedSessionId(scookie.getValue().toString());
      request.setRequestedSessionCookie(true);
      request.setRequestedSessionURL(false);
    }
    ...
  }
}
```

可以看到，RequestedSessionId是Request类下的一个属性，用于保存用户访问已存在的session的ID。
而Tomcat是通过遍历serverCookies(是Cookies类型的对象，它是“底层”版本的Cookie，是Tomcat解析完Request的Header得到的)，找出name为 JSESSIONID 的cookie，并将这个cookie的值(即session的ID)赋值给RequestedSessionId。


## 对于浏览器不支持Cookie的情况

可以通过 URL重写的方法，例如，使用如下代码：
```bash
response.encodeURL("XXX")
```

例如，我是在自定义的Servlet(名字是SessionExample)使用 `response.encodeURL("SessionExample")`。
得到 “SessionExample;jsessionid=FA89F727B215C2285DE8C9B085841E9C”，然后，浏览器得到的URL会是： http://.../SessionExample;jsessionid=FA89F727B215C2285DE8C9B085841E9C


接着，用户再次访问，Tomcat的CoyoteAdapter在postParseRequest()方法中调用parsePathParameters()解析 PathParameters时会把 “ jsessionid=FA89F727B215C2285DE8C9B085841E9C” 作为参数添加到request的 pathParameters(这是一个HashMap)。

CoyoteAdapter的parsePathParameters()方法：
```bash
protected void parsePathParameters(org.apache.coyote.Request req,Request request) {
  
  // 这里得到： “/.../SessionExample;jsessionid=FA89F727B215C2285DE8C9B085841E9C”
  ByteChunk uriBC = req.decodedURI().getByteChunk();
  int semicolon = uriBC.indexOf(';', 0);
  
  ...
  while (semicolon > -1) {
    // Parse path param, and extract it from the decoded request URI
    int start = uriBC.getStart();
    int end = uriBC.getEnd();
    
    int pathParamStart = semicolon + 1;
    int pathParamEnd = ByteChunk.findBytes(uriBC.getBuffer(),
                       start + pathParamStart, end,
                       new byte[] {';', '/'});
    
    String pv = null;
    
    ...
    
    try {
      // 这里，得到的pv = jsessionid=FA89F727B215C2285DE8C9B085841E9C
      pv = (new String(uriBC.getBuffer(), start + pathParamStart, 
              (end - start) - pathParamStart, enc));
    }
    catch (UnsupportedEncodingException e) {...}
      // 更新request的uri为 /.../SessionExample
      uriBC.setEnd(start + semicolon);
    }
    
    if (pv != null) {
      int equals = pv.indexOf('=');
      if (equals > -1) {
        String name = pv.substring(0, equals);     // name = jsessionid
        String value = pv.substring(equals + 1);   // value = FA89F727B215C2285DE8C9B085841E9C
        request.addPathParameter(name, value);
      }
    }
   
    semicolon = uriBC.indexOf(';', semicolon);
}
```

然后，同样在postParseRequest()方法中，会获取保存在request的PathParameter里面的sessionID，再把sessionID设置到Request的RequestedSessionId属性。