package Learning.Sample;

import java.lang.reflect.Method;

public class testSameClass {

	public void testClassIdentity(){
		String classDataRootPath = "C:\\workspace helios\\MyClassLoader";
		FileSystemClassLoader fscl1 = new FileSystemClassLoader(classDataRootPath); 
	    FileSystemClassLoader fscl2 = new FileSystemClassLoader(classDataRootPath); 
	    
	    String className = "HelloWorld";
	    
	    try {
	    	/*ClassLoader parentLoader = fscl1.getParent();
	    	Class<?> class0 = parentLoader.loadClass(className);
	    	Object obj0 = class0.newInstance();*/
	    	
	    	Class<?> class1 = fscl1.loadClass(className);
	    	ClassLoader loader1 = class1.getClassLoader();
	    	Object obj1 = class1.newInstance(); 
	        Class<?> class2 = fscl2.loadClass(className); 
	        ClassLoader loader2 = class2.getClassLoader();
	        Object obj2 = class2.newInstance(); 
	        
	        Class<?> class3 = fscl1.loadClass(className);
	        ClassLoader loader3 = class1.getClassLoader();
	        
	        Method setSampleMethod = class1.getMethod("setSample", java.lang.Object.class); 
	        setSampleMethod.invoke(obj1, obj2);
	    }
	    catch (Exception e) { 
	        e.printStackTrace(); 
	    } 
	    
	}
	
	public static void main(String[] args){
		testSameClass t = new testSameClass();
		t.testClassIdentity();
	}
}
