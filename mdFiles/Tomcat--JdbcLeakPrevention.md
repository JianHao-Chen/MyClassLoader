---
title: Tomcat是如何避免JDBC内存泄漏的
categories: Tomcat源码
---

本文将介绍Tomcat是如何避免JDBC内存泄漏的。

<!--more-->

---

## Tomcat中JDBC内存泄漏是如何发生的

请看如下例子：
```bash
public class JdbcLeakPreventionExample extends HttpServlet {

  public void doGet(HttpServletRequest request,HttpServletResponse response)
  throws IOException, ServletException{
    try {
      Class.forName("com.mysql.jdbc.Driver");
      // access MySQL
    }
    catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    catch(SQLException e){
      e.printStackTrace();
    }
  }
}
```
如上所示，这是一个用户的Servlet，这个Servlet加载MySQL数据库驱动并且访问数据库，然后结束。看起来没有问题。

#### 加载MySQL数据库驱动
我们都知道使用以下语句可以加载MySQL数据库驱动：
```bash
Class.forName("com.mysql.jdbc.Driver");
```
Class.forName()将对应的驱动类加载到内存中，然后执行内存中的static静态代码段，代码段中，会创建一个驱动Driver的实例，放入DriverManager中，供DriverManager使用。

【补充】
DriverManager的作用：

* 注册和删除加载的驱动程序
* 根据给定的url获取符合url协议的驱动Driver
* 建立Conenction连接，进行数据库交互

#### 问题分析
由于用户的Servlet没有在退出时从DriverManager清理MySQL的JDBC驱动，所以加载这个驱动类的类加载器会逃过垃圾回收，产生内存泄漏。


#### 解决方法
解决方法的重点自然是清除掉不用的JDBC驱动。

Tomcat提供了一个类`JdbcLeakPrevention`，它只有一个方法，它的关键代码是：
```bash
Enumeration<Driver> drivers = DriverManager.getDrivers();
while (drivers.hasMoreElements()) {
  Driver driver = drivers.nextElement();
  if (driver.getClass().getClassLoader() != this.getClass().getClassLoader()) 
    continue;
  DriverManager.deregisterDriver(driver);
}
```
这里，关键部分是对加载driver的ClassLoader的判断。这里的策略是当一个driver的ClassLoader与加载JdbcLeakPrevention类的ClassLoader相同，那么就注销这个driver。

很容易想到，我们必须使加载JdbcLeakPrevention类的ClassLoader就是当前准备退出的WebappClassLoader。

Tomcat在WebappClassLoader的stop()方法中，会调用clearReferences()方法，这个方法会调用clearReferencesJdbc()方法。我们来看这个方法的关键代码：
```bash
private final void clearReferencesJdbc() {
  // 这里getResourceAsStream()方法是WebappClassLoader里面自定义的方法，它的逻辑与loadClass()方法相似:
  // (1) 在自己(WebappClassLoader)中的缓存(resourceEntries)找。
  // (2) 在自己的仓库(WEB-INF/class)中找
  // (3) 委托给父类加载器委托给父类加载器
  InputStream is = getResourceAsStream("org/apache/catalina/loader/JdbcLeakPrevention.class");
  
  // 读取class文件，省略了try catch
  byte[] classBytes = new byte[2048];
  int offset = 0;
  int read = is.read(classBytes, offset, classBytes.length-offset);
  
  // Define Class
  // 使得到的obj是由WebappClassLoader加载的。
  Class<?> lpClass =
    defineClass("org.apache.catalina.loader.JdbcLeakPrevention",
    classBytes, 0, offset, this.getClass().getProtectionDomain());
   Object obj = lpClass.newInstance();
  
  // 通过反射调用clearJdbcDriverRegistrations()
  List<String> driverNames = (List<String>) obj.getClass().
      getMethod("clearJdbcDriverRegistrations").invoke(obj);
}
```
【注意】
不能直接生成一个JdbcLeakPrevention的实例,因为这样会导致：
`JdbcLeakPrevention的加载会由加载当前WebappClassLoader的类加载器加载,我在自己的机器debug看了，是 sun.misc.Launcher$AppClassLoader。`
这样得到的JdbcLeakPrevention,我们调用它的clearJdbcDriverRegistrations()是不会得到预期效果的。


