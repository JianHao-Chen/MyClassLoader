package My.resources;

import javax.naming.directory.DirContext;



public class CacheEntry {

	
	// --------------- Instance Variables -------------------------
	public long timestamp = -1;
    public String name = null;
    public ResourceAttributes attributes = null;
    public Resource resource = null;
    public DirContext context = null;
    public boolean exists = true;
    public long accessCount = 0;
    public int size = 1;
	
    // -------------------- Public Methods -------------------------
    
    public String toString() {
        return ("Cache entry: " + name + "\n"
                + "Exists: " + exists + "\n"
                + "Attributes: " + attributes + "\n"
                + "Resource: " + resource + "\n"
                + "Context: " + context);
    }
    
    
    
}
