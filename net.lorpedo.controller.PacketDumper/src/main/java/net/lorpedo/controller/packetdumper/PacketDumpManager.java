package net.lorpedo.controller.packetdumper;

import java.nio.ByteBuffer;
import java.util.List;



import net.lorpedo.controller.core.OFHandler;
import net.lorpedo.controller.core.Switch;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;


public class PacketDumpManager implements OFHandler {

	//HW... how to get the whole packet?
	// check the size of payload.... 
	private  synchronized void dumpLLDP(byte[] payload) {
		// LLDP, ARP info parse and ... topology output to stdout
		//  

		int idx = 14;

		System.out.println("length of payload :" + payload.length );
		while(idx < payload.length) {
			int type = (payload[idx] & 0xff) >> 1;
		int length = ((payload[idx] & 0x01) << 9) | (payload[idx + 1] & 0xff);

		System.out.println("type = " + type);
		System.out.println("length = " + length);
		idx += 2;

		if(type == 0 && length == 0)
			break;
		
		byte[] longBuffer = new byte[8];
	//	byte[] intBUffer = new byte[4];
		byte[] shortBuffer = new byte[2];
		
		switch(type) {
		case 1:

			//Chasis ID (in Mininet, it is host operating system's MAC address) 
			//			for(int j = 0; j < length; j++)
			//				System.out.printf("%02x", payload[idx + j]);

	//		short chassisIdSubtype = payload[idx + 0];

			longBuffer[0] = 0x00;
			longBuffer[1] = 0x00;

			for(int i = 0; i < 6; i++)
				longBuffer[i + 2] = payload[idx + 1 + i];

	//		long chassisId = byteToLong(longBuffer);

			break;
		case 2:
			//Port ID

			short portIdSubtype = payload[idx + 0];


			longBuffer[0] = 0x00;
			longBuffer[1] = 0x00;

			for(int i = 0; i < 6; i++)
				longBuffer[i + 2] = payload[idx + 1 + i];

			long portId = byteToLong(longBuffer);
			
			System.out.print("type 2 " + portIdSubtype + " ");
			System.out.printf("%6x", portId);
			
			break;
		case 3:
			//TTL
			shortBuffer[0] = payload[idx];
			shortBuffer[1] = payload[idx + 1];
		//	short ttl = byteToShort(longBuffer);
			
			for(int j = 0; j < length; j++)
				System.out.printf("%02x", payload[idx + j]);
			break;
		case 4:
			//Port Description
			for(int j = 0; j < length; j++) {
				System.out.printf("%c", payload[idx + j]);
			}
			break;
		case 5:
			//System Name
			for(int j = 0; j < length; j++) {
				System.out.printf("%c", payload[idx + j]);
			}
			break;
		case 6:
			//System Description
			for(int j = 0; j < length; j++) {
				System.out.printf("%c", payload[idx + j]);
			}
			break;
			
		case 7:
			//System Capabilities
			for(int j = 0; j < length; j++)
				System.out.printf("%02x", payload[idx + j]);
			break;
			
		case 8: 
			//Management Address
			for(int j = 0; j < length; j++)
				System.out.printf("%02x", payload[idx + j]);
			break;
			
		default:
			for(int j = 0; j < length; j++) {
				System.out.printf("%02x", payload[idx + j]);
			}
		}

		System.out.println();

		idx += length;
		}
	}

	@Override
	public boolean handlePacketIn(Switch sw, OFPacketIn pi, List<OFMessage> outs) {

		byte[] eth = pi.getPacketData();
		System.out.printf("Ethertype: %02x%02x\n", eth[12], eth[13]);
		if(eth[12] == (byte)0x88 && eth[13] == (byte)0xcc) {
			dumpLLDP(eth);
		}

		return true;
	}

	public long byteToLong(byte[] buff) {

		ByteBuffer bb = ByteBuffer.wrap(buff);
		return bb.getLong();
	}

}
