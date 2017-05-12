---
title: Java自定义类加载器
categories: JDK源码
---

本文展示一个自定义类加载器的例子(例子来源于IBM developerworks上的文章)。

<!--more-->

---

```bash
public class FileSystemClassLoader extends ClassLoader{

  private String rootDir;
	
  public FileSystemClassLoader(String rootDir) { 
    this.rootDir = rootDir; 
  } 
	
  public Class<?> loadClass(String name)throws ClassNotFoundException{
    System.out.println("Current Loader :"+this);
		
    Class c = findLoadedClass(name);
    if (c == null) {
      try {
        if (this.getParent() != null) {
          c = this.getParent().loadClass(name);
        }
      }
      catch (ClassNotFoundException e) {
        c = findClass(name);
      }
    }
    return c;
  }
	
  protected Class<?> findClass(String name)throws ClassNotFoundException{
    byte[] classData = getClassData(name);
    if (classData == null) { 
      throw new ClassNotFoundException(); 
    } 
    else { 
      return defineClass(name, classData, 0, classData.length); 
    } 
  }
	
  private byte[] getClassData(String className) { 
    String path = classNameToPath(className);
    try{
      InputStream ins = new FileInputStream(path);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      int bufferSize = 4096; 
      byte[] buffer = new byte[bufferSize]; 
      int bytesNumRead = 0; 
      while ((bytesNumRead = ins.read(buffer)) != -1) { 
        baos.write(buffer, 0, bytesNumRead); 
      }
      return baos.toByteArray();
    }
    catch (IOException e) { 
      e.printStackTrace(); 
    } 
    return null;
  }

  private String classNameToPath(String className) { 
    return rootDir + File.separatorChar 
      + className.replace('.', File.separatorChar) + ".class"; 
  }

}
```