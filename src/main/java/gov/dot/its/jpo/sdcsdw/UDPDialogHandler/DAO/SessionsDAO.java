package gov.dot.its.jpo.sdcsdw.UDPDialogHandler.DAO;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class SessionsDAO {

	public SessionsDAO(MongoDatabase mongo, String DbName, String collectionName) {
		// Initialize connection to backend DB
		this.collection = mongo.getCollection(collectionName);
	}

	public Document getSession(String sessionID) {
		return this.collection.find(Filters.eq("requestid", sessionID)).first();
	}

	public void insertSession(String sessionID, InetSocketAddress srcAddress) {
		logger.info(String.format("Create session for requestID %s with destination %s:%d\n", sessionID,
				srcAddress.getAddress().getHostAddress(), srcAddress.getPort()));

		Document doc = new Document("requestid", sessionID).append("sourceip", srcAddress.getAddress().getHostAddress())
				.append("sourceport", srcAddress.getPort());

		this.collection.insertOne(doc);
	}

	public void removeSession(String sessionID) {
		logger.info("Deleting session for requestID:" + sessionID);
		this.collection.deleteOne(Filters.eq("requestid", sessionID));
	}

	private MongoCollection<Document> collection;
	private final static Logger logger = Logger.getLogger(SessionsDAO.class);
}
