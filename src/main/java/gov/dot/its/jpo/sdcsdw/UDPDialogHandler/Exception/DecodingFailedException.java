package gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Exception;

public class DecodingFailedException extends Exception
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
	public DecodingFailedException(String msg, Exception cause)
	{
		super(msg, cause);
	}
	
	/**
	 * 
	 * @param msg Message for this exception
	 */
	public DecodingFailedException(String msg)
	{
		super(msg);
	}
}
