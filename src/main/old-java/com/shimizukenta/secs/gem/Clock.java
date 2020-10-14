package com.shimizukenta.secs.gem;

import java.time.LocalDateTime;

import com.shimizukenta.secs.secs2.Secs2;
import com.shimizukenta.secs.secs2.Secs2Exception;

/**
 * This interface is implementation of Clock in GEM (SEMI-E30).
 * 
 * <p>
 * Relates: S2F17, S2F18, S2F31<br />
 * </p>
 * <p>
 * Instances of this class are immutable.<br />
 * </p>
 * 
 * @author kenta-shimizu
 *
 */
public interface Clock {
	
	/**
	 * Returns new Clock instance from LocalDateTime.
	 * 
	 * @param ldt LocalDateTime
	 * @return Clock-of-LocalDateTime
	 */
	public static Clock from(LocalDateTime ldt) {
		return AbstractClock.from(ldt);
	}
	
	/**
	 * Returns new Clock instance of now.
	 * 
	 * @return Clock-of-Now
	 */
	public static Clock now() {
		return AbstractClock.now();
	}
	
	/**
	 * Returns new Clock instance from Secs2.
	 * 
	 * <p>
	 * use for Secs2 of S2F18, S2F31<br />
	 * </p>
	 * 
	 * @param secs2
	 * @return Clock
	 * @throws Secs2Exception
	 */
	public static Clock from(Secs2 secs2) throws Secs2Exception {
		return AbstractClock.from(secs2);
	}
	
	/**
	 * Returns LocalDateTime.
	 * 
	 * @return LocalDateTime
	 */
	public LocalDateTime toLocalDateTime();
	
	/**
	 * Returns A12.
	 * 
	 * @return Secs2.ascii(yyMMddhhmmss)
	 */
	public Secs2 toAscii12();
	
	/**
	 * Returns A16.
	 * 
	 * @return Secs2.ascii(yyyyMMddhhmmssSS)
	 */
	public Secs2 toAscii16();
	
}
