package com.shimizukenta.secs.secs1;

import java.util.Optional;

import com.shimizukenta.secs.SecsCommunicator;
import com.shimizukenta.secs.SecsException;
import com.shimizukenta.secs.SecsSendMessageException;
import com.shimizukenta.secs.SecsWaitReplyMessageException;
import com.shimizukenta.secs.secs2.Secs2;

/**
 * This interface is implementation of SECS-I (SEMI-E4).
 * 
 * @author kenta-shimizu
 *
 */
public interface Secs1Communicator extends SecsCommunicator {
	
	/**
	 * Create header-only Sesc1Message.
	 * 
	 * @param header header-10-bytes
	 * @return Secs1Message
	 */
	public Secs1Message createSecs1Message(byte[] header);
	
	/**
	 * Create Secs1Message.
	 * 
	 * @param header header-10-bytes
	 * @param body SECS-II data
	 * @return Secs1Message
	 */
	public Secs1Message createSecs1Message(byte[] header, Secs2 body);
	
	/**
	 * Send SECS-I-Message.
	 * 
	 * <p>
	 * Send Primary-Secs1Message,<br />
	 * Blocking-method.<br />
	 * Wait until sended Primay-Message and Reply-Secs1Message received if exist.
	 * </p>
	 * 
	 * @param msg
	 * @return reply-Secs1Message if exist
	 * @throws SecsSendMessageException
	 * @throws SecsWaitReplyMessageException
	 * @throws SecsException
	 * @throws InterruptedException
	 */
	public Optional<Secs1Message> send(Secs1Message msg)
			throws SecsSendMessageException,
			SecsWaitReplyMessageException,
			SecsException,
			InterruptedException;

}
