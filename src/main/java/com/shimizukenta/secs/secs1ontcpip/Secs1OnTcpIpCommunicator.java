package com.shimizukenta.secs.secs1ontcpip;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.shimizukenta.secs.SecsException;
import com.shimizukenta.secs.SecsSendMessageException;
import com.shimizukenta.secs.secs1.Secs1Communicator;
import com.shimizukenta.secs.secs1.Secs1DetectTerminateException;
import com.shimizukenta.secs.secs1.Secs1SendMessageException;

public class Secs1OnTcpIpCommunicator extends Secs1Communicator {
	
	private final Secs1OnTcpIpCommunicatorConfig secs1OnTcpIpConfig;
	private AsynchronousSocketChannel channel;
	
	public Secs1OnTcpIpCommunicator(Secs1OnTcpIpCommunicatorConfig config) {
		super(Objects.requireNonNull(config));
		
		this.secs1OnTcpIpConfig = config;
		this.channel = null;
	}
	
	public static Secs1OnTcpIpCommunicator newInstance(Secs1OnTcpIpCommunicatorConfig config) {
		return new Secs1OnTcpIpCommunicator(config);
	}
	
	public static Secs1OnTcpIpCommunicator open(Secs1OnTcpIpCommunicatorConfig config) throws IOException {
		
		final Secs1OnTcpIpCommunicator inst = newInstance(config);
		
		try {
			inst.open();
		}
		catch ( IOException e ) {
			
			try {
				inst.close();
			}
			catch ( IOException giveup ) {
			}
			
			throw e;
		}
		
		return inst;
	}
	
	@Override
	public void open() throws IOException {
		super.open();
		executorService().execute(createTask());
	}
	
	@Override
	public void close() throws IOException {
		super.close();
	}
	
	private Runnable createTask() {
		
		return createLoopTask(() -> {
			
			try (
					AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
					) {
				
				SocketAddress socketAddr = secs1OnTcpIpConfig.socketAddress();
				
				String socketAddrString = socketAddr.toString();
				
				notifyLog("Secs1OnTcpIpCommunicator#try-connect", socketAddrString);
				
				ch.connect(socketAddr, null, new CompletionHandler<Void, Void>() {

					@Override
					public void completed(Void none, Void attachment) {
						
						try {
							synchronized ( Secs1OnTcpIpCommunicator.this ) {
								Secs1OnTcpIpCommunicator.this.channel = ch;
							}
							
							notifyCommunicatableStateChange(true);
							
							notifyLog("Secs1OnTcpIpCommunicator#connected", ch);
							
							final ByteBuffer buffer = ByteBuffer.allocate(1024);
							
							for ( ;; ) {
								
								((Buffer)buffer).clear();
								
								Future<Integer> f = ch.read(buffer);
								
								try {
									int r = f.get().intValue();
									
									if ( r < 0 ) {
										break;
									}
									
									((Buffer)buffer).flip();
									
									putByte(buffer);
								}
								catch ( InterruptedException e ) {
									f.cancel(true);
									throw e;
								}
							}
						}
						catch ( InterruptedException ignore) {
						}
						catch ( ExecutionException e ) {
							
							Throwable t = e.getCause();
							
							if ( t instanceof RuntimeException ) {
								throw (RuntimeException)t;
							}
							
							if ( ! (t instanceof AsynchronousCloseException) ) {
								notifyLog(e);
							}
						}
						finally {
							
							notifyCommunicatableStateChange(false);
							
							synchronized ( Secs1OnTcpIpCommunicator.this ) {
								Secs1OnTcpIpCommunicator.this.channel = null;
							}
							
							try {
								ch.shutdownOutput();
							}
							catch ( IOException giveup ) {
							}
							
							notifyLog("Secs1OnTcpIpCommunicator#closed", socketAddrString);
							
							synchronized ( ch ) {
								ch.notifyAll();
							}
						}
					}
					
					@Override
					public void failed(Throwable t, Void attachment) {
						
						try {
							if ( t instanceof RuntimeException ) {
								throw (RuntimeException)t;
							}
							
							notifyLog(t);
						}
						finally {
							
							synchronized ( ch ) {
								ch.notifyAll();
							}
						}
					}
				});
				
				synchronized ( ch ) {
					ch.wait();
				}
			}
			catch ( IOException e ) {
				notifyLog(e);
			}
			
			{
				long t = (long)(secs1OnTcpIpConfig.reconnectSeconds() * 1000.0F);
				if ( t > 0 ) {
					TimeUnit.MILLISECONDS.sleep(t);
				}
			}
		});
	}
	
	
	private final BlockingQueue<Byte> byteQueue = new LinkedBlockingQueue<>();
	
