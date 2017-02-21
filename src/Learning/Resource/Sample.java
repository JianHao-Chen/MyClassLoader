package Learning.Resource;

public class Sample{
	
	private Sample instance; 
	/*
	 * setSample接受一个 java.lang.Object类型的参数，
	 * 并且会把该参数强制转换成 Sample类型。
	 */
	public void setSample(Object instance) { 
        this.instance = (Sample) instance; 
    } 
}
