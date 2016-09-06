package campbest;

import java.net.*;
import java.io.*;
import java.util.logging.*;

/**
* @author Caroline Danzi
* 6 September 2016
* CSE 383
* ServerMain
*
* Implements a server that connects to any client but only knows the following protocol:
* 1) Send greeting to client (my uniqueid)
* 2) Receive a list of values from client
* 3) Sum values
* 4) Send response string to client
* 
* Used code from previous 283 assignments, written both by myself and Dr. Pierre St. Juste
* Received help from Dr. Campbell during help session
* Modeled some sections of code (marked below) after ClientMain, written by Dr. Campbell
*/
public class ServerMain {
	
	private int port;
	private ServerSocket servSocket;
	private Socket clientSocket;
	private DataInputStream dis;
	private DataOutputStream dos;
	private static Logger LOGGER = Logger.getLogger("info");
	private FileHandler fh = null;
	
	public static void main(String[] args) {
		// Argument parsing code from ClientMain written by Dr. Campbell
		int port = -1;
		try {
			port = Integer.parseInt(args[0]);
		}
		catch (Exception err) {
			System.err.println("Invalid usage - first arg must be port which is an integer");
			System.exit(-1);
		}
		ServerMain sm = new ServerMain(port);
		sm.Main();
	}
	
	/**
	 * Constructor
	 * @param port the port on which this server listens
	 */
	public ServerMain(int port) {
		// Logging code from ClientMain.java, written by Dr. Campbell
		try {
			fh = new FileHandler("server.log");
			LOGGER.addHandler(fh);
			LOGGER.setUseParentHandlers(false);
			SimpleFormatter formatter = new SimpleFormatter();  
			fh.setFormatter(formatter);  

		} catch (IOException err) {
			System.err.println("Error - can't open log file");
		}

		this.port = port;
		this.servSocket = null;
		this.clientSocket = null;
		this.dis = null;
		this.dos = null;
		LOGGER.info("ServerMain - Port = " + port);
	}
	
	/**
	 * Listens for a connection, then handles it according to protocol: 
	 * 1) Send greeting 2) Receive numbers from client 3) Send response
	 */
	private void Main() {
		// Create the server socket - will fail if port already in use
		try {
			servSocket = new ServerSocket(port);
		} catch (Exception e) {
			System.err.println("Error - Could not create server socket");
			LOGGER.log(Level.WARNING, "Could not create server scoket", e);
		}
		
		// Accept client connections and process them
		while(true) {
			try {
				clientSocket = servSocket.accept();
				LOGGER.log(Level.INFO, "Connected to client");
				clientSocket.setSoTimeout(10000); // Set timeout to 10 seconds
				dis = new DataInputStream(clientSocket.getInputStream());
				dos = new DataOutputStream(clientSocket.getOutputStream());

				sendGreeting();
				readValues();
				
			} catch (Exception e) {
				System.err.println("Error connecting to client");
				LOGGER.log(Level.SEVERE, "Error connecting to client", e);
			} finally {
				closeConnection();
			}
			
		}		
	}
	
	/**
	 * Sends a UTF string greeting containing my uniqueid
	 */
	private void sendGreeting() {
		try {
			dos.writeUTF("danzicr");
			dos.flush();
		} catch(Exception e) {
			System.err.println("Error sending greeting");
			LOGGER.log(Level.WARNING, "An error occurred while sending greeting", e);
		}
	}
	
	/**
	 * Read the values sent by the client and return the sum of those values.
	 * The input format from client is <int> <int | double>, where the first
	 * int represents the type of the number to follow (either 1 - int or 2 - double).
	 * A zero ends the client list. 
	 */
	private void readValues(){
		int type;
		double sum = 0;
		double num;
		int valueCount = 0;
		try {
			while((type = dis.readInt()) != 0) {
				// A 1 indicates the next number is an integer
				if(type == 1) {
					num = dis.readInt();
					if(willOverflow(sum, num)) {
						sendErrorResponse("Sum overflowed");
						return;
					}
					sum += num;
				// A 2 indicates the next number is a double
				} else if(type == 2) {
					num = dis.readDouble();
					if(willOverflow(sum, num)) {
						sendErrorResponse("Sum overflowed");
						return;
					}
					sum += num; 
				} else {
					sendErrorResponse("Error - Expected a 1 or a 2 but got " + type);
					LOGGER.log(Level.INFO, "Client behaved unexpectedly");
					return;
				}
				valueCount++;
			}
			// If the client sent less than two values or more than five, 
			// send an error message because this does not follow the protocol
			if(valueCount < 2) {
				sendErrorResponse("Received too few values");
				return;
			} else if(valueCount > 5) {
				sendErrorResponse("Received too many values");
				return;
			}
			sendOkResponse(sum);
		} catch(Exception e) {
			System.err.println("Error in reading list of values");
			LOGGER.log(Level.WARNING, "Error in reading list of values from client", e);
		}
	}
	
	/**
	 * Returns true if adding num to sum will result in a number that is 
	 * larger than the max double value
	 * @param sum the current sum
	 * @param num the number to add to the sum
	 * @return true if sum + num is greater than double max value and false otherwise
	 */
	private boolean willOverflow(double sum, double num) {
		if(Double.MAX_VALUE - sum < num) {
			return true;
		}
		return false;
	}
	
	/**
	 * Send a UTF string with 'OK' and the sum of values
	 * 
	 * @param sum the sum of the client's values represented as a double
	 */
	private void sendOkResponse(double sum) {
		try {
			dos.writeUTF("OK ");
			dos.writeDouble(sum);
			dos.flush();
		} catch(Exception e) {
			System.err.println("Error - could not send repsonse string");
			LOGGER.log(Level.WARNING, "Error - could not send response string", e);
		}
	}
	
	/**
	 * Sends an error response to the client with a corresponding message
	 * @param error The error message to send to the client
	 */
	private void sendErrorResponse(String error) {
		try {
			dos.writeUTF("ERROR " + error);
			dos.flush();
			closeConnection();
		} catch(Exception e) {
			System.err.println("Error - could not send error to client");
			LOGGER.log(Level.WARNING, "Error - could not send error to client", e);
		}
	}
	
	/**
	 * CLose all input/output streams (which also flushes them) and
	 * close the socket connection
	 */
	private void closeConnection() {
		try {
			dis.close();
			dos.close();
			clientSocket.close();
			LOGGER.log(Level.INFO, "Connection closed");
		} catch(Exception e) {
			System.err.println("Error while closing connection");
			LOGGER.log(Level.WARNING, "Error closing connection", e);
		}
	}
}