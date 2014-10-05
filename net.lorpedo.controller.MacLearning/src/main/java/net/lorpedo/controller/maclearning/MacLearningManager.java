package net.lorpedo.controller.maclearning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.util.Ethernet;
import net.floodlightcontroller.util.MACAddress;
import net.lorpedo.controller.core.MacVlanPair;
import net.lorpedo.controller.core.OFHandler;
import net.lorpedo.controller.core.Port;
import net.lorpedo.controller.core.Switch;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;



public class MacLearningManager implements OFHandler {
	
	public static Map<Switch, Map<MacVlanPair, Port>> switchMap = new HashMap<Switch, Map<MacVlanPair, Port>>();//new LRULinkedHashMap<Integer, Short>(64001, 64000);
	// flow-mod - for use in the cookie
	private static final int LEARNING_SWITCH_APP_ID = 1;
	private static final int APP_ID_BITS = 12;
	private static final int APP_ID_SHIFT = (64 - APP_ID_BITS);
	private static final long LEARNING_SWITCH_COOKIE = (long) (LEARNING_SWITCH_APP_ID & ((1 << APP_ID_BITS) - 1)) << APP_ID_SHIFT;

	private static final short IDLE_TIMEOUT_DEFAULT = 500;
	private static final short HARD_TIMEOUT_DEFAULT = 100;
	private static final short PRIORITY_DEFAULT = 100;
	private static final int MAX_MACS_PER_SWITCH  = 1000; 

	public MacLearningManager() {
		
	}
	
	@Override
	public boolean handlePacketIn(Switch sw, OFPacketIn pi,  List<OFMessage> outs) {

		// Build the Match
		OFMatch match = new OFMatch();
		match.loadFromPacket(pi.getPacketData(), pi.getInPort());
		
		Long sourceMac = Ethernet.toLong(match.getDataLayerSource());
		Long destMac = Ethernet.toLong(match.getDataLayerDestination());
		Integer sourceIp = match.getNetworkSource();
		Integer destIp = match.getNetworkDestination();
		
		Short vlan = match.getDataLayerVirtualLan();
		if (vlan == (short) 0xffff) {
			vlan = 0;
		}
		if ((destMac & 0xfffffffffff0L) == 0x0180c2000000L) {
			return true;
		}
		
		Map<MacVlanPair, Port> macTable = switchMap.get(sw);
		
		if(macTable == null) {
			macTable = new HashMap<MacVlanPair,Port>(MAX_MACS_PER_SWITCH);
			switchMap.put(sw, macTable);
		}
		
		//(macTable.get(new MacVlanPair(sourceMac, vlan)) == null) && 
		if ((sourceMac & 0x010000000000L) == 0) {
			// If source MAC is a unicast address, learn the port for this MAC/VLAN
			macTable.put(new MacVlanPair(sourceMac, vlan), new Port(sourceMac, pi.getInPort()));
		}
		
		Port outPort = macTable.get(new MacVlanPair(destMac, vlan));


		// push a flow mod if we know where the packet should be going
		if (outPort!=null && outPort.getPort() != null) {
			match.setWildcards(0xffff);

			
			short command = 0;
			// start of torpedo learning ///
			OFFlowMod flowMod = (OFFlowMod) sw.stream.getMessageFactory().getMessage(OFType.FLOW_MOD);
			flowMod.setMatch(match);
			flowMod.setCookie(LEARNING_SWITCH_COOKIE);
			flowMod.setCommand(command);
			flowMod.setIdleTimeout(IDLE_TIMEOUT_DEFAULT);
			flowMod.setHardTimeout(HARD_TIMEOUT_DEFAULT);
			flowMod.setPriority(PRIORITY_DEFAULT);
			flowMod.setBufferId(pi.getBufferId());
			flowMod.setOutPort(outPort.getPort());
			flowMod.setFlags((short) (1 << 0)); // OFPFF_SEND_FLOW_REM

			flowMod.setActions(Arrays.asList((OFAction) new OFActionOutput(outPort.getPort(), (short) 0xffff)));
			flowMod.setLength((short) (OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));
			// end of torpedo mac learning //
			

			 
			outs.add(flowMod);
		}

		// Send a packet out
		if (outPort == null || pi.getBufferId() == 0xffffffff) {
			OFPacketOut po = new OFPacketOut();
			po.setBufferId(pi.getBufferId());
			po.setInPort(pi.getInPort());

			// set actions
			OFActionOutput action = new OFActionOutput();
			action.setMaxLength((short) 1500);
//			action.setPort((short) ((outPort == null) ? OFPort.OFPP_FLOOD
//					.getValue() : outPort));
			action.setPort((short) OFPort.OFPP_FLOOD.getValue());
			List<OFAction> actions = new ArrayList<OFAction>();
			actions.add(action);
			po.setActions(actions);
			po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

			// set data if needed
			if (pi.getBufferId() == 0xffffffff) {
				byte[] packetData = pi.getPacketData();
				po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
						+ po.getActionsLength() + packetData.length));
				po.setPacketData(packetData);
			} else {
				po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
						+ po.getActionsLength()));
			}

			outs.add(po);
		}
		
		
		//h1 printer(macTable);

		return true;			

	}
	public static String ipToString(int ip) {
		StringBuilder buf = new StringBuilder();
		buf.append((ip >> 24) & 0xff).append(".");
		buf.append((ip >> 16) & 0xff).append(".");
		buf.append((ip >> 8) & 0xff).append(".");
		buf.append((ip >> 0) & 0xff);

		return buf.toString();
	}
	public static StringBuilder macAppend(StringBuilder buf, long v) {
		if(v < 0x10)
			buf.append("0").append(Long.toHexString(v));
		else
			buf.append(Long.toHexString(v));

		return buf;
	}

	public static String macToString(long mac) {
		StringBuilder buf = new StringBuilder();

		macAppend(buf, (mac >> 40) & 0xff).append(":");
		macAppend(buf, (mac >> 32) & 0xff).append(":");
		macAppend(buf, (mac >> 24) & 0xff).append(":");
		macAppend(buf, (mac >> 16) & 0xff).append(":");
		macAppend(buf, (mac >> 8) & 0xff).append(":");
		macAppend(buf, (mac >> 0) & 0xff);

		return buf.toString();
	}
	void printer(Map<Long, Short> map) {
	
		for(Long l : map.keySet()) {
			System.out.println(MACAddress.valueOf(l) + " : " + map.get(l));
		}
	}
}
