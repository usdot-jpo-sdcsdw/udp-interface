package gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import gov.dot.its.jpo.sdcsdw.MessageTypes.AdvisoryBroadcast;
import gov.dot.its.jpo.sdcsdw.MessageTypes.AdvisorySituationBundle;
import gov.dot.its.jpo.sdcsdw.MessageTypes.AdvisorySituationData;
import gov.dot.its.jpo.sdcsdw.MessageTypes.AdvisorySituationDataDistribution;
import gov.dot.its.jpo.sdcsdw.MessageTypes.AdvisorySituationDataDistributionList;
import gov.dot.its.jpo.sdcsdw.MessageTypes.AsdBundles;
import gov.dot.its.jpo.sdcsdw.MessageTypes.AsdRecords;
import gov.dot.its.jpo.sdcsdw.MessageTypes.AsdmType;
import gov.dot.its.jpo.sdcsdw.MessageTypes.BiDeliveryStart;
import gov.dot.its.jpo.sdcsdw.MessageTypes.BiDeliveryStop;
import gov.dot.its.jpo.sdcsdw.MessageTypes.BiEncryption;
import gov.dot.its.jpo.sdcsdw.MessageTypes.BiSignature;
import gov.dot.its.jpo.sdcsdw.MessageTypes.BiTxChannel;
import gov.dot.its.jpo.sdcsdw.MessageTypes.BiTxMode;
import gov.dot.its.jpo.sdcsdw.MessageTypes.BiType;
import gov.dot.its.jpo.sdcsdw.MessageTypes.BroadcastInst;
import gov.dot.its.jpo.sdcsdw.MessageTypes.DataReceipt;
import gov.dot.its.jpo.sdcsdw.MessageTypes.DialogID;
import gov.dot.its.jpo.sdcsdw.MessageTypes.DsrcInst;
import gov.dot.its.jpo.sdcsdw.MessageTypes.Expiration;
import gov.dot.its.jpo.sdcsdw.MessageTypes.NwCorner;
import gov.dot.its.jpo.sdcsdw.MessageTypes.SeCorner;
import gov.dot.its.jpo.sdcsdw.MessageTypes.SeqID;
import gov.dot.its.jpo.sdcsdw.MessageTypes.ServiceRegion;
import gov.dot.its.jpo.sdcsdw.MessageTypes.ServiceResponse;
import gov.dot.its.jpo.sdcsdw.MessageTypes.StartTime;
import gov.dot.its.jpo.sdcsdw.MessageTypes.StopTime;

/**
 * Creates response messages, implements bundling
 * 
 * @author mna30547
 *
 */
public class MessageCreator {

	public static ServiceResponse createServiceResponse(DialogID dialogIDObj, String groupID, String requestID,
			String serviceRequestHash, String nwCornerLat, String nwCornerLon, String seCornerLat,
			String seCornerLong) {
		logger.info("Creating Service Respones");
		ServiceResponse serviceResponse = new ServiceResponse();
		serviceResponse.setDialogID(dialogIDObj);
		serviceResponse.setGroupID(groupID);
		serviceResponse.setRequestID(requestID);

		// Set Sequence ID to service response

		SeqID seqID = new SeqID();
		seqID.setSvcResp("");
		serviceResponse.setSeqID(seqID);

		Expiration expiration = createExpiration(1);
		serviceResponse.setExpiration(expiration);

		ServiceRegion serviceRegion = createServiceRegion(nwCornerLat, nwCornerLon, seCornerLat, seCornerLong);

		serviceResponse.setServiceRegion(serviceRegion);

		serviceResponse.setHash(serviceRequestHash);

		return serviceResponse;
	}

