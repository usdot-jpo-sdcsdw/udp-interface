package gov.dot.its.jpo.sdcsdw.udpdialoghandler.service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import gov.dot.its.jpo.sdcsdw.Models.AdvisorySituationDataDistribution;
import gov.dot.its.jpo.sdcsdw.Models.AdvisorySituationDataDistributionList;
import gov.dot.its.jpo.sdcsdw.Models.DataReceipt;
import gov.dot.its.jpo.sdcsdw.Models.DialogMessage;
import gov.dot.its.jpo.sdcsdw.Models.ServiceRequest;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.Asn1Type;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.Asn1Types;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.PerXerCodec;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.CodecFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.FormattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.UnformattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.per.RawPerData;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.xer.RawXerData;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.exception.DecodingFailedException;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.exception.KeyNotFoundException;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.exception.ProcessingFailedException;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.model.Response;
import gov.dot.its.jpo.sdcsdw.udpdialoghandler.session.SessionHandlerInterface;
import gov.dot.its.jpo.sdcsdw.xerjaxbcodec.XerJaxbCodec;

/**
 * 
 * @author mna30547 Receives a message, decodes it, unmarshalls it and sends it
 *         for processing
 */
public class DialogHandler {

	public DialogHandler(SessionHandlerInterface sessionHandler, MessageProcessor messageProcessor) {
		this.sessionHandler = sessionHandler;
		this.messageFactory = messageProcessor;
	}

	/**
	 * Given the originating address and PER encoded dialog message create a
	 * Response object. The Response object encapsulates the PER encoded dialog
	 * message response along with the destination for the response message
	 * 
	 * @param InetAddress
	 *            address
	 * @param byte[]
	 *            data
	 * @return Response
	 * @throws ProcessingFailedException
	 */
	public Response processAndCreateResponse(InetAddress address, byte[] data) throws ProcessingFailedException {
		DialogMessage messageObjectToReturn = null;

		// Create the response dialog message
		try {
			messageObjectToReturn = createDialogMessageResponseObject(address, data);
		} catch (ProcessingFailedException e) {
			throw new ProcessingFailedException("Failed to create response object:" + e.getMessage());
		}

		// Before doing any additional processing see if a session exists for this
		// object:
		// if no session exists (ie a message from the dialog sequence was sent before
		// the ServiceRequest)
		// there is no point in doing additional processing
		InetSocketAddress destination = null;
		try {
			destination = this.sessionHandler.getSession(messageObjectToReturn.getRequestID());
			logger.info(
					String.format("Found session, will send %s to %s:%d\n", messageObjectToReturn.getASN1MessageType(),
							destination.getAddress().getHostAddress(), destination.getPort()));
		} catch (KeyNotFoundException e) {
			throw new ProcessingFailedException("No session exists for the request");
		}

		/*-At this point we have a valid response object and know who it needs to go to
		There are two potential paths:
			(1) We received a ServiceRequest/DataAcceptance. We will respond with a 
					ServiceResponse/DataReceipt
			(2) We received a Data Request. We will respond with Advisory Situation Data Distribution(s)
			
			Regardless of which path from this point we simply need to encode the response in PER 
			and encapsulate it in a Response object so the UDP forwarded can handle it
		
		*/

		// SPECIAL CONSIDERATIONS:

		// We may have multiple Advisory Situation Data Distributions. If this is the
		// case, we want to
		// send each of these distributions separately. When given a DataRequest the
		// createDialogResponseMessage
		// function creates an AdvisorySituationDataDistributionList. This class simply
		// contains a list of distributions.
		if (messageObjectToReturn instanceof AdvisorySituationDataDistributionList) {
			List<byte[]> encodedDistributions = new ArrayList<byte[]>();
			// Encode each of the distributions
			for (AdvisorySituationDataDistribution distribution : ((AdvisorySituationDataDistributionList) messageObjectToReturn)
					.getDistributionList()) {
				// encode each distribution
				byte[] encodedResponseObject = encodeResponseObjectInPer(distribution);
				// Add each distribution to a list
				encodedDistributions.add(encodedResponseObject);
			}
			// Create response object: Remember, the response object encapsulates the
			// message(s) along
			// with the destination they should go to
			return new Response(encodedDistributions, destination);
		}

		// If we are responding with a DataReceipt:
		if (messageObjectToReturn instanceof DataReceipt) {
			// This is the final message the system will send in a given dialog
			// sequence, if we are sending this back it means the dialog is over, purge this
			// session from the handler!
			logger.info("Message was of type DataReceipt, remove session: " + messageObjectToReturn.getRequestID());
			this.sessionHandler.removeSession(messageObjectToReturn.getRequestID());
		}

		byte[] encodedResponseObject = encodeResponseObjectInPer(messageObjectToReturn);
		logger.info("Successfully encoded message:" + messageObjectToReturn.getASN1MessageType());
		return new Response(encodedResponseObject, destination);

	}

