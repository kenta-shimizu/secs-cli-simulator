package com.shimizukenta.secs.secs1ontcpip;

import java.net.SocketAddress;
import java.util.Objects;

import com.shimizukenta.secs.ReadOnlySocketAddressProperty;
import com.shimizukenta.secs.ReadOnlyTimeProperty;
import com.shimizukenta.secs.SocketAddressProperty;
import com.shimizukenta.secs.TimeProperty;
import com.shimizukenta.secs.secs1.Secs1CommunicatorConfig;

/**
 * This class is SECS-I-on-TCP/IP-Receiver-Communicator config.
 * 
 * <p>
 * To set Bind SocketAddress, {@link #socketAddress(SocketAddress)}<br />
 * </p>
 * 
 * @author kenta-shimizu
 *
 */
public class Secs1OnTcpIpReceiverCommunicatorConfig extends Secs1CommunicatorConfig {
	
	private static final long serialVersionUID = 6842392464950831424L;
	
	private SocketAddressProperty socketAddr = SocketAddressProperty.newInstance(null);
	private TimeProperty rebindSeconds = TimeProperty.newInstance(5.0F);
	
	public Secs1OnTcpIpReceiverCommunicatorConfig() {
		super();
	}
	
	/**
	 * Bind SocketAddress setter.
	 * 
	 * <p>
	 * Not accept {@code null}
	 * </p>
	 * 
	 * @param socketAddress
	 */
	public void socketAddress(SocketAddress socketAddress) {
		this.socketAddr.set(Objects.requireNonNull(socketAddress));
	}
	
	/**
	 * Bind SocketAddress getter.
	 * 
	 * @return Connect SocketAddress
	 */
	public ReadOnlySocketAddressProperty socketAddress() {
		return this.socketAddr;
	}
	
	/**
	 * Rebind seconds setter.
	 * 
	 * @param seconds
	 */
	public void rebindSeconds(float seconds) {
		this.rebindSeconds.set(seconds);
	}
	
	/**
	 * Rebind seconds getter.
	 * 
	 * @return Rebind-Seconds
	 */
	public ReadOnlyTimeProperty rebindSeconds() {
		return this.rebindSeconds;
	}
	
}
