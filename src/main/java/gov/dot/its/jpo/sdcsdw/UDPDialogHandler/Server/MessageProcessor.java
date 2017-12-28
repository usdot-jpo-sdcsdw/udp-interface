package gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.MongoException;

import gov.dot.its.jpo.sdcsdw.MessageTypes.AdvisorySituationData;
import gov.dot.its.jpo.sdcsdw.MessageTypes.DataAcceptance;
import gov.dot.its.jpo.sdcsdw.MessageTypes.DataRequest;
import gov.dot.its.jpo.sdcsdw.MessageTypes.DialogID;
import gov.dot.its.jpo.sdcsdw.MessageTypes.DialogMessage;
import gov.dot.its.jpo.sdcsdw.MessageTypes.ServiceRequest;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.DAO.AsdDaoInterface;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Exception.ProcessingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.Asn1Types;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.PerXerCodec;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.CodecFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.FormattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.UnformattingFailedException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.per.RawPerData;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.xer.RawXerData;
import gov.dot.its.jpo.sdcsdw.xerjaxbcodec.XerJaxbCodec;

public class MessageProcessor {

	public MessageProcessor(AsdDaoInterface asdDAO, double NWCornerLatStr, double NWCornerLonStr, double SECornerLatStr,
			double SECornerLonStr) {

		try {
			this.digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			logger.error("Crashed while initialized MessageDigest", e);
		}

		this.asdDAO = asdDAO;

		setRegion(NWCornerLatStr, NWCornerLonStr, SECornerLatStr, SECornerLonStr);
	}

	public MessageProcessor(AsdDaoInterface asdDAO) {
		this(asdDAO, DEFALUT_NW_CNR_LATITUDE, DEFALUT_NW_CNR_LONGITUDE, DEFALUT_SE_CNR_LATITUDE,
				DEFALUT_SE_CNR_LONGITUDE);
	}

	public DialogMessage processMessage(DialogMessage abstractObject, byte[] originalBytePayload)
			throws NoSuchElementException, ProcessingFailedException {
		DialogMessage dialogMessageToReturnToHandler = null;

		// Each response, regardless of type should share the dialogID, groupID, and
		// requestID
		// of the request:
		DialogID dialogIDObj = abstractObject.getDialogID();
		String groupID = abstractObject.getGroupID();
		String requestID = abstractObject.getRequestID();

		// Possible Options:
		// (1) Get a Service Request: Create and return a ServiceResponse
		// (2) Get a Data Request:
		/*-
		 * Query Mongo
		 * Convert raw PER ASD to POJO
		 * Put each ASD POJO into a list
		 * Create an AdvisorySituationDataDistributionList, this is simply an object that stores a 
		 * 		AdvisorySituationDataDistributions. 
		*/
		// (3) Get a Data Acceptance: Create and return a DataReceipt

		// IF RECEIVED SERVICE REQUEST
		if (abstractObject instanceof ServiceRequest) {
			// One of the fields of ServiceResponse is a hash of the incoming service
			// request!
			String serviceRequestHash = Hex.encodeHexString(digest.digest(originalBytePayload)).toUpperCase();
			dialogMessageToReturnToHandler = MessageCreator.createServiceResponse(dialogIDObj, groupID, requestID,
					serviceRequestHash, this.NWCornerLat, this.NWCornerLon, this.SECornerLat, this.SECornerLon);
		} else if (abstractObject instanceof DataRequest) {
			List<Document> retrievedRecords;
			// Get data from Mongo
			try {
				retrievedRecords = this.asdDAO.getAllTIMData();
			} catch (MongoException e) {
				throw new ProcessingFailedException("Failed to retrieve data from mongo. Error: " + e.getMessage());
			}

			List<AdvisorySituationData> listOfASDS = createListOfASDS(retrievedRecords);
			dialogMessageToReturnToHandler = MessageCreator.createAdvisorySituationDataDistributionList(listOfASDS,
					dialogIDObj, groupID, requestID);

		} else if (abstractObject instanceof DataAcceptance) {
			// Return Data Receipt
			dialogMessageToReturnToHandler = MessageCreator.createDataReceipt(dialogIDObj, groupID, requestID);
		} else {
			throw new NoSuchElementException(
					"The message was not one of the expected message types: (ServiceRequest,DataRequest,DataAcceptance)");
		}
		return dialogMessageToReturnToHandler;
	}

	private void setRegion(double NWCornerLatStr, double NWCornerLonStr, double SECornerLatStr, double SECornerLonStr) {

		this.NWCornerLat = convertGeoCoordinateToInt(NWCornerLatStr);
		this.NWCornerLon = convertGeoCoordinateToInt(NWCornerLonStr);
		this.SECornerLat = convertGeoCoordinateToInt(SECornerLatStr);
		this.SECornerLon = convertGeoCoordinateToInt(SECornerLonStr);
	}

	private List<AdvisorySituationData> createListOfASDS(List<Document> retrievedRecords) {
		List<AdvisorySituationData> asdList = new ArrayList<AdvisorySituationData>();
		for (Document doc : retrievedRecords) {
			String hexEncodedASD = doc.getString("encodedMsg");
			byte[] hexEncodedASDAsByte = null;
			try {
				hexEncodedASDAsByte = Hex.decodeHex(hexEncodedASD);
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				logger.error("Got a bad message");
				continue;
			}
			// decode byte array to XML:
			try {
				String xerEncodedASD = (PerXerCodec.perToXer(Asn1Types.getAsn1TypeByName("AdvisorySituationData"),
						hexEncodedASDAsByte, RawPerData.unformatter, RawXerData.formatter));
				// Convert XML to POJO

				AdvisorySituationData asdObject = (AdvisorySituationData) XerJaxbCodec.XerToJaxbPojo(xerEncodedASD);
				asdList.add(asdObject);
			} catch (CodecFailedException | FormattingFailedException | UnformattingFailedException | JAXBException e) {
				// Going to ignore this message
				logger.error("Failed to decode and convert to POJO ASD retrieved from Mongo", e);
			}

		}
		return asdList;
	}

	/**
	 * Takes a Lat or Long as a double and converts to an int.
	 * 
	 * @param point
	 * @return
	 */
	private static String convertGeoCoordinateToInt(double point) {
		double convertedPoint = point * LAT_LONG_CONVERSION_FACTOR;
		int geoCoordAsInt = (int) Math.round(convertedPoint);
		return geoCoordAsInt + "";
	}

	private final static int LAT_LONG_CONVERSION_FACTOR = 10000000;
	private String NWCornerLat;
	private String NWCornerLon;
	private String SECornerLat;
	private String SECornerLon;
	private MessageDigest digest;
	static final double DEFALUT_NW_CNR_LATITUDE = 43.0;
	static final double DEFALUT_NW_CNR_LONGITUDE = -85.0;
	static final double DEFALUT_SE_CNR_LATITUDE = 41.0;
	static final double DEFALUT_SE_CNR_LONGITUDE = -82.0;
	private AsdDaoInterface asdDAO;
	private final static Logger logger = Logger.getLogger(MessageProcessor.class);

}
