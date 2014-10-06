package net.lorpedo.controller.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFEchoReply;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFeaturesRequest;
import org.openflow.protocol.OFHello;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFSetConfig;
import org.openflow.protocol.factory.BasicFactory;

public class Main implements Runnable {
	
	private static Main instance;
	
	public static Main getInstance() {
		if(instance == null) {
			try {
				instance = new Main();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		return instance;
	}
	
	public Controller controller = null;
	public final static int BACKLOG = 16;

	public static int connectionCounter = 0;

	private Selector selector;
	private ServerSocketChannel server;

	private Worker[] workers;

	public List<OFHandler> handlers = null; 
	
	private Main() throws IOException { //Constructor 
		int port = 6633;	// Read the value from configuration
		int count = 2;		// "
		
		selector = Selector.open();
		
		server = ServerSocketChannel.open();
		
		server.configureBlocking(false);
		server.bind(new InetSocketAddress(port), BACKLOG);
		server.register(selector, SelectionKey.OP_ACCEPT);
		
		workers = new Worker[count];
		
		for(int i = 0; i < count; i++) {
			workers[i] = new Worker();
			new Thread(workers[i]).start();
		}
		
		controller = Controller.getInstance();
		
		//Module Initialization
		handlers = new ArrayList<OFHandler>();
		//PacketDumpManager packetDumpManager = new PacketDumpManager();
		
		//handlers.add(packetDumpManager);
	}
	
	public void add(OFHandler handler) {
		handlers.add(handler);
	}

	public void add(int index, OFHandler handler) {
		handlers.add(index, handler);
	}
	
	public void remove(OFHandler handler) {
		handlers.remove(handler);
	}
	
	public void start() {
		new Thread(this).start();
	}

	public void stop() {
		try {
			selector.close();
		} catch (IOException e) {
		}

		for(Worker worker: workers) {
			worker.close();
		}
	}

	@Override
	public void run() {
		int index = 0;
		try {
			while(true) {
				int count = selector.select();

				if(count > 0) {
					selector.selectedKeys().clear();

					for(int j = 0; j < count; j++) {
						SocketChannel channel = server.accept(); //? are connections queued at server queue?
						index = (index + 1) % workers.length; // distribute in round robin fashion 
						workers[index].add(channel); // adding channel or connection

						int size = 0;
						for(int i = 0; i < workers.length; i++) {
							size += workers[i].size(); // sel.keys.size() method is called 
						}
						System.out.println("# of connections: " + size);
					}
				}
			}
		} catch(ClosedSelectorException e) {
			// Closed
		} catch(IOException e) {
			//
			e.printStackTrace();
		}
	}
	
	private class Worker implements Runnable {
		
		private Selector selector;
		private LinkedList<SocketChannel> queue = new LinkedList<SocketChannel>();
		private int size;
		private AtomicBoolean isAdded = new AtomicBoolean(false);

		public Worker() throws IOException {
			selector = Selector.open();
		}
		
		public void close() {
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}



		public void process(Switch sw, List<OFMessage> list, List<OFMessage> outs) {
			//	System.out.println("Packet Received");
			for(OFMessage msg: list) {
				switch(msg.getType()) {
				case PACKET_IN:

					OFPacketIn pi = (OFPacketIn)msg;

					for(OFHandler handler: handlers) {
						if(!handler.handlePacketIn(sw, pi, outs))
							break;
					}
					
					break;

				case HELLO:

					OFHello helloReply = new OFHello();
					helloReply.setXid(msg.getXid());
					outs.add(helloReply);
					//System.out.println("hello sent!");

					//send Feature Request
					OFFeaturesRequest featuresRequest = new OFFeaturesRequest();
					featuresRequest.setXid(msg.getXid());
					outs.add(featuresRequest);
					
					// send OFPT_SET_CONFIG
					OFSetConfig setConfig = new OFSetConfig();
					setConfig.setXid(msg.getXid());
					setConfig.setMissSendLength((short) 1500);
					outs.add(setConfig);

					break;

				case ECHO_REQUEST:

					OFEchoReply echoReply = new OFEchoReply();
					echoReply.setXid(msg.getXid());
					outs.add(echoReply);

					break;

				case FEATURES_REPLY:
					
					System.out.println("Features reply");
					OFFeaturesReply featuresrReply = (OFFeaturesReply)msg;

					// sw.distaPathId = ...
					long dataPathId = featuresrReply.getDatapathId();
					int nBuffers = featuresrReply.getBuffers();
					byte tables = featuresrReply.getTables();
					int capabilities = featuresrReply.getCapabilities();
					int actions = featuresrReply.getActions();
					List<OFPhysicalPort> listPort = featuresrReply.getPorts();

					sw.setDataPathId(dataPathId);
					sw.setnBuffers(nBuffers);
					sw.setTables(tables);
					sw.setCapabilities(capabilities);
					sw.setActions(actions);
					sw.setListPort(listPort);
					System.out.println(sw);
					//
					break;

				case ERROR:
					System.out.println("ERROR");

				case PORT_STATUS:
					System.out.println("PORT_STATUS");					
					

				default:
					//
					System.out.println("Unknown packet: " + msg.getType());
				}
			}
		}

		@Override
		public void run() {
			System.out.println("run!!!");
			
			LinkedList<OFMessage> outs = new LinkedList<OFMessage>();

			while(true) {
				try {
					if(isAdded.get()) {
						synchronized(queue) {
							while(!queue.isEmpty()) {
								SocketChannel channel = queue.removeFirst();
								Switch sw = new Switch(new OFMessageAsyncStream(channel, new BasicFactory()));
								channel.register(selector, SelectionKey.OP_READ, sw);
								System.out.println("Connected: #" + ++connectionCounter);
							}
							isAdded.set(false);
						}
					}

					int count = selector.select();
					if(count != 0) {
						Iterator<SelectionKey> it = selector.selectedKeys().iterator();
						while(it.hasNext()) {
							SelectionKey key = it.next();
							Switch sw = (Switch)key.attachment();
							it.remove();
							List<OFMessage>  list = null;
							try {
							 list = sw.stream.read();//i
							} catch(IOException e) {
								System.out.println("read exception");
								key.channel().close();
								synchronized(queue) {
									size--;
								}
							}
							if(list == null) {
								System.out.println("null but not closing the channel...");
								key.channel().close();
								
								//
								key.cancel();
								break;
							} else {
								outs.clear();
								process(sw, list, outs);//

								if(!outs.isEmpty()) {
									sw.stream.write(outs);//uj
									try {
										sw.stream.flush();
									} catch(IOException e) {
										System.out.println("Maybe connection disconnected?");
										// Maybe connection disconnected
										key.cancel();
									}
								}
							}
						}
					}
				} catch(ClosedSelectorException e) {
					e.printStackTrace();
					synchronized(queue) {
						size--;
					}
					// Closed
				} catch(IOException e) {
					e.printStackTrace();
	
					for(SelectionKey key: selector.keys()) {
						try {
							key.channel().close();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		}

		public void add(SocketChannel channel) throws IOException {
			channel.configureBlocking(false);
			channel.socket().setTcpNoDelay(true);
			channel.socket().setSendBufferSize(65536);
			channel.socket().setPerformancePreferences(0,2,3);

			synchronized(queue) {
				size++;
				queue.add(channel);
				isAdded.set(true);
			}

			while(isAdded.get()) {
				selector.wakeup();
				Thread.yield();
			}
		}

		public int size() {
			synchronized(queue) {
				return size;
			}
		}
	}

}
