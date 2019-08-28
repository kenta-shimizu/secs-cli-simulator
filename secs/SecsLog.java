package secs;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class SecsLog {
	
	private static final String SUBJECT_OF_THROWABLE = "Throwable";
	
	private final String subject;
	private final LocalDateTime timestamp;
	private final Object value;
	
	public SecsLog(CharSequence subject, LocalDateTime timestamp, Object value) {
		this.subject = subject.toString();
		this.timestamp = timestamp;
		this.value = value;
	}
	
	public SecsLog(CharSequence subject, Object value) {
		this(subject, LocalDateTime.now(), value);
	}
	
	public SecsLog(CharSequence subject, LocalDateTime timestamp) {
		this(subject, timestamp, null);
	}
	
	public SecsLog(CharSequence subject) {
		this(subject, LocalDateTime.now(), null);
	}
	
	public SecsLog(Throwable t) {
		this(SUBJECT_OF_THROWABLE, LocalDateTime.now(), t);
	}
	
	public String subject() {
		return this.subject;
	}
	
	public LocalDateTime timestamp() {
		return this.timestamp;
	}
	
	public Optional<Object> value() {
		return this.value == null ? Optional.empty() : Optional.of(this.value);
	}
	
	
	private static final String BR = System.lineSeparator();
	private static final String SPACE = "\t";
	private static DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder(toStringTimestamp())
				.append(SPACE)
				.append(subject());
		
		toStringValue().ifPresent(v -> {
			sb.append(BR).append(v);
		});
		
		return sb.toString();
	}
	
	private String toStringTimestamp() {
		return timestamp.format(DATETIME);
	}
	
	private Optional<String> toStringValue() {
		
		return value().map(o -> {
			
			if ( o instanceof Throwable ) {
				
				try (
						StringWriter sw = new StringWriter();
						) {
					
					try (
							PrintWriter pw = new PrintWriter(sw);
							) {
						
						((Throwable) o).printStackTrace(pw);
						pw.flush();
						
						return sw.toString();
					}
				}
				catch ( IOException e ) {
					return "THROWABLE-PARSE-FAILED";
				}
				
			} else {
				
				return o.toString();
			}
			
		});
	}
	
}
