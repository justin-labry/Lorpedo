package org.lorpedo.controller.core;

public class Port {
	Short port;
	Long hardwareAddress;
	
	public Port(Long hardwareAddress, Short port) {
		this.hardwareAddress = hardwareAddress;
		this.port = port;
	}
	public Short getPort() {
		return port;
	}
	public void setPort(Short port) {
		this.port = port;
	}
	public Long getHardwareAddress() {
		return hardwareAddress;
	}
	public void setHardwareAddress(Long hardwareAddress) {
		this.hardwareAddress = hardwareAddress;
	}
	
	

}
