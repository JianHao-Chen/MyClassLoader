package My.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 	Encapsulate the contents of a resource.
 * 
 */
public class Resource {

	// ---------------------- Constructors ---------------------
	
	public Resource() {
    }
    
    
    public Resource(InputStream inputStream) {
        setContent(inputStream);
    }
    
    
    public Resource(byte[] binaryContent) {
        setContent(binaryContent);
    }
    
	// ------------------ Instance Variables ------------------
    
    /**
     * Binary content.
     */
    protected byte[] binaryContent = null;
    
    
    /**
     * Input stream.
     */
    protected InputStream inputStream = null;
    
	// ---------------------- Properties ---------------------
    
    /**
     * Content accessor.
     * 
     * @return InputStream
     */
    public InputStream streamContent()
        throws IOException {
    	 if (binaryContent != null) {
             return new ByteArrayInputStream(binaryContent);
         }
         return inputStream;
    }
    
    /**
     * Content accessor.
     * 
     * @return binary content
     */
    public byte[] getContent() {
        return binaryContent;
    }
    
    /**
     * Content mutator.
     * 
     * @param inputStream New input stream
     */
    public void setContent(InputStream inputStream) {
        this.inputStream = inputStream;
    }
    
    
    /**
     * Content mutator.
     * 
     * @param binaryContent New bin content
     */
    public void setContent(byte[] binaryContent) {
        this.binaryContent = binaryContent;
    }
	
}
