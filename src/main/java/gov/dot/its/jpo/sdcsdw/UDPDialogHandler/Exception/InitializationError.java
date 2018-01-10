package gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Exception;

public class InitializationError extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @param msg Message for this exception
	 * @param cause The underlying exception
	 */
	public InitializationError(String msg, Exception cause)
	{
		super(msg, cause);
	}
	
	/**
	 * 
	 * @param msg Message for this exception
	 */
	public InitializationError(String msg)
	{
		super(msg);
	}
}