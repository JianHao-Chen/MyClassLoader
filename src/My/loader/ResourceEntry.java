package My.loader;

import java.net.URL;

public class ResourceEntry {
	/**
     * The "last modified" time of the origin file at the time this class
     * was loaded, in milliseconds since the epoch.
     */
    public long lastModified = -1;


    /**
     * Binary content of the resource.
     */
    public byte[] binaryContent = null;


    /**
     * Loaded class.
     */
    public volatile Class loadedClass = null;


    /**
     * URL source from where the object was loaded.
     */
    public URL source = null;


    /**
     * URL of the codebase from where the object was loaded.
     */
    public URL codeBase = null;
}
