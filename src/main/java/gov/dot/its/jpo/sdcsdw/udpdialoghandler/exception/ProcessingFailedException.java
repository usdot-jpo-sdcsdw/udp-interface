package gov.dot.its.jpo.sdcsdw.udpdialoghandler.exception;

public class ProcessingFailedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @param msg
	 *            Message for this exception
	 * @param cause
	 *            The underlying exception
	 */
	public ProcessingFailedException(String msg, Exception cause) {
		super(msg, cause);
	}

	/**
	 * 
	 * @param msg
	 *            Message for this exception
	 */
	public ProcessingFailedException(String msg) {
		super(msg);
	}
}
