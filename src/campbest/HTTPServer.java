package campbest;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author Caroline Danzi
 * 6 September 2016
 * CSE 383
 * HTTPServer
 * 
 * Implements an HTTP Server that responds to GET requests with the following plain text: 
 * Caroline Danzi
 * danzicr
 * The path passed to it
 * All Headers passed to it
 * 
 * It responds to all POST requests with the string "POST"
 * For all other methods it returns a 500 error
 * 
 * I certify that this is my own work, with the exception of some code borrowed from 
 * programs written by Dr. Campbell (marked below) and Dr. St. Juste (marked below)
 */
public class HTTPServer {
	
	private int port;
	private ServerSocket servSock;
	private Socket clientSock;
	private InputStream is;
	private OutputStream os;
	private final int BUF_SIZE = 1024000;
	private final String CRLF = "\r\n";
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
		HTTPServer server = new HTTPServer(port);
		server.Main();
	}
	
	/**
	 * Constructor
	 * @param port the port on which this server listens
	 */
	public HTTPServer(int port) {
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
		this.servSock = null;
		this.clientSock = null;
		this.is = null;
		this.os = null;
	}
	
	/**
	 * Code for connecting to and handling client requests
	 */
	public void Main() {
		// Create the server socket
		try {
			servSock = new ServerSocket(port);
		} catch(Exception e) {
			System.err.println("Error - Could not create server socket");
			LOGGER.log(Level.SEVERE, "Could not create server scoket", e);
		}
		
		while(true) {
			try {
				// Accept connection and get input and output streams
				clientSock = servSock.accept();
				LOGGER.log(Level.INFO, "Connected to client");
				clientSock.setSoTimeout(10000);
				is = clientSock.getInputStream();
				os = clientSock.getOutputStream();
				
				// Get the client's request headers, determine
				// the method and then process the request
				String request = getClientRequest();
				String method = getRequestMethod(request);
				if(method.equals("GET")) {
					processGET(request);
				} else if(method.equals("POST")) {
					processPOST();
				} else {
					sendError("Did not recognize request");
				}
			} catch(Exception e) {
				System.err.println("Error connecting to client");
				LOGGER.log(Level.WARNING, "Error connecting to client in Main", e);
			} finally {
				closeConnection();
			}
		}
	}
	
	/**
	 * Gets the content the client sends over and converts it to a string
	 * Code taken from previous project in 283. This particular method was
	 * written by Dr. Pierre St. Juste and modified by me. 
	 * 
	 * @return the client request as a String
	 */
	private String getClientRequest(){
		int total = 0, rcv = 0;
		byte[] buffer = new byte[BUF_SIZE];
		String msg = "";
		try {
			// InputStream.read returns -1 when there are no more bytes to read
			while (rcv != -1) {
				rcv = is.read(buffer, total, BUF_SIZE - total - 1);
				msg += new String(buffer, total, rcv);
				total += rcv;

				// Loop until seeing the double CRLF, which indicates the
				// end of the request header
				if (msg.indexOf(CRLF + CRLF) != -1) {
					LOGGER.log(Level.INFO, "Read " + total + " bytes from client");
					break;
				}
			}
		} catch(Exception e) {
			System.err.println("Error while reading bytes from client");
			LOGGER.log(Level.SEVERE, "Error - trouble reading bytes from client in getClientRequest", e);
			sendError("Could not read request");
		}
		// return the client request as a string
		return msg;
	}
	
	/**
	 * Determine the type of request
	 * @param header the header from the client request as a String
	 * @return the request type
	 */
	private String getRequestMethod(String header) {
		return header.substring(0, header.indexOf(' '));
	}
	/**
	 * Responds to a GET request with the following plain text: 
	 * Caroline Danzi
	 * danzicr
	 * The path passed to it
	 * All headers passed to it
	 * @param header the request header from the client
	 */
	private void processGET(String header) {
		StringTokenizer st = new StringTokenizer(header);
		// Pass over the first token, which is the HTTP method
		st.nextToken();
		String path = st.nextToken();
		String response = "HTTP/1.1 200 OK\r\nContent-type: text/plain\r\n\r\n"
				+ "Caroline Danzi\ndanzicr\n" + path + "\n" + header + "\n";
		try {
			os.write(response.getBytes());
			os.flush();
			LOGGER.log(Level.INFO, "Sent GET response");
		} catch(Exception e) {
			System.err.println("Error sending GET response to client");
			LOGGER.log(Level.SEVERE, "Error while sending GET response to client, in processGET", e);
		}
	}
	
	/**
	 * Responds to a POST request by sending the String "POST"
	 */
	private void processPOST() {
		try {
			String response = "HTTP/1.1 200 OK\r\nContent-type: text/plain\r\n\r\nPOST";
			os.write(response.getBytes());
			os.flush();
			LOGGER.log(Level.INFO, "Sent POST response");
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error - could not send POST response, in processPOST", e);
		}
	}
	
	/**
	 * Send a 500 error message to the client
	 * @param error the error message to send
	 */
	private void sendError(String error) {
		String message = "HTTP/1.1 500 Internal Server Error\n" + error;
		try {
			os.write(message.getBytes());
			os.flush();
			LOGGER.log(Level.INFO, "Sent error message to client: " + error);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error - could not send error message to client, in sendError", e);
		}
	}
	
	/**
	 * Closes the input stream, flushes the output stream and closes it, 
	 * and closes the client socket
	 */
	public void closeConnection() {
		try {
			is.close();
			os.flush();
			os.close();
			clientSock.close();
			LOGGER.log(Level.INFO, "Connection closed");
		} catch(Exception e) {
			System.err.println("Error closing connection");
			LOGGER.log(Level.SEVERE, "Error closing connection, in closeConnection", e);
		}
	}

}
