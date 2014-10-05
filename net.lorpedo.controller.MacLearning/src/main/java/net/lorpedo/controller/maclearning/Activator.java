package net.lorpedo.controller.maclearning;

import net.lorpedo.controller.core.Main;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	
	private MacLearningManager macLearningManager;
	private LinkDiscoveryManager linkDiscoveryManager;
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		
		System.out.println("MacLearning's activator start");
		
		macLearningManager = new MacLearningManager();
		linkDiscoveryManager = new LinkDiscoveryManager();
		
		Main.getInstance().add(0, macLearningManager);
		Main.getInstance().add(linkDiscoveryManager);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		System.out.println("MacLearning's activator stop");
		
		Main.getInstance().remove(linkDiscoveryManager);
		Main.getInstance().remove(macLearningManager);
		
		linkDiscoveryManager = null;
		macLearningManager = null;
	}

}
