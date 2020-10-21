package com.shimizukenta.secs;

import com.shimizukenta.secs.SecsCommunicator;

import java.util.EventListener;

/**
 * SECS-Communicate-State Change Listener.
 * 
 * <p>
 * This interface is called in {@link SecsCommunicator#addSecsCommunicatableStateChangeListener(SecsCommunicatableStateChangeListener)}<br />
 * </p>
 * 
 * @author kenta-shimizu
 *
 */
public interface SecsCommunicatableStateChangeListener extends EventListener {
	
	/**
	 * SECS-Communicate-State Changed
	 * 
	 * <p>
	 * Blocking-method.<br />
	 * pass through quickly.<br />
	 * </p>
	 * 
	 * @param communicatable {@code true} if state is communicatable
	 */
	public void changed(boolean communicatable);
}
