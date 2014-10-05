package net.lorpedo.controller.core;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		
		System.out.println("Hello Lorpedo!!");
		int count = 0;

		count = 2;
		
		Thread t = null;
		try {
		t = new Thread(Main.getInstance());
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("Hello Lorpedo!! - 3");
		t.start();
		
		System.out.println("Lorpedo is activated with " + count + " threads..");
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		System.out.println("Goodbye Lorpedo!!");
	}

}
