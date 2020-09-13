package com.shimizukenta.secs.hsmsss;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import com.shimizukenta.secs.AbstractSecsInnerEngine;
import com.shimizukenta.secs.SecsException;
import com.shimizukenta.secs.SecsSendMessageException;
import com.shimizukenta.secs.SecsWaitReplyMessageException;
import com.shimizukenta.secs.TimeProperty;
import com.shimizukenta.secs.secs2.Secs2BuildException;
import com.shimizukenta.secs.secs2.Secs2ByteBuffersBuilder;

public class HsmsSsSendReplyManager extends AbstractSecsInnerEngine {
	
	private final Collection<Pack> packs = new ArrayList<>();
	
	private final HsmsSsCommunicator parent;
	
	public HsmsSsSendReplyManager(HsmsSsCommunicator parent) {
		super(parent);
		this.parent = parent;
	}
	
	public Optional<HsmsSsMessage> send(HsmsSsMessage msg)
			throws SecsSendMessageException, SecsWaitReplyMessageException, SecsException
			, InterruptedException {
		
		AsynchronousSocketChannel channel = parent.optionalChannel()
				.orElseThrow(() -> new HsmsSsNotConnectedException(msg));
		
		HsmsSsMessageType type = HsmsSsMessageType.get(msg);
		
		switch ( type ) {
		case SELECT_REQ:
		case LINKTEST_REQ: {
			
			Pack p = entry(msg);
			
			try {
				send(channel, msg);
				
				try {
					return Optional.of(reply(p, parent.hsmsSsConfig().timeout().t6()));
				}
				catch ( TimeoutException e ) {
					throw new HsmsSsTimeoutT6Exception(msg, e);
				}
			}
			finally {
				remove(p);
			}
			/* break; */
		}
		case DATA: {
			
			if ( msg.wbit() ) {
				
				Pack p = entry(msg);
				
				try {
					send(channel, msg);
					
					try {
						return Optional.of(reply(p, parent.hsmsSsConfig().timeout().t3()));
					}
					catch ( TimeoutException e ) {
						throw new HsmsSsTimeoutT3Exception(msg, e);
					}
				}
				finally {
					remove(p);
				}
				
			} else {
				
				send(channel, msg);
				return Optional.empty();
			}
			/* break */
		}
		default: {
			
			send(channel, msg);
			return Optional.empty();
		}
		}
	}
	
	private static final long MAX_BUFFER_SIZE = 256L * 256L;
	
	protected long prototypeMaxBufferSize() {
		return MAX_BUFFER_SIZE;
	}
	
	public void send(AsynchronousSocketChannel channel, HsmsSsMessage msg)
			throws SecsSendMessageException, SecsException
			, InterruptedException {
		
		synchronized ( channel ) {
			
			try {
				Secs2ByteBuffersBuilder bb = Secs2ByteBuffersBuilder.build(1024, msg.secs2());
				
				long len = bb.size() + 10L;
				
				if ((len > 0x00000000FFFFFFFFL) || (len < 10L)) {
					throw new HsmsSsTooBigSendMessageException(msg);
				}
				
				notifyTrySendMessagePassThrough(msg);
				
				long bufferSize = len + 4L;
				
				if ( bufferSize > prototypeMaxBufferSize() ) {
					
					{
						ByteBuffer buffer = ByteBuffer.allocate(14);
						
						buffer.put((byte)(len >> 24));
						buffer.put((byte)(len >> 16));
						buffer.put((byte)(len >>  8));
						buffer.put((byte)(len      ));
						buffer.put(msg.header10Bytes());
						
						((Buffer)buffer).flip();
						
						send(channel, buffer);
					}
					
					for ( ByteBuffer buffer : bb.getByteBuffers() ) {
						send(channel, buffer);
					}
					
				} else {
					
					ByteBuffer buffer = ByteBuffer.allocate((int)bufferSize);
					
					buffer.put((byte)(len >> 24));
					buffer.put((byte)(len >> 16));
					buffer.put((byte)(len >>  8));
					buffer.put((byte)(len      ));
					buffer.put(msg.header10Bytes());
					
					for ( ByteBuffer bf : bb.getByteBuffers() ) {
						buffer.put(bf);
					}
					
					((Buffer)buffer).flip();
					
					send(channel, buffer);
				}
				
				notifySendedMessagePassThrough(msg);
				notifyLog("Sended HsmsSs-Message", msg);
			}
			catch ( ExecutionException e ) {
				
				Throwable t = e.getCause();
				
				if ( t instanceof RuntimeException ) {
					throw (RuntimeException)t;
				}
				
				throw new HsmsSsSendMessageException(msg, e);
			}
			catch ( Secs2BuildException | HsmsSsDetectTerminateException e ) {
				throw new HsmsSsSendMessageException(msg, e);
			}
		}
	}
	
