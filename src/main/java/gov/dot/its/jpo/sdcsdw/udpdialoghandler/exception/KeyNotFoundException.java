package gov.dot.its.jpo.sdcsdw.udpdialoghandler.exception;

public class KeyNotFoundException extends Exception
{


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @param msg Message for this exception
	 * @param cause The underlying exception
	 */
	public KeyNotFoundException(String msg, Exception cause)
	{
		super(msg, cause);
	}
	
	/**
	 * 
	 * @param msg Message for this exception
	 */
	public KeyNotFoundException(String msg)
	{
		super(msg);
	}
}