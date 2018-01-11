package gov.dot.its.jpo.sdcsdw.udpdialoghandler.session;

import java.net.InetSocketAddress;

import gov.dot.its.jpo.sdcsdw.udpdialoghandler.exception.KeyNotFoundException;

public interface SessionHandlerInterface {
	public InetSocketAddress getSession(String requestID) throws KeyNotFoundException;

	public void createSession(String requestID, InetSocketAddress srcAddress);

	public void removeSession(String requestID);
}