	public static AdvisorySituationDataDistributionList createAdvisorySituationDataDistributionList(
			List<AdvisorySituationData> asdList, DialogID dialogIDObj, String groupID, String requestID) {
		// Given a list of POJO ASDS
		// DECODE THEM, CREATE ADVISORY BROADCASTS, PACKAGE!
		logger.info("Creating Advisory Situation Data Distribution List");
		List<AdvisoryBroadcast> broadcasts = extractTimsAndGenerateBroadcasts(asdList);
		List<AdvisoryBroadcast> record = new ArrayList<AdvisoryBroadcast>();
		List<AdvisorySituationBundle> bundles = new ArrayList<AdvisorySituationBundle>();

		List<AdvisorySituationDataDistribution> distributionList = new ArrayList<AdvisorySituationDataDistribution>();
		SeqID seqID = new SeqID();
		seqID.setData("");

		int bundlesMade = 0;
		int recordsMade = 0;
		int recordsMadeInThisBundle = 0;
		for (int i = 0; i < asdList.size(); i++) {
			record.add(broadcasts.get(i));
			recordsMade++;
			recordsMadeInThisBundle++;
			if (recordsMadeInThisBundle == MAX_BROADCASTS_PER_RECORD) {
				logger.info("GOT 10 RECORDS, THATS A FULL BUNDLE");
				bundlesMade++;
				AsdRecords asdRecords = new AsdRecords();
				asdRecords.setAdvisoryBroadcast(record.toArray(new AdvisoryBroadcast[record.size()]));
				AdvisorySituationBundle bundle = new AdvisorySituationBundle();
				bundle.setBundleNumber(Integer.toString(bundlesMade));

				bundle.setBundleId(String.format("%08X", bundleIdGenerator.nextInt()));
				bundle.setAsdRecords(asdRecords);
				bundles.add(bundle);

				record = new ArrayList<AdvisoryBroadcast>();
				recordsMadeInThisBundle = 0;

				if (bundlesMade == MAX_BUNDLES_PER_DISTRIBUTION) {
					// Can't make any more bundles in this distribution!
					logger.info("REACHED LIMIT OF BUNDLES PER DISTRIBUTION");
					AdvisorySituationDataDistribution advSitDataDist = createDistribution(bundles, bundlesMade,
							dialogIDObj, groupID, requestID, recordsMade, seqID);
					distributionList.add(advSitDataDist);

					recordsMade = 0;
					bundlesMade = 0;
					bundles = new ArrayList<AdvisorySituationBundle>();

				}

			}

		}

		if (record.size() > 0) {
			logger.info("HAVE SOME RECORDS LEFT OVER ADD A BUNDLE");
			// we have some records that don't make a complete bundle by themselves
			bundlesMade++;
			AsdRecords asdRecords = new AsdRecords();
			asdRecords.setAdvisoryBroadcast(record.toArray(new AdvisoryBroadcast[record.size()]));
			AdvisorySituationBundle bundle = new AdvisorySituationBundle();
			bundle.setBundleNumber(Integer.toString(bundlesMade));
			bundle.setBundleId(String.format("%08X", bundleIdGenerator.nextInt()));
			bundle.setAsdRecords(asdRecords);
			bundles.add(bundle);

		}

		if (bundles.size() > 0) {
			// we have some bundles that didn't make it into a distribution!
			AdvisorySituationDataDistribution advSitDataDist = createDistribution(bundles, bundlesMade, dialogIDObj,
					groupID, requestID, recordsMade, seqID);
			distributionList.add(advSitDataDist);

			recordsMade = 0;
			bundlesMade = 0;
			bundles = new ArrayList<AdvisorySituationBundle>();
		}

		if (distributionList.size() == 0) {
			distributionList.add(createDistribution(bundles, bundlesMade, dialogIDObj, groupID, requestID,
					recordsMadeInThisBundle, seqID));
		}

		AdvisorySituationDataDistributionList responseDistributionList = new AdvisorySituationDataDistributionList(
				distributionList);
		responseDistributionList.setRequestID(requestID);
		return responseDistributionList;
	}

	private static AdvisorySituationDataDistribution createDistribution(List<AdvisorySituationBundle> bundles,
			int bundlesMade, DialogID dialogIDObj, String groupID, String requestID, int recordsMade, SeqID seqID) {
		AdvisorySituationDataDistribution advSitDataDist = new AdvisorySituationDataDistribution();
		AsdBundles asdBundles = new AsdBundles();
		asdBundles.setAdvisorySituationBundle(bundles.toArray(new AdvisorySituationBundle[bundles.size()]));
		advSitDataDist.setAsdBundles(asdBundles);
		advSitDataDist.setBundleCount(Integer.toString(bundlesMade));
		advSitDataDist.setDialogID(dialogIDObj);
		advSitDataDist.setGroupID(groupID);
		advSitDataDist.setRequestID(requestID);
		advSitDataDist.setRecordCount(Integer.toString(recordsMade));
		advSitDataDist.setSeqID(seqID);
		return advSitDataDist;
	}

	public static DataReceipt createDataReceipt(DialogID dialogIDObj, String groupID, String requestID) {
		logger.info("Creating Data Receipt");
		DataReceipt dataReceipt = new DataReceipt();
		dataReceipt.setDialogID(dialogIDObj);
		dataReceipt.setGroupID(groupID);
		dataReceipt.setRequestID(requestID);

		SeqID seqID = new SeqID();
		seqID.setReceipt("");
		dataReceipt.setSeqID(seqID);

		return dataReceipt;
	}

	private static List<AdvisoryBroadcast> extractTimsAndGenerateBroadcasts(List<AdvisorySituationData> asdList) {
		// Given a byte array
		List<AdvisoryBroadcast> broadcasts = new ArrayList<AdvisoryBroadcast>();
		for (AdvisorySituationData asd : asdList) {

			AdvisoryBroadcast broadcastMessage = new AdvisoryBroadcast();
			String tim = asd.getAsdmDetails().getAdvisoryMessage();

			broadcastMessage.setMessagePsid(String.format("%08X", PSID));

			BroadcastInst broadcastInst = createBroadcastInstructions(asd.getAsdmDetails().getStartTime(),
					asd.getAsdmDetails().getStopTime(), asd.getAsdmDetails().getAsdmType());

			broadcastMessage.setBroadcastInst(broadcastInst);
			broadcastMessage.setAdvisoryMessage(tim);

			broadcasts.add(broadcastMessage);
		}

		return broadcasts;
	}

