package secs;

import secs.secs2.Secs2;

public abstract class SecsMessage {

	public SecsMessage() {
		/* Nothing */
	}
	
	abstract public int getStream();
	abstract public int getFunction();
	abstract public boolean wbit();
	abstract public Secs2 secs2();
	
	abstract public int deviceId();
	
	public int sessionId() {
		return deviceId();
	}
	
	abstract public byte[] header10Bytes();
	
	abstract protected Integer systemBytesKey();
	abstract protected String toHeaderBytesString();
	
}
