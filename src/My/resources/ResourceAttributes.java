package My.resources;

import java.util.Date;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

public class ResourceAttributes implements Attributes{
	
	
	// --------------------- Constructors ---------------------
	
	
	/**
     * Default constructor.
     */
    public ResourceAttributes() {
    }
    
    /**
     * Merges with another attribute set.
     */
    public ResourceAttributes(Attributes attributes) {
        this.attributes = attributes;
    }
	
	// ------------------- Instance Variables -------------------
    
    /**
     * External attributes.
     */
    protected Attributes attributes = null;
    
    /**
     * Content length.
     */
    protected long contentLength = -1;

    /**
     * Creation time.
     */
    protected long creation = -1;
    
    
    /**
     * Last modified time.
     */
    protected long lastModified = -1;


    /**
     * Last modified date.
     */
    protected Date lastModifiedDate = null;
    
	// --------------------- Properties ---------------------
    
    
    /**
     * Get content length.
     * 
     * @return content length value
     */
    public long getContentLength() {
    	if (contentLength != -1L)
            return contentLength;
    	
    	return 0;
    }
    
    /**
     * Get last modified time.
     * 
     * @return lastModified time value
     */
    public long getLastModified() {
    	if (lastModified != -1L)
            return lastModified;
    	
    	return lastModified;
    }

	@Override
	public boolean isCaseIgnored() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Attribute get(String attrID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<? extends Attribute> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<String> getIDs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Attribute put(String attrID, Object val) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Attribute put(Attribute attr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Attribute remove(String attrID) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
     * Clone the attributes object (WARNING: fake cloning).
     */
    public Object clone() {
        return this;
    }

}