	private static BroadcastInst createBroadcastInstructions(StartTime startTime, StopTime stopTime, AsdmType type) {
		BroadcastInst bcastInst = new BroadcastInst();

		BiDeliveryStart biDeliveryStart = new BiDeliveryStart();
		BiDeliveryStop biDeliveryStop = new BiDeliveryStop();

		
		if (startTime != null && stopTime != null) {
			
			biDeliveryStart.fillAllFields(startTime);
			biDeliveryStop.fillAllFields(stopTime);
		}

		else {
			// if start time and stop time were NOT defined, we've gotta do it ourselves!
			Calendar now = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
			biDeliveryStart.setDay(Integer.toString(now.get(Calendar.DAY_OF_MONTH)));
			biDeliveryStart.setHour(Integer.toString(now.get(Calendar.HOUR_OF_DAY)));
			biDeliveryStart.setMinute(Integer.toString(now.get(Calendar.MINUTE)));
			biDeliveryStart.setMonth(Integer.toString(now.get(Calendar.MONTH)));
			biDeliveryStart.setYear(Integer.toString(now.get(Calendar.YEAR)));

			biDeliveryStop.setDay(Integer.toString(now.get(Calendar.DAY_OF_MONTH) + 7));
			biDeliveryStop.setHour(Integer.toString(now.get(Calendar.HOUR_OF_DAY)));
			biDeliveryStop.setMinute(Integer.toString(now.get(Calendar.MINUTE)));
			biDeliveryStop.setMonth(Integer.toString(now.get(Calendar.MONTH)));
			biDeliveryStop.setYear(Integer.toString(now.get(Calendar.YEAR)));

		}

		bcastInst.setBiDeliveryStart(biDeliveryStart);
		bcastInst.setBiDeliveryStop(biDeliveryStop);
		BiEncryption biEncript = new BiEncryption();
		if (ENCRYPTION) {
			biEncript.setTrue("");
		} else {
			biEncript.setFalse("");
		}

		bcastInst.setBiEncryption(biEncript);

		bcastInst.setBiPriority(Integer.toString(PRIORITY));

		BiSignature biSign = new BiSignature();
		if (SIGNATURE) {
			biSign.setTrue("");
		} else {
			biSign.setFalse("");
		}
		bcastInst.setBiSignature(biSign);

		BiType biType = new BiType();
		biType.setValuesFromStartTimeObject(type);
		bcastInst.setBiType(biType);

		DsrcInst dsrcInst = new DsrcInst();
		BiTxChannel biTxChannel = new BiTxChannel();
		biTxChannel.setChannel((int) TX_CHANNEL);
		dsrcInst.setBiTxChannel(biTxChannel);

		dsrcInst.setBiTxInterval(Integer.toString(TX_INTERVAL));

		BiTxMode biTxMode = new BiTxMode();
		biTxMode.setMode((int) TX_MODE);
		dsrcInst.setBiTxMode(biTxMode);

		bcastInst.setDsrcInst(dsrcInst);

		return bcastInst;
	}

	private static ServiceRegion createServiceRegion(String nwCornerLat, String nwCornerLon, String seCornerLat,
			String seCornerLong) {
		// ServiceRegion
		NwCorner nwCorner = new NwCorner();
		nwCorner.setLat(nwCornerLat);
		nwCorner.setLong(nwCornerLon);

		SeCorner seCorner = new SeCorner();
		seCorner.setLat(seCornerLat);
		seCorner.setLong(seCornerLong);

		ServiceRegion serviceRegion = new ServiceRegion();
		serviceRegion.setNwCorner(nwCorner);
		serviceRegion.setSeCorner(seCorner);

		return serviceRegion;
	}

	private static Expiration createExpiration(int expireInMin) {
		// Setting Expiration to be in expireInMin minutes
		Expiration expiration = new Expiration();
		Calendar now = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
		now.add(Calendar.MINUTE, expireInMin);
		expiration.setYear(now.get(Calendar.YEAR));
		expiration.setMonth(now.get(Calendar.MONTH) + 1);
		expiration.setDay(now.get(Calendar.DAY_OF_MONTH));
		expiration.setHour(now.get(Calendar.HOUR_OF_DAY));
		expiration.setMinute((now.get(Calendar.MINUTE)));
		expiration.setSecond((now.get(Calendar.SECOND)));
		expiration.setOffset(0);
		return expiration;
	}

	private static Random bundleIdGenerator = new Random();
	// Default values for the RSU broadcast instructions for May PlugFest
	private static final int PSID = 0x8003;
	private static final int PRIORITY = 32;
	private static final long TX_MODE = 1;
	private static final long TX_CHANNEL = 5;
	private static final int TX_INTERVAL = 1;
	private static final boolean SIGNATURE = true;
	private static final boolean ENCRYPTION = false;
	private static final int ONE_DAY = 1000 * 60 * 60 * 24;
	public static final int MAX_BROADCASTS_PER_RECORD = 10;
	public static final int MAX_BUNDLES_PER_DISTRIBUTION = 4;
	private static final String UTC_TIMEZONE = "UTC";
	private final static Logger logger = Logger.getLogger(MessageCreator.class);
}
