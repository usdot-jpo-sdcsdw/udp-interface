package gov.dot.its.jpo.sdcsdw.UDPDialogHandler.Model;

import java.net.InetSocketAddress;
import java.util.List;


public class Response {

	private byte[] responseMessage;
	private List<byte[]> responseList;
	private InetSocketAddress destination;
	private boolean containsMultiple;

	public Response(byte[] responseMessage, InetSocketAddress destination) {
		this.responseMessage=responseMessage;
		this.destination=destination;
		this.containsMultiple=false;
	}
	
	public boolean containsMultiple() {
		return this.containsMultiple;
	}
	
	public Response(List<byte[]> encodedResponses, InetSocketAddress destination) {
		this.responseList=encodedResponses;
		this.destination=destination;
		this.containsMultiple=true;
	}

	public List<byte[]> getResponseList(){
		return this.responseList;
	}
	
	public byte[] getResponseMessage() {
		return this.responseMessage;
	}
	
	public InetSocketAddress getDestination() {
		return this.destination;
	}
}