package org.lorpedo.controller.core;

import java.util.List;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;


public interface OFHandler {
	
	boolean handlePacketIn(Switch sw, OFPacketIn pi,  List<OFMessage> outs);

}
