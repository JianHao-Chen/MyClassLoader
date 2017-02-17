package My.resources;

import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

public abstract class BaseDirContext implements DirContext {
	
	// -------------------- Constructors --------------------

    /**
     * Builds a base directory context.
     */
    public BaseDirContext() {
        this.env = new Hashtable();
    }


    /**
     * Builds a base directory context using the given environment.
     */
    public BaseDirContext(Hashtable env) {
        this.env = env;
    }
    
    
	// ------------------ Instance Variables ------------------ 
    
    /**
     * Environment.
     */
    protected Hashtable env;
    
    /**
     * Cached.
     */
    protected boolean cached = true;


    /**
     * Cache TTL.
     */
    protected int cacheTTL = 5000; // 5s


    /**
     * Max size of cache for resources.
     */
    protected int cacheMaxSize = 10240; // 10 MB


    /**
     * Max size of resources that will be content cached.
     */
    protected int cacheObjectMaxSize = 512; // 512 K
    
    
	// -------------------- Properties --------------------
    
    /**
     * Set cached.
     */
    public void setCached(boolean cached) {
        this.cached = cached;
    }


    /**
     * Is cached ?
     */
    public boolean isCached() {
        return cached;
    }
    
    
    /**
     * Set cache TTL.
     */
    public void setCacheTTL(int cacheTTL) {
        this.cacheTTL = cacheTTL;
    }


    /**
     * Get cache TTL.
     */
    public int getCacheTTL() {
        return cacheTTL;
    }


    /**
     * Return the maximum size of the cache in KB.
     */
    public int getCacheMaxSize() {
        return cacheMaxSize;
    }


    /**
     * Set the maximum size of the cache in KB.
     */
    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }


    /**
     * Return the maximum size of objects to be cached in KB.
     */
    public int getCacheObjectMaxSize() {
        return cacheObjectMaxSize;
    }


    /**
     * Set the maximum size of objects to be placed the cache in KB.
     */
    public void setCacheObjectMaxSize(int cacheObjectMaxSize) {
        this.cacheObjectMaxSize = cacheObjectMaxSize;
    }
    
    
    /**
     * Retrieves all of the attributes associated with a named object.
     * 
     * @return the set of attributes associated with name
     * @param name the name of the object from which to retrieve attributes
     * @exception NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(String name)
        throws NamingException {
    	return getAttributes(name, null);
    }
}
