package gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Session;

import java.net.InetSocketAddress;
import java.util.HashMap;

import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Exception.KeyNotFoundException;



/**
 * 
 * Simple Session Handler keeps track of a request ID and destination
 * 
 * @author mna30547
 *
 */
public class LocalSessionHandler implements SessionHandlerInterface {
	private HashMap<String, InetSocketAddress> sessionMap = new HashMap<String, InetSocketAddress>();

	public InetSocketAddress getSession(String requestID) throws KeyNotFoundException {
		if (sessionMap.containsKey(requestID)) {
			return sessionMap.get(requestID);
		}
		else {
			throw new KeyNotFoundException("No session exists for request ID: " + requestID);
		}
	}

	public void createSession(String requestID, InetSocketAddress srcAddress) {
		// What if I call setSession but a session already exists?
		// In that case we will ignore the previous session, assume
		// that dialog is lost and overwrite its session with the new one
		sessionMap.put(requestID, srcAddress);

	}

	public void removeSession(String requestID) {
		// TODO Auto-generated method stub
		sessionMap.remove(requestID);
	}

}
