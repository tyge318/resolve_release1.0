package wordnet;

import java.io.*; 
import java.net.*;
import java.util.HashMap;

public class StatsLookUp {
	public static HashMap<String, String> statsTable = null;
	public static void main(String args[]) {
		buildStatsHash("stats.txt");
		try{ 
			 int serverPort = 3456; //Integer.parseInt(args[0]); 
			 ServerSocket lookUpServer = new ServerSocket(serverPort); 
			 System.out.println("LookUp Process running...");
			 while (true){ 
				 Socket clientSocket = lookUpServer.accept( ); 
//				 lookUpThread thread = new lookUpThread(clientSocket); 
				 Thread newT = new Thread(new lookUpThread(clientSocket));
				 newT.start( ); 
			 } 		 
		} 
		catch(Exception e){e.printStackTrace( );}
	}
	public static void buildStatsHash(String path) {
		statsTable = new HashMap<String, String>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String entry;
			while ( (entry = br.readLine()) != null) {
				if( !entry.equals("======================")) {
					String [] elements = entry.split("=");
					statsTable.put(elements[0], elements[1]);
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
class lookUpThread implements Runnable {
	Socket clientSocket;
	lookUpThread(Socket cs) {
		clientSocket = cs;
	}
	public void run() {
		try {
//			clientSocket.setSoTimeout(3*60*1000);
			InputStream Is = clientSocket.getInputStream();
			OutputStream Os = clientSocket.getOutputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(Is));
			PrintStream Ps = new PrintStream(Os);
			String key;
			while( !clientSocket.isClosed() && ((key = br.readLine())!= null)) {
//				String key;
				String result = (StatsLookUp.statsTable.get(key) == null) ? "" : (StatsLookUp.statsTable.get(key));
				Ps.println(result);
			}
			System.out.println("Thread ID:" + Thread.currentThread().getId() + " - Client closed socket.");
		}catch(Exception e){
			e.printStackTrace( );
		} 
	}
}