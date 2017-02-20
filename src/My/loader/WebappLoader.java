package My.loader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Scanner;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import My.resources.BaseDirContext;
import My.resources.FileDirContext;
import My.resources.ProxyDirContext;

public class WebappLoader implements Loader{
	
	
	private static final String AbsoluteClassPath = 
		"C:\\workspace helios\\MyClassLoader\\MyResource\\classes";

	// ------------------- Constructors ------------------- 
	/**
     * Construct a new WebappLoader with no defined parent class loader
     * (so that the actual parent will be the system class loader).
     */
    public WebappLoader() {

        this(null);

    }
    
    /**
     * Construct a new WebappLoader with the specified class loader
     * to be defined as the parent of the ClassLoader we ultimately create.
     *
     * @param parent The parent class loader
     */
    public WebappLoader(ClassLoader parent) {
        super();
        this.parentClassLoader = parent;
    }
    
    
	// --------------- Instance Variables --------------- 
    
    /**
     * The class loader being managed by this Loader component.
     */
    private WebappClassLoader classLoader = null;
    
    private String loaderClass =
        "My.loader.WebappClassLoader";
    
    /**
     * The parent class loader of the class loader we will create.
     */
    private ClassLoader parentClassLoader = null;
    
    /**
     * The set of repositories associated with this class loader.
     */
    private String repositories[] = new String[0];
    
    
    private ProxyDirContext resources;
    
    
	// ------------------ Properties ------------------
    /**
     * Return the Java class loader to be used by this Container.
     */
    public ClassLoader getClassLoader() {

        return ((ClassLoader) classLoader);

    }
    
    
    
    public void start(){

    	DirContext resource = getProxyResources();
    	
    	// Construct a class loader based on our current repositories list
    	try {
    		classLoader = createClassLoader();
    		
    		classLoader.setResources(resource);
    		
    		classLoader.setDelegate(false);
    		
    		// Configure our repositories
            setRepositories(resource);

            
    	}
    	catch (Throwable t) {
    		
    	}
    }
    
    public void stop(){
    	classLoader.stop();
    }
    
    
    public void loopForCommandLine(){
    	Scanner s = new Scanner(System.in);
    	while(true){
    		System.out.println("input the class name you want to load:");
    		String line = s.nextLine(); 
    		if (line.equals("exit")) 
    			break;
    		else if(line.equals("reload")){
    			stop();
    			start();
    		}
    			
    		
    		handleRequest(line);
    	}
    }
    
    public void handleRequest(String line){
    	line = "HelloWorld.class";
    	Class<?> clazz = null;
    	try {
    		 
    		clazz = getClassLoader().loadClass(line);
			
		} catch (ClassNotFoundException e) {
			System.out.println("Class "+line+" Not Found");
		}
		
		try{
			// run main function of the class we load
			Method method = 
				clazz.getDeclaredMethod("main", String[].class);
			String[] args = new String[2];
			args[0] = "123";
			args[1] = "456";
			method.invoke(null,(Object)new String[]{"abc","efg"});
		}
		catch(NoSuchMethodException e){
			System.out.println("No Such Method ");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
    public synchronized DirContext setResources(DirContext resources) {
    	
    	if (resources instanceof BaseDirContext) {
    		((BaseDirContext) resources).setCached(true);
            ((BaseDirContext) resources).setCacheTTL(5000);
            ((BaseDirContext) resources).setCacheMaxSize(10240);
            ((BaseDirContext) resources).setCacheObjectMaxSize(512);
    	}
    	
    	if (resources instanceof FileDirContext) {
    		//((FileDirContext) resources).setCaseSensitive(isCaseSensitive());
            //((FileDirContext) resources).setAllowLinking(isAllowLinking());
    	}
    	
    	return resources;
    }
    
    
    // -------------------- Private Methods --------------------
    
    private ProxyDirContext getProxyResources(){
    	Hashtable env = new Hashtable();
    	DirContext dirContext = new FileDirContext();
    	
    	dirContext = setResources(dirContext);
    	
    	ProxyDirContext proxyDirContext = null;
    	try {
    		proxyDirContext =
                new ProxyDirContext(env, dirContext);
    		
    		
    	}
    	catch (Throwable t) {
    		
    	}
    	
    	return proxyDirContext;
    }
    
    private WebappClassLoader createClassLoader()
    	throws Exception {
    	
    	Class clazz = Class.forName(loaderClass);
        WebappClassLoader classLoader = null;
        
        Class[] argTypes = { ClassLoader.class };
        Object[] args = { parentClassLoader };
        Constructor constr = clazz.getConstructor(argTypes);
        classLoader = (WebappClassLoader) constr.newInstance(args);
        
        return classLoader;
    }
    
    
    /**
     * Configure the repositories for our class loader, based on the
     * associated Context.
     * @throws IOException 
     */
    private void setRepositories(DirContext resources) throws IOException {
    	
    	
    	// Loading the work directory
    	// TODO
    	
    	
    	// Setting up the class repository (/MyRepository), if it exists
    	String classesPath = "/MyResource/classes";
    	DirContext classes = null;
    	
    	try {
    		Object object = resources.lookup(classesPath);
    		if (object instanceof DirContext) {
    			classes = (DirContext) object;
    		}
    	}
    	catch(NamingException e) {
    		// Silent catch: it's valid that no /MyRepository collection
            // exists
    	}
    	
    	
    	if (classes != null) {
    		File classRepository = null;
    		String absoluteClassesPath = AbsoluteClassPath;
    		if (absoluteClassesPath != null) {
    			classRepository = new File(absoluteClassesPath);
    		}
    		
    		
    		// Adding the repository to the class loader
    		classLoader.addRepository(classesPath + "/", classRepository);
    	}
    }
}
