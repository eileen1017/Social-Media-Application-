//package broadcast;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;



/*
 * A server that delivers status messages to other users.
 */
public class Server {

	// Create a socket for the server 
	private static ServerSocket serverSocket = null;
	// Create a socket for the server 
	private static Socket userSocket = null;
	// Maximum number of users 
	private static int maxUsersCount = 5;
	// An array of threads for users
	private static userThread[] threads = null;


	public static void main(String args[]) {

		// The default port number.
		int portNumber = 58888;
		if (args.length < 2) {
			System.out.println("Usage: java Server <portNumber>\n"
					+ "Now using port number=" + portNumber + "\n" +
					"Maximum user count=" + maxUsersCount);
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
			maxUsersCount = Integer.valueOf(args[1]).intValue();
		}

		System.out.println("Server now using port number=" + portNumber + "\n" + "Maximum user count=" + maxUsersCount);
		
		
		userThread[] threads = new userThread[maxUsersCount];


		/*
		 * Open a server socket on the portNumber (default 8000). 
		 */
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}

		/*
		 * Create a user socket for each connection and pass it to a new user
		 * thread.
		 */
		while (true) {
			try {
				userSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxUsersCount; i++) {
					if (threads[i] == null) {
						threads[i] = new userThread(userSocket, threads);
						threads[i].start();
						break;
					}
				}
				if (i == maxUsersCount) {
					PrintStream output_stream = new PrintStream(userSocket.getOutputStream());
					output_stream.println("#busy");
					output_stream.close();
					userSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}

/*
 * Threads
 */
class userThread extends Thread {

	private String name = null;
	private String rname = null;
	private BufferedReader input_stream = null;
	private PrintStream output_stream = null;
	private Socket userSocket = null;
	private final userThread[] threads;
	private int maxUsersCount;
	private List<String> fLst = new ArrayList<String>();
	private List<String> ReqLst = new ArrayList<String>();

	public userThread(Socket userSocket, userThread[] threads) {
		this.userSocket = userSocket;
		this.threads = threads;
		maxUsersCount = threads.length;
	}

	public void run() {
		int maxUsersCount = this.maxUsersCount;
		userThread[] threads = this.threads;

		try {
			/*
			 * Create input and output streams for this client.
			 * Read user name.
			 */
            input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
            output_stream = new PrintStream(userSocket.getOutputStream());
            output_stream.println("Please enter your name: ");
            name = input_stream.readLine().trim();
            

			/* Welcome the new user. */
			output_stream.println("#Welcome "+ name + " to our Social Media App.\nIf you would like to exit from the chat, please enter Exit.");
            for (int i = 0; i < maxUsersCount; i++){
            	if (threads[i] != null && threads[i] != this){
            		threads[i].output_stream.println("New user "+ name+ " has started the chat.");
            	}
            }
            
			/* Start the conversation. */
			while (true) {
				String sentence = input_stream.readLine();
				if (sentence.startsWith("#Bye")){
					break;
				} 
				else if (sentence.startsWith("#friendme")){
					rname = sentence.split(" ")[1];
					synchronized (userThread.class){
						for (int i = 0; i < maxUsersCount; i++){
							if (threads[i] != null && threads[i].name.equals(rname)){
								threads[i].ReqLst.add(name);
								threads[i].output_stream.println("#friendme <requester " + name + ">");
							}
						}
					}
				} 
				else if (sentence.startsWith("#friends")){
					rname = sentence.split(" ")[1];
					if (this.ReqLst.contains(rname)){
						(this.fLst).add(rname);
						this.ReqLst.remove(rname);
						synchronized(userThread.class){
							for(int i = 0; i <maxUsersCount;i++){
								if (threads[i] != null && threads[i].name.equals(rname)){
									threads[i].fLst.add(name);
									threads[i].output_stream.println("#OKfriends "+ rname +" "+ name);
									this.output_stream.println("#OKfriends " + rname + " " + name);
								}
							}
						}
					}
				} 
				else if (sentence.startsWith("#FriendRequestDenied")){
					rname = sentence.split(" ")[1];
					if (this.ReqLst.contains(rname)){
						this.ReqLst.remove(rname);
						synchronized(userThread.class){
							for (int i = 0; i < maxUsersCount; i++){
								if (threads[i] != null && threads[i].name.equals(rname)){
									threads[i].output_stream.println("#DenyFriendRequest " + name);
								}
							}
						}
					}
				} 
				else if (sentence.startsWith("#unfriend")){
					rname = sentence.split(" ")[1];
					if (this.fLst.contains(rname)){
						this.fLst.remove(rname);
						synchronized(userThread.class){
							for (int i = 0; i < maxUsersCount; i++){
								if (threads[i] != null && threads[i].name.equals(rname)){
									threads[i].fLst.remove(name);
									threads[i].output_stream.println("#NotFriends " + rname + " " + name);
									this.output_stream.println("#NotFriends " + rname + " " + name);
								}
							}
						}
					}
				}
				else {
					for (int i = 0; i < maxUsersCount; i++){
						if (threads[i] != null && this.fLst.contains(threads[i].name)){
							threads[i].output_stream.println(name + " says: " + sentence + " .");
						}
					}
				} 
			}
			for (int i = 0; i < maxUsersCount;i++){
				if (threads[i]!=null && threads[i] != this){
					threads[i].output_stream.println("#Leave <" + name + "> leaves the room now.");
				}
			}
			output_stream.println("#Bye "+name+" .");

			// conversation ended.

			/*
			 * Clean up. Set the current thread variable to null so that a new user
			 * could be accepted by the server.
			 */
			synchronized (userThread.class) {
				for (int i = 0; i < maxUsersCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}
			/*
			 * Close the output stream, close the input stream, close the socket.
			 */
			input_stream.close();
			output_stream.close();
			userSocket.close();
		} catch (IOException e) {
		}
	}
}




