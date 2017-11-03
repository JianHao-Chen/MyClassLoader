package Learning.Sample;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 *	用来加载存储在文件系统上的 Java 字节代码 
 */
public class FileSystemClassLoader extends ClassLoader{

	private String rootDir;
	
	public FileSystemClassLoader(String rootDir) { 
        this.rootDir = rootDir; 
    } 
	
	public Class<?> loadClass(String name)throws ClassNotFoundException{
		System.out.println("Current Loader is:"+this+" ,it is loading "+name);
		
		Class c = findLoadedClass(name);
		if (c == null) {
			try {
				if (this.getParent() != null) {
				    c = this.getParent().loadClass(name);
				}
			}
			catch (ClassNotFoundException e) {
			    System.out.println("Class "+name+" no found by "+this.getParent());
			}
			
			
			    c = findClass(name);
			
			
			
		}
		
		return c;
	}
	
	protected Class<?> findClass(String name)throws ClassNotFoundException{
		byte[] classData = getClassData(name);
		if (classData == null) { 
            throw new ClassNotFoundException(); 
        } 
        else { 
            Class<?> clazz = null;
            try{
                clazz = defineClass(name, classData, 0, classData.length); 
            }
            catch(Exception e){
                System.out.println("DefineClass error "+name);
            }
            return clazz;
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