	/**
	 * Given a DialogMessage POJO return its per-encoded byte array representation
	 * 
	 * @param messageObjectToReturn
	 * @return
	 * @throws ProcessingFailedException
	 */
	private byte[] encodeResponseObjectInPer(DialogMessage messageObjectToReturn) throws ProcessingFailedException {
		logger.info("Converting response message object to XER");
		// Convert response object to XML (so that it can be encoded)
		String xerEncodedResponseMessageToReturn = "";
		try {
			xerEncodedResponseMessageToReturn = XerJaxbCodec.JaxbPojoToXer(messageObjectToReturn);
			xerEncodedResponseMessageToReturn = XerJaxbCodec.createSelfClosingTags(xerEncodedResponseMessageToReturn);
		} catch (JAXBException e) {
			throw new ProcessingFailedException("Failed to create XER from POJO representation of reponse object");
		}
		// System.out.println(xerEncodedResponseMessageToReturn);

		logger.info("Encoding response message");
		// Encode Response
		Asn1Type asn1MessageType = Asn1Types.getAsn1TypeByName(messageObjectToReturn.getASN1MessageType());
		byte[] encodedMessageToReturn = null;

		try {
			encodedMessageToReturn = encodePayloadAs(xerEncodedResponseMessageToReturn, asn1MessageType);
		} catch (UnformattingFailedException | CodecFailedException | FormattingFailedException e) {
			throw new ProcessingFailedException("Response message failed to encode" + e.getMessage());
		}

		return encodedMessageToReturn;
	}

	/**
	 * Given a byte array representing an UPER encoded dialog message, create the
	 * proper response represented as a POJO
	 * 
	 * @param srcAddress
	 * @param payload
	 * @return DialogMessage POJO
	 * @throws ProcessingFailedException
	 */
	private DialogMessage createDialogMessageResponseObject(InetAddress srcAddress, byte[] payload)
			throws ProcessingFailedException {
		logger.info("Beginning message processing in dialogHandler");
		DialogMessage requestMessage;
		try {
			requestMessage = decodePayload(payload);
		} catch (DecodingFailedException ee) {
			throw new ProcessingFailedException("Message failed to decode, not an ASN1 encoded message");
		}

		if (requestMessage instanceof ServiceRequest) {
			// If the request is a ServiceRequest, start a session for the dialog
			int sourcePort = Integer.parseInt(((ServiceRequest) requestMessage).getDestination().getPort());
			InetSocketAddress source = new InetSocketAddress(srcAddress.getHostAddress(), sourcePort);
			logger.info(String.format(
					"Message was of type ServiceRequest. The destination for all messages in session %s "
							+ "will be: IP: %s, PORT: %s\n",
					requestMessage.getRequestID(), source.getAddress().getHostAddress(), sourcePort));

			this.sessionHandler.createSession(requestMessage.getRequestID(), source);
		}

		// Ask the message processor to figure the proper response
		// for the request
		logger.info(String.format("Determining Proper Response for received %s message\n",
				requestMessage.getASN1MessageType()));
		DialogMessage responseMessage = null;
		try {
			responseMessage = messageFactory.getResponseMessage(requestMessage, payload);
		} catch (NoSuchElementException | ProcessingFailedException e) {
			// Some failure in processing occurred, possibilities:
			// Message was not a ServiceRequest, DataRequest, or DataAcceptance
			// Mongo went down
			logger.error("Message failed processing.");
			throw new ProcessingFailedException(e.getMessage());
		}

		logger.info(String.format("Successfully created response message object of type: %s\n",
				responseMessage.getASN1MessageType()));

		return responseMessage;

	}

	/**
	 * Given a byte array representing an UPER encoded message, decode it, get the
	 * raw XML, and generate the corresponding POJO
	 * 
	 * @param byte[]
	 *            payload
	 * @throws DecodingFailedException
	 */
	private DialogMessage decodePayload(byte[] payload) throws DecodingFailedException {

		for (Asn1Type messageType : Asn1Types.getAllTypes()) {
			// We need to know the type of message when trying to decode
			// however given the raw UPER we have no idea
			// as such we try to decode the message as each type to see which works
			try {
				String decodedXer = decodePayloadAs(payload, messageType);
				DialogMessage unmarshalledObject = XerJaxbCodec.XerToJaxbPojo(decodedXer);
				return unmarshalledObject;
			} catch (CodecFailedException e) {
				// CodecFailed meaning the Asn1Type we were trying to decode the message as
				// IS NOT correct, try the next one!
			} catch (FormattingFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnformattingFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JAXBException e) {
				// JAXBException meaning there is not corresponding POJO defined for what
				// the message was decoded as. This is likely because the message decoded as
				// something unexpected
				logger.error("Unexpected unmarshalling", e);
			}

		}

		throw new DecodingFailedException("Message was not of an expected type");
	}

	/**
	 * Given raw XER encode it to UPER
	 * 
	 * @param xerToEncode
	 * @return UPER encoded byte array
	 * @throws FormattingFailedException
	 * @throws CodecFailedException
	 * @throws UnformattingFailedException
	 */
	private byte[] encodePayloadAs(String xerToEncode, Asn1Type messageType)

			throws UnformattingFailedException, CodecFailedException, FormattingFailedException {
		return PerXerCodec.xerToPer(messageType, xerToEncode, RawXerData.unformatter, RawPerData.formatter);

	}

	/**
	 * Helper function, given a per encoded byte array and a target type, decode the
	 * byte array into XER
	 * 
	 * @param payload
	 * @param type
	 * @return
	 * @throws CodecFailedException
	 * @throws FormattingFailedException
	 * @throws UnformattingFailedException
	 */
	private static String decodePayloadAs(byte[] payload, Asn1Type type)
			throws CodecFailedException, FormattingFailedException, UnformattingFailedException {

		return PerXerCodec.perToXer(type, payload, RawPerData.unformatter, RawXerData.formatter);

	}

	private SessionHandlerInterface sessionHandler = null;
	private MessageProcessor messageFactory;
	private final static Logger logger = Logger.getLogger(DialogHandler.class);
}
