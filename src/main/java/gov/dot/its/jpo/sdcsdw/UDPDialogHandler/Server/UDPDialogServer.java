package gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Exception.InitializationError;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Exception.ProcessingFailedException;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Model.Response;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Session.LocalSessionHandler;

/**
 * @author mna30547 This Class creates a thread which listens on a port for UDP
 *         messages
 */
public class UDPDialogServer implements Runnable {

	public UDPDialogServer(DialogHandler dialogHandler, int ingressPort) {
		this.dialogHandler = dialogHandler;
		this.ingressPort = ingressPort;
	}

	@Override
	public void run() {
		// If able to initialize run, else exit
		if (initializeSockets()) {
			listen();
		}
	}

	/**
	 * Create ingress and egress socket that UDP messages travel on
	 * 
	 * @return boolean: initialization successful?
	 */
	public boolean initializeSockets() {

		boolean successfullyInitialized = false;
		setIngressPort(this.ingressPort);

		// Create Ingress Socket
		try {
			setIngressSocket(new DatagramSocket(getIngressPort()));
		} catch (SocketException e) {
			logger.error(String.format("Failed to create socket on port: %d in UDPReceiver", getIngressPort()), e);
			return successfullyInitialized;
		}

		// Create Egress Socket
		try {
			setEgressSocket(new DatagramSocket());
		} catch (SocketException e) {
			logger.error(String.format("Failed to create socket on port: %d in UDPReceiver",
					getEgressSocket().getLocalPort()), e);
			return successfullyInitialized;
		}
		// if you make it here, you have successfully initialized
		successfullyInitialized = true;
		return successfullyInitialized;

	}

	/**
	 * Listen on the specified port for UDP messages
	 */
	public void listen() {
		DatagramSocket socket = getIngressSocket();
		byte[] receiveData = new byte[DEFAULT_PACKET_SIZE];
		logger.info("Started listener on port: " + socket.getLocalPort());
		while (true) {
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				socket.receive(receivePacket);
				logger.info("Received a message");
				// When a message is received pass it on to the DialogHandler
				forwardMessageToDialogHandler(receivePacket);
			} catch (IOException e) {
				logger.error(String.format("Crashed while listening on port %d", socket.getLocalPort()), e);
			}
		}

	}

	/**
	 * Pass message to the dialog handler
	 * 
	 * @param receivePacket
	 */
	private void forwardMessageToDialogHandler(DatagramPacket receivePacket) {

		logger.info("Forwarding message to dialogHandler");
		Response response = null;
		try {
			response = this.dialogHandler.processAndCreateResponse(receivePacket.getAddress(), receivePacket.getData());
		} catch (ProcessingFailedException e) {
			logger.error("Failed to create response for request:" + e.getMessage(), e);
			return;
		}
		sendMessageResponse(response);
	}

	private void sendMessageResponse(Response response) {
		// TODO Auto-generated method stub
		// Create packet to be sent as response
		InetAddress receiverAddress = response.getDestination().getAddress();
		int receiverPort = response.getDestination().getPort();
		logger.info(
				String.format("SENDING MESSAGE RESPONSE TO %S:%d\n", receiverAddress.getHostAddress(), receiverPort));

		if (response.containsMultiple()) {
			for (byte[] responseMessage : response.getResponseList()) {
				DatagramPacket packet = new DatagramPacket(responseMessage, responseMessage.length, receiverAddress,
						receiverPort);
				try {
					getEgressSocket().send(packet);
					logger.info("Sent message response: Done");
				} catch (IOException e) {
					logger.error("Crashed while sending response message", e);
				}
			}
		}

		else {
			byte[] responseMessage = response.getResponseMessage();

			DatagramPacket packet = new DatagramPacket(responseMessage, responseMessage.length, receiverAddress,
					receiverPort);
			try {
				getEgressSocket().send(packet);
				logger.info("Sent message response: Done");
			} catch (IOException e) {
				logger.error("Crashed while sending response message", e);
			}
		}
	}

	private DatagramSocket getEgressSocket() {
		// TODO Auto-generated method stub
		return this.egressSocket;
	}

	private void setEgressSocket(DatagramSocket datagramSocket) {
		this.egressSocket = datagramSocket;

	}

	private int getIngressPort() {
		return this.ingressPort;
	}

	private void setIngressPort(int port) {
		this.ingressPort = port;
	}

	private void setIngressSocket(DatagramSocket socket) {
		this.ingressSocket = socket;
	}

	private DatagramSocket getIngressSocket() {
		return this.ingressSocket;
	}

	private DialogHandler dialogHandler;
	private int ingressPort;
	private int DEFAULT_PACKET_SIZE = 65535;
	private DatagramSocket ingressSocket;
	private DatagramSocket egressSocket;
	private final static Logger logger = Logger.getLogger(UDPDialogServer.class);

}
