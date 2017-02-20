package My.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.HashMap;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import My.resources.Resource;
import My.resources.ResourceAttributes;

public class WebappClassLoader extends URLClassLoader{

	
	 // ---------------- Constructors ----------------
	
	/**
     * Construct a new ClassLoader with no defined repositories and no
     * parent ClassLoader.
     */
    public WebappClassLoader() {
        super(new URL[0]);
        this.parent = getParent();
        system = getSystemClassLoader();
    }
    
    /**
     * Construct a new ClassLoader with no defined repositories and the given
     * parent ClassLoader.
     *
     * @param parent Our parent class loader
     */
    public WebappClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        this.parent = getParent();
        system = getSystemClassLoader();
    }
    
    
	// ----------------- Instance Variables ----------------------
    
    /**
     * Associated directory context giving access to the resources in this
     * webapp.
     */
    protected DirContext resources = null;
    
    /**
     * The cache of ResourceEntry for classes and resources we have loaded,
     * keyed by resource name.
     */
    protected HashMap resourceEntries = new HashMap();
    
    
    /**
     * Should this class loader delegate to the parent class loader
     * <strong>before</strong> searching its own repositories (i.e. the
     * usual Java2 delegation model)?  If set to <code>false</code>,
     * this class loader will search its own repositories first, and
     * delegate to the parent only if the class or resource is not
     * found locally.
     */
    protected boolean delegate = false;
    
    
    /**
     * The parent class loader.
     */
    protected ClassLoader parent = null;

    
    /**
     * The class loader being managed by this Loader component.
     */
    private WebappClassLoader classLoader = null;

    /**
     * The system class loader.
     */
    protected ClassLoader system = null;
    
    /**
     * The parent class loader of the class loader we will create.
     */
    private ClassLoader parentClassLoader = null;
    
    
    private String loaderClass =
        "My.loader.WebappClassLoader";
    
    /**
     * The list of local repositories, in the order they should be searched
     * for locally loaded classes or resources.
     */
    protected String[] repositories = new String[0];
    
    /**
     * Repositories translated as path in the work directory (for Jasper
     * originally), but which is used to generate fake URLs should getURLs be
     * called.
     */
    protected File[] files = new File[0];
    
	// ----------------------- Properties ----------------------
    
    
    
    
    
    
    private boolean initialized=false;
    public void init() {
    	initialized=true;
    }
    
    
    /**
     * Start the class loader.
     *
     * @exception LifecycleException if a lifecycle error occurs
     */
    public void start(){
    	
    }
    
    
    public void stop(){
    	
    	int length = files.length;
        for (int i = 0; i < length; i++) {
            files[i] = null;
        }
        
        resourceEntries.clear();
        resources = null;
        repositories = null;
        files = null;
        parent = null;
    }
    
    
    
    /**
     * Get associated resources.
     */
    public DirContext getResources() {
        return this.resources;
    }
    /**
     * Set associated resources.
     */
    public void setResources(DirContext resources) {
        this.resources = resources;
    }
    
    
    /**
     * Return the "delegate first" flag for this class loader.
     */
    public boolean getDelegate() {
        return (this.delegate);
    }

    /**
     * Set the "delegate first" flag for this class loader.
     *
     * @param delegate The new "delegate first" flag
     */
    public void setDelegate(boolean delegate) {
        this.delegate = delegate;
    }
    
    
    /**
     * Add a new repository to the set of places this ClassLoader can look for
     * classes to be loaded.
     *
     * @param repository Name of a source of classes to be loaded, such as a
     *  directory pathname, a JAR file pathname, or a ZIP file pathname
     *
     * @exception IllegalArgumentException if the specified repository is
     *  invalid or does not exist
     */
    synchronized void addRepository(String repository, File file) {
    	if (repository == null)
            return;
    	
    	int i;
    	// Add this repository to our internal list
        String[] result = new String[repositories.length + 1];
        for (i = 0; i < repositories.length; i++) {
            result[i] = repositories[i];
        }
        
        result[repositories.length] = repository;
        repositories = result;
        
        // Add the file to the list
        File[] result2 = new File[files.length + 1];
        for (i = 0; i < files.length; i++) {
            result2[i] = files[i];
        }
        result2[files.length] = file;
        files = result2;
    }
    
    
    /**
     * Load the class with the specified name.  This method searches for
     * classes in the same manner as <code>loadClass(String, boolean)</code>
     * with <code>false</code> as the second argument.
     *
     * @param name Name of the class to be loaded
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public Class loadClass(String name) throws ClassNotFoundException {
    	
    	return (loadClass(name, false));
    }
    
    
    /**
     * Load the class with the specified name, searching using the following
     * algorithm until it finds and returns the class.  If the class cannot
     * be found, returns <code>ClassNotFoundException</code>.
     * <ul>
     * <li>Call <code>findLoadedClass(String)</code> to check if the
     *     class has already been loaded.  If it has, the same
     *     <code>Class</code> object is returned.</li>
     * <li>If the <code>delegate</code> property is set to <code>true</code>,
     *     call the <code>loadClass()</code> method of the parent class
     *     loader, if any.</li>
     * <li>Call <code>findClass()</code> to find this class in our locally
     *     defined repositories.</li>
     * <li>Call the <code>loadClass()</code> method of our parent
     *     class loader, if any.</li>
     * </ul>
     * If the class was found using the above steps, and the
     * <code>resolve</code> flag is <code>true</code>, this method will then
     * call <code>resolveClass(Class)</code> on the resulting Class object.
     *
     * @param name Name of the class to be loaded
     * @param resolve If <code>true</code> then resolve the class
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public synchronized Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
    	
    	Class clazz = null;
    	
    	// Check our previously loaded local class cache
    	clazz = findLoadedClass0(name);
    	if (clazz != null) {
    		if (resolve)
                resolveClass(clazz);
            return (clazz);
    	}
    	
    	// Check our previously loaded class cache (recorded by JVM)
    	// the check is only for this class loader(here means WebappClassLoader).
        clazz = findLoadedClass(name);
        if (clazz != null){
        	if (resolve)
                resolveClass(clazz);
            return (clazz);
        }
    	
        
        // Try loading the class with the system class loader
        try {
        	clazz = system.loadClass(name);
        	if (clazz != null) {
        		if (resolve)
                    resolveClass(clazz);
        		return (clazz);
        	}
        }catch (ClassNotFoundException e) {
        }
        
        
        // Search local repositories
        try{
        	clazz = findClass(name);
        	if (clazz != null) {
        		if (resolve)
                    resolveClass(clazz);
        		return (clazz);
        	}
        }
        catch (ClassNotFoundException e) {
            ;
        }
        
        throw new ClassNotFoundException(name);
    }
    
    
    /**
     * Finds the class with the given name if it has previously been
     * loaded and cached by this class loader, and return the Class object.
     * If this class has not been cached, return <code>null</code>.
     *
     * @param name Name of the resource to return
     */
    protected Class findLoadedClass0(String name) {
    	
    	ResourceEntry entry = (ResourceEntry) resourceEntries.get(name);
    	if (entry != null) {
            return entry.loadedClass;
        }
        return (null);
    }
    
    
    /**
     * Find the specified class in our local repositories, if possible.  If
     * not found, throw <code>ClassNotFoundException</code>.
     *
     * @param name Name of the class to be loaded
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public Class findClass(String name) throws ClassNotFoundException {
    	
    	// Ask our superclass to locate this class, if possible
        // (throws ClassNotFoundException if it is not found)
        Class clazz = null;
        try {
        	clazz = findClassInternal(name);
        }
        catch(ClassNotFoundException cnfe) {
        	
        }
        
        return (clazz);
    }
    
    /**
     * Find specified class in local repositories.
     *
     * @return the loaded class, or null if the class isn't found
     */
    protected Class findClassInternal(String name)
        throws ClassNotFoundException {
    	
    	ResourceEntry entry = null;
    	
    	entry = findResourceInternal(name, name);
    	
    	if (entry == null)
            throw new ClassNotFoundException(name);
    	
    	Class clazz = entry.loadedClass;
    	if (clazz != null)
            return clazz;
    	
    	synchronized (this) {
    		clazz = entry.loadedClass;
            if (clazz != null)
                return clazz;
            
            if (entry.binaryContent == null)
                throw new ClassNotFoundException(name);
            
            
            try {
            	clazz = defineClass("HelloWorld", entry.binaryContent, 0,
                        entry.binaryContent.length,
                        new CodeSource(entry.codeBase, new Certificate[0]));
            }
            catch (UnsupportedClassVersionError ucve) {
            	
            }
            
            entry.loadedClass = clazz;
            entry.binaryContent = null;
            entry.source = null;
            entry.codeBase = null;
    	}
    	
    	return clazz;
    }
    
    /**
     * Find specified resource in local repositories.
     *
     * @return the loaded resource, or null if the resource isn't found
     */
    protected ResourceEntry findResourceInternal(String name, String path) {
    	if ((name == null) || (path == null))
            return null;
    	
    	ResourceEntry entry = (ResourceEntry) resourceEntries.get(name);
    	if (entry != null)
            return entry;
    	
    	int contentLength = -1;
        InputStream binaryStream = null;
        
        int repositoriesLength = repositories.length;
        
        int i;

        Resource resource = null;
        
        for (i = 0; (entry == null) && (i < repositoriesLength); i++) {
        	try {
        		String fullPath = repositories[i] + path;
        		Object lookupResult = resources.lookup(fullPath);
        		if (lookupResult instanceof Resource) {
                    resource = (Resource) lookupResult;
                }
        		
        		// Note : Not getting an exception here means the resource was
                // found
        		entry = findResourceInternal(files[i], path);
        		
        		ResourceAttributes attributes =
                    (ResourceAttributes) resources.getAttributes(fullPath);
                contentLength = (int) attributes.getContentLength();
                entry.lastModified = attributes.getLastModified();
                
                if (resource != null) {
                	try {
                		binaryStream = resource.streamContent();
                		
                	}
                	catch (IOException e) {
                        return null;
                    }
                }
        	}
        	catch (NamingException e) {
            }
        	
        	
        	if (binaryStream != null) {
    			byte[] binaryContent = new byte[contentLength];
    			int pos = 0;
                try {
                	while (true) {
                		int n = binaryStream.read(binaryContent, pos,
                                binaryContent.length - pos);
                		if (n <= 0)
                			break;
                		pos += n;
                	}
                }catch (IOException e) {
                	System.out.println("read file content error");
                	return null;
                }
                finally {
                	if (binaryStream != null) {
                        try {
                            binaryStream.close();
                        } catch (IOException e) { /* Ignore */}
                    }
                }
                
                entry.binaryContent = binaryContent;
    		}
        }
        
        
        // Add the entry in the local resource repository
        synchronized (resourceEntries) {
        	ResourceEntry entry2 = (ResourceEntry) resourceEntries.get(name);
            if (entry2 == null) {
                resourceEntries.put(name, entry);
            } else {
                entry = entry2;
            }
        }
        
        return entry;
    }
    
    /**
     * Find specified resource in local repositories.
     *
     * @return the loaded resource, or null if the resource isn't found
     */
    protected ResourceEntry findResourceInternal(File file, String path){
    	ResourceEntry entry = new ResourceEntry();
    	try {
            entry.source = getURI(new File(file, path));
            entry.codeBase = getURL(new File(file, path), false);
        } catch (MalformedURLException e) {
            return null;
        }   
        return entry;
    }
    
    /**
     * Get URL.
     */
    protected URL getURI(File file)
        throws MalformedURLException {


        File realFile = file;
        try {
            realFile = realFile.getCanonicalFile();
        } catch (IOException e) {
            // Ignore
        }
        return realFile.toURI().toURL();

    }
    
    /**
     * Get URL.
     */
    protected URL getURL(File file, boolean encoded)
        throws MalformedURLException {

        File realFile = file;
        try {
            realFile = realFile.getCanonicalFile();
        } catch (IOException e) {
            // Ignore
        }
        if(encoded) {
            return getURI(realFile);
        } else {
            return realFile.toURL();
        }

    }
}
