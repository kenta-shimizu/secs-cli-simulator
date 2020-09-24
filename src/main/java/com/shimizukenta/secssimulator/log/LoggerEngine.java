package com.shimizukenta.secssimulator.log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import com.shimizukenta.secs.BooleanProperty;
import com.shimizukenta.secs.ReadOnlyBooleanProperty;
import com.shimizukenta.secssimulator.AbstractSecsSimulator;
import com.shimizukenta.secssimulator.AbstractSecsSimulatorEngine;

public class LoggerEngine extends AbstractSecsSimulatorEngine {
	
	
	private final BooleanProperty logging = BooleanProperty.newInstance(false);
	
	public LoggerEngine(AbstractSecsSimulator engine) {
		super(engine);
	}
	
	public ReadOnlyBooleanProperty logging() {
		return logging;
	}
	
	private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
	
	public void putLog(Object o) {
		if ( logging.booleanValue() ) {
			queue.offer(o);
		}
	}
	
	private final Object syncLogging = new Object();
	
	public void start(Path path) {
		synchronized ( this ) {
			stop();
			
			logging.waitUntilFalse();
			
			executorService().execute(() -> {
				
				Collection<Callable<Void>> tasks = Arrays.asList(
						() -> {
							try {
								synchronized ( syncLogging ) {
									syncLogging.wait();
								}
							}
							catch ( InterruptedException ignore ) {
							}
							
							return null;
						},
						() -> {
							try {
								try (
										BufferedWriter bw = Files.newBufferedWriter(
												path,
												StandardCharsets.UTF_8,
												StandardOpenOption.WRITE,
												StandardOpenOption.CREATE,
												StandardOpenOption.APPEND);
										) {
									
									logging.set(true);
									
									for ( ;; ) {
										Object o = queue.take();
										
										bw.write(o.toString());
										bw.newLine();
										bw.newLine();
										
										bw.flush();
									}
								}
								catch ( IOException e ) {
									notifyLog(e);
								}
							}
							catch ( InterruptedException ignore ) {
							}
							
							return null;
						});
				
				try {
					queue.clear();
					
					executorService().invokeAny(tasks);
				}
				catch ( ExecutionException e ) {
					
					Throwable t = e.getCause();
					
					if ( t instanceof RuntimeException ) {
						throw (RuntimeException)t;
					}
					
					if ( t instanceof Error ) {
						throw (Error)t;
					}
					
					notifyLog(t);
				}
				catch ( InterruptedException ignore ) {
				}
				finally {
					logging.set(false);
				}
			});
		}
	}
	
	public void stop() {
		synchronized ( syncLogging ) {
			syncLogging.notifyAll();
		}
	}
	
}
