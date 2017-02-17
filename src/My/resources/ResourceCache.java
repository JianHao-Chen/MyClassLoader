package My.resources;

import java.util.HashMap;

public class ResourceCache {
	
	// ------------------- Constructors -------------------
    public ResourceCache(){
    	
    }
    
    // ----------------- Instance Variables ----------------- 
    
    /**
     * Cache.
     * Path -> Cache entry.
     */
    protected CacheEntry[] cache = new CacheEntry[0];
    
    /**
     * Not found cache.
     */
    protected HashMap notFoundCache = new HashMap();
    
    /**
     * Max size of resources which will have their content cached.
     */
    protected int cacheMaxSize = 10240; // 10 MB
    
    
    /**
     * Current cache size in KB.
     */
    protected int cacheSize = 0;
    
    /**
     * Number of accesses to the cache.
     */
    protected long accessCount = 0;
    
    /**
     * Number of cache hits.
     */
    protected long hitsCount = 0;
    
	// --------------------- Properties ---------------------
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
     * Return the current cache size in KB.
     */
    public int getCacheSize() {
        return cacheSize;
    }
    
    
	// --------------------- Public Methods ---------------------
    
    public boolean allocate(int space) {
    	
    	int toFree = space - (cacheMaxSize - cacheSize);
    	
    	if (toFree <= 0) {
            return true;
        }
    	
    	return false;
    }
    
    
    
    public CacheEntry lookup(String name) {
    	
    	CacheEntry cacheEntry = null;
    	CacheEntry[] currentCache = cache;
    	accessCount++;
    	int pos = find(currentCache, name);
    	if((pos!=-1)&&(name.equals(currentCache[pos].name))){
    		cacheEntry = currentCache[pos];
    	}
    	
    	if (cacheEntry == null) {
    		try {
    			cacheEntry = (CacheEntry) notFoundCache.get(name);
    		}
    		catch (Exception e) {
                // Ignore: the reliability of this lookup is not critical
            }
    	}
    	
    	if (cacheEntry != null) {
    		hitsCount++;
    	}
    	
    	return cacheEntry;
    }
    
    
    public void load(CacheEntry entry) {
    	if (entry.exists) {
    		if (insertCache(entry)) {
    			cacheSize += entry.size;
    		}
    	}
    }
    
    
    /**
     * Find a map elemnt given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
    private static final int find(CacheEntry[] map, String name) {
    	int a = 0;
        int b = map.length - 1;
        
        // Special cases: -1 and 0
        if (b == -1) {
            return -1;
        }
        
        if (name.compareTo(map[0].name) < 0) {
        	return -1;
        }
        
        if (b == 0) {
            return 0;
        }
        
        int i = 0;
        
        while (true){
        	i = (b + a) / 2;
        	int result = name.compareTo(map[i].name);
        	if (result > 0) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
        	if ((b - a) == 1) {
        		int result2 = name.compareTo(map[b].name);
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
        	}
        	
        }
    }
    
    /**
     * Insert into the right place in a sorted MapElement array, and prevent
     * duplicates.
     */
    private final boolean insertCache(CacheEntry newElement) {
    	CacheEntry[] oldCache = cache;
    	int pos = find(oldCache, newElement.name);
    	
    	// if already exsit return false 
    	if ((pos != -1) && (newElement.name.equals(oldCache[pos].name))) {
            return false;
        }
    	
    	CacheEntry[] newCache = new CacheEntry[cache.length + 1];
    	System.arraycopy(oldCache, 0, newCache, 0, pos + 1);
    	newCache[pos + 1] = newElement;
    	System.arraycopy
        	(oldCache, pos + 1, newCache, pos + 2, oldCache.length - pos - 1);
    	cache = newCache;
    	return true;
    }
    
}