	private void send(AsynchronousSocketChannel channel, ByteBuffer buffer)
			throws ExecutionException, HsmsSsDetectTerminateException, InterruptedException {
		
		while ( buffer.hasRemaining() ) {
			
			final Future<Integer> f = channel.write(buffer);
			
			try {
				int w = f.get().intValue();
				
				if ( w <= 0 ) {
					throw new HsmsSsDetectTerminateException();
				}
			}
			catch ( InterruptedException e ) {
				f.cancel(true);
				throw e;
			}
		}
	}
	
	private HsmsSsMessage reply(Pack p, TimeProperty timeout)
			throws SecsWaitReplyMessageException, SecsException
			, TimeoutException, InterruptedException {
		
		final Callable<HsmsSsMessage> getMsgTask = () -> {
			
			try {
				synchronized ( packs ) {
					for ( ;; ) {
						HsmsSsMessage m = p.replyMsg();
						if ( m != null ) {
							return m;
						}
						packs.wait();
					}
				}
			}
			catch ( InterruptedException ignroe ) {
			}
			
			return null;
		};
		
		final Callable<HsmsSsMessage> checkTerminateTask = () -> {
			
			try {
				synchronized ( packs ) {
					
					for ( ;; ) {
						
						if ( packs.isEmpty() ) {
							return null;
						}
						
						packs.wait();
					}
				}
			}
			catch ( InterruptedException ignore ) {
			}
			
			return null;
		};
		
		try {
			HsmsSsMessage msg = executeInvokeAny(getMsgTask, checkTerminateTask, timeout);
			
			if ( msg == null ) {
				throw new HsmsSsDetectTerminateException();
			}
			
			if ( HsmsSsMessageType.get(msg) == HsmsSsMessageType.REJECT_REQ ) {
				throw new HsmsSsRejectException(msg);
			}
			
			return msg;
		}
		catch ( ExecutionException e ) {
			
			Throwable t = e.getCause();
			
			if ( t instanceof RuntimeException ) {
				throw (RuntimeException)t;
			}
			
			throw new SecsException(e);
		}
	}
	
	public void clear() {
		synchronized ( packs ) {
			packs.clear();
			packs.notifyAll();
		}
	}
	
	private Pack entry(HsmsSsMessage msg) {
		synchronized ( packs ) {
			Pack p = new Pack(msg);
			packs.add(p);
			return p;
		}
	}
	
	private void remove(Pack p) {
		synchronized ( packs ) {
			packs.remove(p);
		}
	}
	
	public Optional<HsmsSsMessage> put(HsmsSsMessage msg) {
		
		final Integer key = msg.systemBytesKey();
		
		synchronized ( packs ) {
			
			for ( Pack p : packs ) {
				
				if ( p.key().equals(key) ) {
					
					p.put(msg);
					packs.notifyAll();
					return Optional.empty();
				}
			}
			
			return Optional.of(msg);
		}
	}
	
	private class Pack {
		
		private final HsmsSsMessage primary;
		private final Integer key;
		private HsmsSsMessage reply;
		
		public Pack(HsmsSsMessage primaryMsg) {
			this.primary = primaryMsg;
			this.key = primary.systemBytesKey();
			this.reply = null;
		}
		
		public Integer key() {
			return key;
		}
		
		public void put(HsmsSsMessage replyMsg) {
			synchronized ( this ) {
				this.reply = replyMsg;
			}
		}
		
		public HsmsSsMessage replyMsg() {
			synchronized ( this ) {
				return reply;
			}
		}
		
		@Override
		public int hashCode() {
			return key.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if ((o != null) && (o instanceof Pack)) {
				return ((Pack)o).key.equals(key);
			} else {
				return false;
			}
		}
	}

}
