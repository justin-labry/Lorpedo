package net.lorpedo.controller.packetdumper;

import net.lorpedo.controller.core.Main;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	
	private PacketDumpManager packetDumpManager;
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		
		System.out.println("PacketDumper activator start");
		
		packetDumpManager = new PacketDumpManager();
		
		Main.getInstance().add(2, packetDumpManager);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		System.out.println("PacketDumper activator stop");
		
		Main.getInstance().remove(packetDumpManager);
		
		packetDumpManager = null;
	}

}
