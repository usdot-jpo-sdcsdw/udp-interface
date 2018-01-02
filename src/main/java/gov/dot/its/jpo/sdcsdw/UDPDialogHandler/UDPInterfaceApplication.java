package gov.dot.its.jpo.sdcsdw.UDPDialogHandler;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.DAO.ASDDAO;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.DAO.MockASDDAO;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.DAO.SessionsDAO;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Server.DialogHandler;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Server.DialogMessageFactory;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Server.UDPDialogServer;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Session.LocalSessionHandler;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Session.DistributedSessionHandler;
import gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Session.SessionHandlerInterface;

/**
 * Entry Point
 *
 */
public class UDPInterfaceApplication {
	public static void main(String[] args) {

		// LOAD ALL DEPENDENCIES:

		Properties props = readProperties("./config/settings.properties");

		String debug = props.getProperty("Debug");
		String ingressPortStr = props.getProperty("IngressPort");

		if (debug == null || ingressPortStr == null) {
			logger.error("Failed to initialize application. Expected configuration values:"
					+ "Debug: True/False, IngressPort: int value of port to listen on");
			return;
		}

		int ingressPort;
		try {
			ingressPort = Integer.parseInt(ingressPortStr);
		} catch (NumberFormatException e) {
			logger.error("Failed to initialize application. Expected configuration values are:\n"
					+ "IngressPort: INT value of port to listen on");
			return;
		}

		SessionHandlerInterface sessionHandler;
		DialogMessageFactory messageProcessor;

		if (debug.equals("True")) {
			sessionHandler = new LocalSessionHandler();
			messageProcessor = new DialogMessageFactory(new MockASDDAO());

		} else {
			// READ ALL PROPERTIES AND CONFIRM THEY EXIST
			String MongoIP = props.getProperty("MongoIP");
			String MongoPortStr = props.getProperty("MongoPort");
			String MongoDBName = props.getProperty("MongoDBName");
			String SessionsCollectionName = props.getProperty("MongoSessionsCollectionName");
			String TIMSCollectionName = props.getProperty("MongoTIMSCollectionName");
			String NWCornerLatStr = props.getProperty("NWCornerLat");
			String NWCornerLonStr = props.getProperty("NWCornerLon");
			String SECornerLatStr = props.getProperty("SECornerLat");
			String SECornerLonStr = props.getProperty("SECornerLon");

			if (MongoIP == null || MongoPortStr == null || MongoDBName == null | SessionsCollectionName == null
					|| TIMSCollectionName == null || NWCornerLatStr == null || NWCornerLonStr == null
					|| SECornerLatStr == null || SECornerLonStr == null) {
				logger.error(
						"Failed to initialize application. When Debug is False, the expected configuration values are:\n"
								+ "MongoIP: Host Address of Mongo DB to connect to\n"
								+ "MongoPort: int value of port Mongo instance is on\n"
								+ "MongoDBName: Name of mongo database\n"
								+ "MongoSessionsCollectionName: Name of sessions collection\n"
								+ "MongoTIMSCollectionName: Name of TIMS collection\n"
								+ "NWCornerLat: NW Lat as a double\n" + "NWCornerLon:NW Lon as a double\n"
								+ "SECornerLat: SE Lat as a double\n" + "SECornerLon: SE Lon as a double\n");
				return;
			}

			int MongoPort;
			double NWCornerLat;
			double NWCornerLon;
			double SECornerLat;
			double SECornerLon;
			try {
				// CONVERT STRING VALUES TO INT/DOUBLE
				NWCornerLat = Double.parseDouble(NWCornerLatStr);
				NWCornerLon = Double.parseDouble(NWCornerLonStr);
				SECornerLat = Double.parseDouble(SECornerLatStr);
				SECornerLon = Double.parseDouble(SECornerLonStr);
				MongoPort = Integer.parseInt(MongoPortStr);
			} catch (NumberFormatException e) {
				logger.error(
						"Failed to initialize application. When Debug is False, the expected configuration values are:\n"
								+ "MongoPort: INT value of port Mongo instance is on\n"
								+ "NWCornerLat: NW Lat as a DOUBLE\n" + "NWCornerLon:NW Lon as a DOUBLE\n"
								+ "SECornerLat: SE Lat as a DOUBLE\n" + "SECornerLon: SE Lon as a DOUBLE\n");
				return;
			}

			// Initialize MONGO Connection:
			MongoDatabase mongo = initializeDatabaseConnection(MongoIP, MongoPort, MongoDBName);

			// Initialize Sessions DAO which the session handler will use to get sessions
			// data
			SessionsDAO sessionsDAO = new SessionsDAO(mongo, MongoDBName, SessionsCollectionName);

			// Initialize Session Handler
			sessionHandler = new DistributedSessionHandler(sessionsDAO);

			// Initialize ASD DAO which the message processor will use to get data from the
			// mongo DB
			ASDDAO asdDAO = new ASDDAO(mongo, MongoDBName, TIMSCollectionName);

			// Initialize Message Processor
			messageProcessor = new DialogMessageFactory(asdDAO, NWCornerLat, NWCornerLon, SECornerLat, SECornerLon);
		}

		// Initialize Dialog Handler
		DialogHandler dialogHandler = new DialogHandler(sessionHandler, messageProcessor);

		logger.info("Staring UDP Dialog Listener/Handler Application");
		UDPDialogServer udpForwarder = new UDPDialogServer(dialogHandler, ingressPort);
		Thread listenerThread = new Thread(udpForwarder);
		listenerThread.start();
	}

	private static MongoDatabase initializeDatabaseConnection(String MongoIP, int MongoPort, String MongoDBName) {
		logger.info("Initializing DB Connection");
		MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();
		optionsBuilder.maxConnectionIdleTime(25000);
		MongoClientOptions options = optionsBuilder.build();

		MongoClient mongo = new MongoClient(new ServerAddress(MongoIP, MongoPort), options);
		return mongo.getDatabase(MongoDBName);

	}

	private static Properties readProperties(String pathToProperties) {
		Properties props = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(pathToProperties);
		} catch (FileNotFoundException e1) {
			logger.error("Failed to read settings.properties file, expected file at /config/settings.properties", e1);
			return null;
		}

		try {
			props.load(input);
		} catch (IOException e1) {
			logger.error("Failed to load properties from settings.properties", e1);
			return null;
		}

		return props;
	}

	private final static Logger logger = Logger.getLogger(UDPInterfaceApplication.class);
	
}
