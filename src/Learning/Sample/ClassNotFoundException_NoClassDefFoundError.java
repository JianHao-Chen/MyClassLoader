package Learning.Sample;

import java.lang.reflect.Method;


/**
 *		 ～～～～～	【ClassNotFoundException】		～～～～～
 *	当程序尝试通过以下3种方法,用class的名字加载这个class:
 *		<1> Class.forName()
 *		<2> ClassLoader.findSystemClass()
 *		<3> ClassLoader.loadClass()
 *	但是找不到这个class的定义,于是这个异常被抛出。
 *	
 *
 */
public class ClassNotFoundException_NoClassDefFoundError {

	/*
	 * 在编译时,A类是存在的（从B类的角度看）。
	 * 但在运行时,读取B类的二进制字节码后,(我们在目录下去掉A.class文件)
	 * JVM还要去加载A类,但是A类找不到了,(这个A的定义是从B的字节码中读取到的)
	 * 就报NoClassDefFoundError
	 */
	public void testNoClassDefFoundError(){
		String classDataRootPath = "C:\\workspace helios\\MyClassLoader";
		FileSystemClassLoader fscl1 = new FileSystemClassLoader(classDataRootPath); 
	
		String className = "B";
	    
	    try {
	    	Class<?> class1 = fscl1.loadClass(className);
	    	ClassLoader loader1 = class1.getClassLoader();
	    	Object obj1 = class1.newInstance();
	
	    }
	    catch (Exception e) { 
	        e.printStackTrace(); 
	    }
	}
	
	public static void main(String[] args){
		ClassNotFoundException_NoClassDefFoundError c = new ClassNotFoundException_NoClassDefFoundError();
		c.testNoClassDefFoundError();
	}
}
