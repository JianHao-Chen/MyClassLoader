package Learning.Sample;


/**
 *	用来加载存储在文件系统上的 Java 字节代码 
 */
public class FileSystemClassLoader extends ClassLoader{

	private String rootDir;
	public FileSystemClassLoader(String rootDir) { 
        this.rootDir = rootDir; 
    } 
	
	
}
