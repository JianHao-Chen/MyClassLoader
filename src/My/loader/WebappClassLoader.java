package My.loader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

import javax.naming.directory.DirContext;

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
    	
    }
    
}
