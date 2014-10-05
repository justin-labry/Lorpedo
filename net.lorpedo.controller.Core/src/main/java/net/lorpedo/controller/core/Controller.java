package net.lorpedo.controller.core;

import java.util.List;
import java.util.Map;

public class Controller {

	private static Controller controller = new Controller();
	Map<Long, List<Link>> switches = null;

	private Controller() { }

	public static Controller getInstance() {
		return controller;
	}
}
