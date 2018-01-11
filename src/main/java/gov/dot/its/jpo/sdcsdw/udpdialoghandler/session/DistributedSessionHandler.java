package gov.dot.its.jpo.sdcsdw.udpdialoghandler.session;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.bson.Document;

import gov.dot.its.jpo.sdcsdw.udpdialoghandler.dao.SessionsDAO;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.exception.KeyNotFoundException;

/**
 * 
 * Simple Session Handler keeps track of a request ID and destination
 * 
 * @author mna30547
 *
 */
public class DistributedSessionHandler implements SessionHandlerInterface {

	public DistributedSessionHandler(SessionsDAO sessionsDAO) {
		this.sessionsDAO = sessionsDAO;
	}

	public InetSocketAddress getSession(String requestID) throws KeyNotFoundException {

		Document session = this.sessionsDAO.getSession(requestID);

		if (session.isEmpty()) {
			throw new KeyNotFoundException("No session exists for request ID: " + requestID);
		}

		logger.info("Got session:" + session.toJson());
		String sourceIP = (String) session.get("sourceip");
		int sourcePort = (Integer) session.get("sourceport");
		InetSocketAddress socketAddress = new InetSocketAddress(sourceIP, sourcePort);
		return socketAddress;
	}

	public void createSession(String requestID, InetSocketAddress srcAddress) {
		logger.info(String.format("Create session for requestID %s with destination %s:%d\n", requestID,
				srcAddress.getAddress().getHostAddress(), srcAddress.getPort()));

		this.sessionsDAO.insertSession(requestID, srcAddress);
		logger.info("Session Created");

	}

	public void removeSession(String requestID) {
		this.sessionsDAO.removeSession(requestID);

	}

	private SessionsDAO sessionsDAO;
	private final static Logger logger = Logger.getLogger(DistributedSessionHandler.class);
}
