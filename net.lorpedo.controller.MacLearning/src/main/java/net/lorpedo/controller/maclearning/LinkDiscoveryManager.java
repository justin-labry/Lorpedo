package net.lorpedo.controller.maclearning;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.floodlightcontroller.util.BSN;
import net.floodlightcontroller.util.Ethernet;
import net.floodlightcontroller.util.LLDP;
import net.floodlightcontroller.util.LLDPTLV;
import net.lorpedo.controller.core.Link;
import net.lorpedo.controller.core.OFHandler;
import net.lorpedo.controller.core.Port;
import net.lorpedo.controller.core.Switch;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.HexString;


public class LinkDiscoveryManager implements OFHandler {
	
	private static final byte[] LLDP_STANDARD_DST_MAC_STRING = 
			HexString.fromHexString("01:80:c2:00:00:0e");
		private static final long LINK_LOCAL_MASK  = 0xfffffffffff0L;
		private static final long LINK_LOCAL_VALUE = 0x0180c2000000L;

		// BigSwitch OUI is 5C:16:C7, so 5D:16:C7 is the multicast version
		// private static final String LLDP_BSN_DST_MAC_STRING = "5d:16:c7:00:00:01";
		private static final String LLDP_BSN_DST_MAC_STRING = "ff:ff:ff:ff:ff:ff";

		private static final byte TLV_DIRECTION_TYPE = 0x73;
		private static final short TLV_DIRECTION_LENGTH = 1;  // 1 byte
		private static final byte TLV_DIRECTION_VALUE_FORWARD[] = {0x01};
		private static final byte TLV_DIRECTION_VALUE_REVERSE[] = {0x02};

		protected final int DISCOVERY_TASK_INTERVAL = 1; 	// 1 second.
		protected final int LLDP_TO_ALL_INTERVAL = 15 ; 	//15 seconds.
		protected long lldpClock = 0;
		
		private static final LLDPTLV forwardTLV 
		= new LLDPTLV().
		setType((byte)TLV_DIRECTION_TYPE).
		setLength((short)TLV_DIRECTION_LENGTH).
		setValue(TLV_DIRECTION_VALUE_FORWARD);
		
		private static final LLDPTLV reverseTLV 
		= new LLDPTLV().
		setType((byte)TLV_DIRECTION_TYPE).
		setLength((short)TLV_DIRECTION_LENGTH).
		setValue(TLV_DIRECTION_VALUE_REVERSE);
		
		
		protected LLDPTLV controllerTLV;
		
	private Timer timer;
	private List<Link> links  = new LinkedList<Link>();
	
