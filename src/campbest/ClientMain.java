package campbest;
import java.net.*;
import java.io.*;
import java.util.logging.*;

/*
 * Scott Campbell
 * Client for first programming project for cse383 f16
 * 
 * This client will:
 * Normally description would go here.
 * 
 * I certify this is my own work
 */
public class ClientMain {
	int port;
	String fqdn;
	Socket sock = null;
	DataInputStream dis = null;
	DataOutputStream dos = null;
	private static Logger LOGGER = Logger.getLogger("info");
	FileHandler fh = null;



	//Main method - DO NO WORK IN PSVM, just invoke other classes
	public static void main(String[] args) {
		int port = -1;
		try {
			port = Integer.parseInt(args[0]);
		}
		catch (Exception err) {
			System.err.println("Invalid usage - first arg must be port which is an integer");
			System.exit(-1);
		}
		if (args.length < 6)
		{
			System.err.println("Invalid usage - port FQDN VALUES (at least 2 values required)");
		}
		String fqdn = args[1];
		//copy over values
		java.util.ArrayList<String> values = new java.util.ArrayList<String>();
		for (int i=2;i<args.length;i++) {
			values.add(args[i]);
		}

		ClientMain client = new ClientMain(port,fqdn);
		client.Main(values);
	}

	//Construtor
	public ClientMain(int port, String fqdn) {
		try {
			fh = new FileHandler("client.log");
			LOGGER.addHandler(fh);
			LOGGER.setUseParentHandlers(false);
			SimpleFormatter formatter = new SimpleFormatter();  
			fh.setFormatter(formatter);  

		} catch (IOException err) {
			System.err.println("Error - can't open log file");
		}

		LOGGER.info("ClientMAIN - Port = " + port + " fqdn=" + fqdn);
		this.fqdn = fqdn;
		this.port = port;
	}

	//main - takes an arraylist of arguments -> each argument is type, number. 
	public void Main(java.util.ArrayList<String> values) {
		boolean result = false;
		String greeting = "";
		String response = "";

		for (int retry = 5; retry > 0; retry --) {
			try {
				LOGGER.info("Connecting");
				connect();
				greeting = readGreeting();
				System.err.println(greeting);
				sendValues(values);
				response = readResponse();
				result = true;
			} catch (IOException err) {
				System.err.println("Error during protocol " + err.toString());
				LOGGER.log(Level.SEVERE,"error during connection", err);
			}
		}
		if (result) {
			System.out.println("Success");
			System.out.println("Greeting = " + greeting);
			System.out.println("Response => " + response);
		}
		else {
			System.out.println("Failed");
		}
	}

	//connects to server
	public void connect() throws IOException {
		sock = new Socket(fqdn,port);
		dis = new DataInputStream(sock.getInputStream());
		dos = new DataOutputStream(sock.getOutputStream());
		sock.setSoTimeout(5000); // 5 second timeout
	}

	//server sends initial greeting that should contain students uniqueid
	public String readGreeting() throws IOException {
		String greet = dis.readUTF();
		return greet;
	}

	//send values to server
	public void sendValues(java.util.ArrayList<String> values) throws IOException {
		java.util.Iterator<String> itr = values.iterator();
		while (itr.hasNext()) {
			String type = itr.next();
			if (!itr.hasNext()) {
				System.err.println("Invalid input - each value must be type number");
				System.exit(-1);
			}
			if (!itr.hasNext()) {
				System.err.println("Error - values must be TYPE NUM");;
				System.exit(-1);
			}
			String num = itr.next();
			if (type.equals("i")) {
				dos.writeInt(1);
				try {
					dos.writeInt(Integer.parseInt(num));
				} catch (NumberFormatException err) {
					System.err.println("Error parsing int " + num);
					System.exit(-1);
				}
			} else if (type.equals("d")) {
				dos.writeInt(2);
				try {
					dos.writeDouble(Double.parseDouble(num));;
				} catch (NumberFormatException err) {
					System.err.println("Error parsing double " + num);
					System.exit(-1);
				}
			} else {
				System.err.println("Invalid input - invalid type");
				System.exit(-1);
			}
		}
		dos.writeInt(0);	//signifies end of values
		dos.flush();
	}

	//read response - first string should be "OK" or "Error - and error message"  Then it should send back SUM of numbers as a double
	public String readResponse() throws IOException {
		String response = dis.readUTF();
		response += " sum of values = ";
		response += dis.readDouble();
		return response;
	}
}
