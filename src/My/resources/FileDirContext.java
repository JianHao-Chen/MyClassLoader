package My.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class FileDirContext extends BaseDirContext{

	
	// ------------------------ Constructors ------------------------
	public FileDirContext() {
		setDocBase(absoluteBase);
	}
	
	public FileDirContext(Hashtable env) {
        super(env); 
    }
	
	
	// -------------------- Instance Variables --------------------
	
	/**
     * The document base directory.
     */
    protected File base = null;
    
    
    /**
     * Absolute normalized filename of the base.
     */
    protected String absoluteBase = System.getProperty("user.dir");
    
    
	// ---------------------------- Properties ------------------------
    
    /**
     * Set the document root.
     */
    public void setDocBase(String docBase) {
    	if (docBase == null)
            throw new IllegalArgumentException("resources.null");
    	
    	// Calculate a File object referencing this document base directory
        base = new File(docBase);
        try {
        	base = base.getCanonicalFile();
        } catch (IOException e) {
        	// Ignore
        }
        
     // Validate that the document base is an existing directory
        if (!base.exists() || !base.isDirectory() || !base.canRead())
            throw new IllegalArgumentException("fileResources.base");
        this.absoluteBase = base.getAbsolutePath();
    }

    public File getBase() {
		return base;
	}

	public void setBase(File base) {
		this.base = base;
	}

	public String getAbsoluteBase() {
		return absoluteBase;
	}

	public void setAbsoluteBase(String absoluteBase) {
		this.absoluteBase = absoluteBase;
	}
	
	
	
	
	// --------------------- DirContext Methods --------------------
	
	/**
     * Retrieves selected attributes associated with a named object.
     * See the class description regarding attribute models, attribute type
     * names, and operational attributes.
     *
     * @return the requested attributes; never null
     * @param name the name of the object from which to retrieve attributes
     * @param attrIds the identifiers of the attributes to retrieve. null
     * indicates that all attributes should be retrieved; an empty array
     * indicates that none should be retrieved
     * @exception NamingException if a naming exception is encountered
     */
	public Attributes getAttributes(String name, String[] attrIds)
    throws NamingException {
		
		// Building attribute list
		File file = file(name);
		
		 if (file == null)
	            throw new NamingException("resources.notFound");
		 
		 return new FileResourceAttributes(file);
	}
	
	/**
     * Return a File object representing the specified normalized
     * context-relative path if it exists and is readable.  Otherwise,
     * return <code>null</code>.
     *
     * @param name Normalized context-relative path (with leading '/')
     */
    protected File file(String name) {
    	
    	File file = new File(base, name);
    	if (file.exists() && file.canRead()) {
    		
    		// Check that this file belongs to our root path
            String canPath = null;
            try {
                canPath = file.getCanonicalPath();
            } catch (IOException e) {
            }
            if (canPath == null)
                return null;
            
            // Check to see if going outside of the web application root
        	if (!canPath.startsWith(absoluteBase)) {
        		return null;
        	}
	
        	return file;
    	}
    	return null;
    }
	
	
    
	// ------------------ Context Methods ------------------
    
    /**
     * Retrieves the named object.
     *
     * @param name the name of the object to look up
     * @return the object bound to name
     * @exception NamingException if a naming exception is encountered
     */
    public Object lookup(String name)
        throws NamingException {
    	
    	Object result = null;
    	File file = file(name);
    	
    	if (file == null)
            throw new NamingException("resources.notFound");
    	
    	if (file.isDirectory()) {
    		FileDirContext tempContext = new FileDirContext(env);
    		tempContext.setDocBase(file.getPath());
    		result = tempContext;
    	}
    	else
    		result = new FileResource(file);
    	
    	return result;
    }
    
    
    
    
    
    
    

	@Override
	public Attributes getAttributes(Name name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Attributes getAttributes(String name) throws NamingException {
		// Building attribute list
        File file = file(name);

        if (file == null)
            throw new NamingException("resources.notFound");

        return new FileResourceAttributes(file);
	}

	@Override
	public Attributes getAttributes(Name name, String[] attrIds)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void modifyAttributes(Name name, int mod_op, Attributes attrs)
			throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void modifyAttributes(String name, int mod_op, Attributes attrs)
			throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void modifyAttributes(Name name, ModificationItem[] mods)
			throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void modifyAttributes(String name, ModificationItem[] mods)
			throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bind(Name name, Object obj, Attributes attrs)
			throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bind(String name, Object obj, Attributes attrs)
			throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rebind(Name name, Object obj, Attributes attrs)
			throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rebind(String name, Object obj, Attributes attrs)
			throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DirContext createSubcontext(Name name, Attributes attrs)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DirContext createSubcontext(String name, Attributes attrs)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DirContext getSchema(Name name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DirContext getSchema(String name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DirContext getSchemaClassDefinition(Name name)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DirContext getSchemaClassDefinition(String name)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name,
			Attributes matchingAttributes, String[] attributesToReturn)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name,
			Attributes matchingAttributes, String[] attributesToReturn)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name,
			Attributes matchingAttributes) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name,
			Attributes matchingAttributes) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filter,
			SearchControls cons) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filter,
			SearchControls cons) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filterExpr,
			Object[] filterArgs, SearchControls cons) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name,
			String filterExpr, Object[] filterArgs, SearchControls cons)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	

	@Override
	public void bind(Name name, Object obj) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bind(String name, Object obj) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rebind(Name name, Object obj) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rebind(String name, Object obj) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unbind(Name name) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unbind(String name) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rename(Name oldName, Name newName) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rename(String oldName, String newName) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public NamingEnumeration<NameClassPair> list(Name name)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String name)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<Binding> listBindings(Name name)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<Binding> listBindings(String name)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void destroySubcontext(Name name) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroySubcontext(String name) throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Context createSubcontext(Name name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Context createSubcontext(String name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookupLink(Name name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookupLink(String name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NameParser getNameParser(Name name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NameParser getNameParser(String name) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Name composeName(Name name, Name prefix) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String composeName(String name, String prefix)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object addToEnvironment(String propName, Object propVal)
			throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object removeFromEnvironment(String propName) throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Hashtable<?, ?> getEnvironment() throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() throws NamingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getNameInNamespace() throws NamingException {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	// ----------- FileResourceAttributes Inner Class ------------
	
	protected class FileResourceAttributes extends ResourceAttributes {
		
		// ----------------- Constructor -----------------
		public FileResourceAttributes(File file) {
            this.file = file;
            getCreation();
            getLastModified();
        }
		
		
		// ----------------- Member Variables ------------------
		
		protected File file;

        protected boolean accessed = false;

        protected String canonicalPath = null;
        
        // ------------ ResourceAttributes Methods ------------
        
        /**
         * Get content length.
         *
         * @return content length value
         */
        public long getContentLength() {
            if (contentLength != -1L)
                return contentLength;
            contentLength = file.length();
            return contentLength;
        }
        
        /**
         * Get creation time.
         *
         * @return creation time value
         */
        public long getCreation() {
            if (creation != -1L)
                return creation;
            creation = getLastModified();
            return creation;
        }
        
        /**
         * Get last modified time.
         *
         * @return lastModified time value
         */
        public long getLastModified() {
            if (lastModified != -1L)
                return lastModified;
            lastModified = file.lastModified();
            return lastModified;
        }
		
	}
	
	
	
	// --------------- FileResource Inner Class ---------------- 
	
	/**
     * This specialized resource implementation avoids opening the InputStream
     * to the file right away (which would put a lock on the file).
     */
    protected class FileResource extends Resource {
    	
    	// -------------------- Constructor --------------------

        public FileResource(File file) {
            this.file = file;
        }

        // ----------------- Member Variables ------------------

        /**
         * Associated file object.
         */
        protected File file;


        /**
         * File length.
         */
        protected long length = -1L;


        // ---------------- Resource Methods ----------------
        
        /**
         * Content accessor.
         *
         * @return InputStream
         */
        public InputStream streamContent()
            throws IOException {
        	
        	if (binaryContent == null) {
        		FileInputStream fis = new FileInputStream(file);
                inputStream = fis;
                return fis;
        	}
        	return super.streamContent();
        }
    }
	
	
}