	public LinkDiscoveryManager() {
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				long time = System.currentTimeMillis();

	
				synchronized(links) {
					Iterator<Link> i = links.iterator();
					while(i.hasNext()) {
						if(i.next().getTTL() < time)
							i.remove();
					}
					
					discoverOnAllPorts();
					System.out.println("links " + links);
				}
			}
		}, 1, 3 * 1000);
		
	}

	/**
	 * Send LLDPs to all switch-ports
	 */
	private void discoverOnAllPorts() {
		byte[] controllerTLVValue = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};  // 8 byte value.
		this.controllerTLV = new LLDPTLV().setType((byte) 0x0c).setLength((short) controllerTLVValue.length).setValue(controllerTLVValue);
		
		for ( Switch sw : MacLearningManager.switchMap.keySet() ) {
			if ( sw == null ) continue;

				for (Port port: MacLearningManager.switchMap.get(sw).values() ) {
					if(port == null) continue;
					//sendDiscoveryMessage(sw, ofp.getPortNumber(), true, false);
					// using "nearest customer bridge" MAC address for broadest possible propagation
					// through provider and TPMR bridges (see IEEE 802.1AB-2009 and 802.1Q-2011),
					// in particular the Linux bridge which behaves mostly like a provider bridge
					byte[] chassisId = new byte[] {4, 0, 0, 0, 0, 0, 0}; // filled in later
					byte[] portId = new byte[] {2, 0, 0}; // filled in later
					byte[] ttlValue = new byte[] {0, 0x78};
					// OpenFlow OUI - 00-26-E1
					byte[] dpidTLVValue = new byte[] {0x0, 0x26, (byte) 0xe1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
					LLDPTLV dpidTLV = new LLDPTLV().setType((byte) 127).setLength((short) dpidTLVValue.length).setValue(dpidTLVValue);

					byte[] dpidArray = new byte[8];
					ByteBuffer dpidBB = ByteBuffer.wrap(dpidArray);
					ByteBuffer portBB = ByteBuffer.wrap(portId, 1, 2);

					Long dpid = sw.getDataPathId();

					dpidBB.putLong(dpid);
					// set the ethernet source mac to last 6 bytes of dpid
					byte[] tmp_hw = new byte[8];
					tmp_hw = ByteBuffer.allocate(8).putLong(port.getHardwareAddress()).array();
					byte[] port_hw = new byte[6];
					
					for(int i = 0; i < 6; i++)
						port_hw[i] = tmp_hw[i];
					
					System.arraycopy(dpidArray, 2, port_hw, 0, 6);
					// set the chassis id's value to last 6 bytes of dpid
					System.arraycopy(dpidArray, 2, chassisId, 1, 6);
					// set the optional tlv to the full dpid
					System.arraycopy(dpidArray, 0, dpidTLVValue, 4, 8);

					// set the portId to the outgoing port
					portBB.putShort(port.getPort());

					LLDP lldp = new LLDP();
					lldp.setChassisId(new LLDPTLV().setType((byte) 1).setLength((short) chassisId.length).setValue(chassisId));
					lldp.setPortId(new LLDPTLV().setType((byte) 2).setLength((short) portId.length).setValue(portId));
					lldp.setTtl(new LLDPTLV().setType((byte) 3).setLength((short) ttlValue.length).setValue(ttlValue));
					lldp.getOptionalTLVList().add(dpidTLV);

					// Add the controller identifier to the TLV value.
					lldp.getOptionalTLVList().add(controllerTLV);
					
					boolean isReverse = false;
					boolean isStandard = true;
					
					if (isReverse) {
						lldp.getOptionalTLVList().add(reverseTLV);
					}else {
						lldp.getOptionalTLVList().add(forwardTLV);
					}

					Ethernet ethernet;
					
					
					if (isStandard) {
						ethernet = new Ethernet();
						byte[] tmp_mac = ByteBuffer.allocate(8).putLong(port.getHardwareAddress()).array();
						byte[] source_mac = new byte[6];
						for(int i = 0; i < 6; i++)
							source_mac[i] = tmp_mac[i];
						
						ethernet.setSourceMACAddress(source_mac);
						ethernet.setDestinationMACAddress(LLDP_STANDARD_DST_MAC_STRING);
						ethernet.setEtherType(Ethernet.TYPE_LLDP);
						ethernet.setPayload(lldp);
					} else {
						BSN bsn = new BSN(BSN.BSN_TYPE_BDDP);
						bsn.setPayload(lldp);

						ethernet = new Ethernet()
						.setSourceMACAddress(ByteBuffer.allocate(6).putLong(port.getHardwareAddress()).array())
						.setDestinationMACAddress(LLDP_BSN_DST_MAC_STRING)
						.setEtherType(Ethernet.TYPE_BSN);
						ethernet.setPayload(bsn);
					}

					// serialize and wrap in a packet out
					byte[] data = ethernet.serialize();
					OFPacketOut po = (OFPacketOut) sw.stream.getMessageFactory().getMessage(OFType.PACKET_OUT);
					po.setBufferId(OFPacketOut.BUFFER_ID_NONE);
					po.setInPort(OFPort.OFPP_NONE);

					// set actions
					List<OFAction> actions = new ArrayList<OFAction>();
					actions.add(new OFActionOutput(port.getPort(), (short) 0));
					po.setActions(actions);
					po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

					// set data
					po.setLengthU(OFPacketOut.MINIMUM_LENGTH + po.getActionsLength() + data.length);
					po.setPacketData(data);
					
					// send data
					try {
						sw.stream.write(po);
						sw.stream.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						System.out.println("disconnected switches");
					}
					
				}
			}
		}
		
	private Link parseLLDP(byte[] payload) {
		int idx = 14;
		
		Link link = new Link();
		
		while(idx < payload.length) {
			int type = (payload[idx] & 0xff) >> 1;
			int length = ((payload[idx] & 0x01) << 9) | (payload[idx + 1] & 0xff);
			
			idx += 2;
			
			switch(type) {
				case 0:
					return link;
				case 2:
					//Port ID
					byte portIdSubtype = payload[idx];
					if(portIdSubtype == 3) {
				        long mac = (((long)(payload[idx + 1] & 0xff) << 40) +
				                ((long)(payload[idx + 2] & 0xff) << 32) +
				                ((long)(payload[idx + 3] & 0xff) << 24) +
				                ((payload[idx + 4] & 0xff) << 16) +
				                ((payload[idx + 5] & 0xff) <<  8) +
				                ((payload[idx + 6] & 0xff) <<  0));
				        link.setSmac(mac);
					}
					break;
				case 127:
					if(payload[idx + 0] == 0x00 && payload[idx + 1] == 0x12 && payload[idx + 2] == 0x0f && payload[idx + 3] == 0x01) {
						int port = payload[idx + 8] & 0xf;
						link.setSport(port);
					}
					break;
				case 3:
					//TTL
					short ttl = (short)((payload[idx + 0] & 0xff) << 8 | (payload[idx + 1] & 0xff));
					
					link.setTTL(System.currentTimeMillis() + ttl * 1000);
					break;
			}
	
			idx += length;
		}
		return null;
	}
	
	@Override
	public boolean handlePacketIn(Switch sw, OFPacketIn pi, List<OFMessage> outs) {

		byte[] eth = pi.getPacketData();

		if(eth[12] == (byte)0x88 && eth[13] == (byte)0xcc) {
			
			System.out.println("dataPathId: " + HexString.toHexString(sw.getDataPathId()));
			Link link = parseLLDP(eth);
			if(link != null) {
				link.setDport(pi.getInPort());
				link.setDid(sw.getDataPathId());
//				int idex = 0;
//		        long mac = (((long)(eth[idex + 0] & 0xff) << 40) +
//		                ((long)(eth[idex + 1] & 0xff) << 32) +
//		                ((long)(eth[idex + 2] & 0xff) << 24) +
//		                ((eth[idex + 3] & 0xff) << 16) +
//		                ((eth[idex + 4] & 0xff) <<  8) +
//		                ((eth[idex + 5] & 0xff) <<  0));
				
				OFPhysicalPort port = null;
				for(OFPhysicalPort p: sw.getListPort()) {
					if(p.getPortNumber() == pi.getInPort()) {
						port = p;
						break;
					}
				}
				byte[] dmac = port.getHardwareAddress();
				long mac = (((long)(dmac[0] & 0xff) << 40) +
						((long)(dmac[1] & 0xff) << 32) +
						((long)(dmac[2] & 0xff) << 24) +
						((dmac[3] & 0xff) << 16) +
						((dmac[4] & 0xff) << 8) +
						((dmac[5] & 0xff) << 0));
						
						
				link.setDmac(mac);//00:00:00:00:00:01
				
				synchronized(links) {
					int idx = links.indexOf(link);
					if(idx == -1) {
						links.add(link);
					} else {
						links.remove(idx);
						links.add(link);
					}
				}
			}
		}

		return false;
	}
}
