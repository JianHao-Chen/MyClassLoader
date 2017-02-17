package My.loader;

public class StartUp {
	
	public static void main(String[] arg){
		System.out.println(System.getProperty("user.dir"));
		
		WebappLoader loader = new WebappLoader();
		loader.start();
	}

}
