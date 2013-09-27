package org.lorpedo.controller.core;

import java.util.List;

import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFPhysicalPort;

public class Switch {
	public OFMessageAsyncStream stream;
	
	// .. features
	long dataPathId;
	int nBuffers;
	byte tables;
	int capabilities;
	int actions;
	List<OFPhysicalPort> listPort;
	List<Long> listSwitch;
	// ...
	
	
	public Switch(OFMessageAsyncStream stream) {
		this.stream = stream;
	}

	public List<Long> getListSwitch() {
		return listSwitch;
	}

	public void setListSwitch(List<Long> listSwitch) {
		this.listSwitch = listSwitch;
	}

	public OFMessageAsyncStream getStream() {
		return stream;
	}

	public void setStream(OFMessageAsyncStream stream) {
		this.stream = stream;
	}

	public long getDataPathId() {
		return dataPathId;
	}

	public void setDataPathId(long dataPathId) {
		this.dataPathId = dataPathId;
	}

	public int getnBuffers() {
		return nBuffers;
	}

	public void setnBuffers(int nBuffers) {
		this.nBuffers = nBuffers;
	}

	public byte getTables() {
		return tables;
	}

	public void setTables(byte tables) {
		this.tables = tables;
	}

	public int getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(int capabilities) {
		this.capabilities = capabilities;
	}

	public int getActions() {
		return actions;
	}

	public void setActions(int actions) {
		this.actions = actions;
	}

	public List<OFPhysicalPort> getListPort() {
		return listPort;
	}

	public void setListPort(List<OFPhysicalPort> listPort) {
		this.listPort = listPort;
	}

	@Override
	public String toString() {
		return "Switch [stream=" + stream + ", dataPathId=" + dataPathId
				+ ", nBuffers=" + nBuffers + ", tables=" + tables
				+ ", capabilities=" + capabilities + ", actions=" + actions
				+ ", listPort=" + listPort + "]";
	}
	
	
}