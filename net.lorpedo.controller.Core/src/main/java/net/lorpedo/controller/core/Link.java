package net.lorpedo.controller.core;


public class Link {
	protected long sid;
	protected long smac;
	protected int sport;
	
	protected long did;
	protected long dmac;
	protected int dport;
	
	protected long ttl;
	
	public Link() {
	}
	
	public Link(long smac, int sport, long dmac, int dport, long ttl) {
		this.smac = smac;
		this.sport = sport;
		this.dmac = dmac;
		this.dport = dport;
		this.ttl = ttl;
	}
	
	public long getSid() {
		return sid;
	}

	public void setSid(long sid) {
		this.sid = sid;
	}

	public long getDid() {
		return did;
	}

	public void setDid(long did) {
		this.did = did;
	}
	
	public long getSmac() {
		return smac;
	}
	
	public void setSmac(long smac) {
		this.smac = smac;
	}
	
	public long getDmac() {
		return dmac;
	}
	
	public void setDmac(long dmac) {
		this.dmac = dmac;
	}
	
	public int getSport() {
		return sport;
	}
	
	public void setSport(int sport) {
		this.sport = sport;
	}
	
	public int getDport() {
		return dport;
	}
	
	public void setDport(int dport) {
		this.dport = dport;
	}
	
	public long getTTL() {
		return ttl;
	}
	
	public void setTTL(long ttl) {
		this.ttl = ttl;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Link) {
			Link link = (Link)obj;
			
			if(sid == link.sid && smac == link.smac && sport == link.sport && dmac == link.dmac && dport == link.dport && did == link.did)
				return true;
			else
				return false;
				
		} else 
			return false;
	}

	@Override
	public String toString() {
		return Long.toHexString(sid) + " [" + Long.toHexString(sid) + "," + Long.toHexString(smac) + " : " + Long.toHexString(did) + "," + Long.toHexString(dmac) + " ]" ;
	}
}