	protected void putByte(ByteBuffer buffer) throws InterruptedException {
		while ( buffer.hasRemaining() ) {
			byteQueue.put(buffer.get());
		}
	}
	
	@Override
	protected Optional<Byte> pollByte() {
		Byte b = byteQueue.poll();
		return b == null ? Optional.empty() : Optional.of(b);
	}

	@Override
	protected Optional<Byte> pollByte(long timeout, TimeUnit unit) throws InterruptedException {
		Byte b = byteQueue.poll(timeout, unit);
		return b == null ? Optional.empty() : Optional.of(b);
	}
	
	@Override
	protected Optional<Byte> pollByte(byte[] request) throws InterruptedException {
		
		try {
			
			Byte b = executorService().invokeAny(
					Arrays.asList(createPollByteTask(request))
					);
			
			if ( b != null ) {
				return Optional.of(b);
			}
		}
		catch ( ExecutionException e ) {
			
			Throwable t = e.getCause();
			
			if ( t instanceof RuntimeException ) {
				throw (RuntimeException)t;
			}
			
			notifyLog(e);
		}
		
		return Optional.empty();
	}
	
	@Override
	protected Optional<Byte> pollByte(byte[] request, long timeout, TimeUnit unit) throws InterruptedException {
		
		try {
			
			Byte b = executorService().invokeAny(
					Arrays.asList(createPollByteTask(request)),
					timeout,unit);
			
			if ( b != null ) {
				return Optional.of(b);
			}
		}
		catch ( TimeoutException timeup ) {
		}
		catch ( ExecutionException e ) {
			
			Throwable t = e.getCause();
			
			if ( t instanceof RuntimeException ) {
				throw (RuntimeException)t;
			}
			
			notifyLog(e);
		}
		
		return Optional.empty();
	}
	
	private Callable<Byte> createPollByteTask(byte[] request) {
		
		return new Callable<Byte>() {
			
			@Override
			public Byte call() {
				try {
					
					for ( ;; ) {
						
						byte b = byteQueue.take();
						
						for ( byte r : request ) {
							if ( r == b ) {
								return Byte.valueOf(b);
							}
						}
					}
				}
				catch ( InterruptedException ignore ) {
				}
				
				return null;
			}
		};
	}
	
	@Override
	protected void pollByteUntilEmpty() {
		byteQueue.clear();
	}
	
	@Override
	protected void sendByte(byte[] bs)
			throws SecsSendMessageException, SecsException, InterruptedException {
		
		final AsynchronousSocketChannel ch;
		
		synchronized ( this ) {
			ch = channel;
		}
		
		if ( ch == null ) {
			throw new Secs1OnTcpIpNotConnectedException();
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(bs.length);
		buffer.put(bs);
		((Buffer)buffer).flip();
		
		while ( buffer.hasRemaining() ) {
			
			Future<Integer> f = ch.write(buffer);
			
			try {
				int w = f.get().intValue();
				
				if ( w <= 0 ) {
					throw new Secs1DetectTerminateException();
				}
			}
			catch ( ExecutionException e ) {
				
				Throwable t = e.getCause();
				
				if ( t instanceof RuntimeException ) {
					throw (RuntimeException)t;
				}
				
				throw new Secs1SendMessageException(e);
			}
			catch ( InterruptedException e ) {
				f.cancel(true);
				throw e;
			}
		}
	}

}
