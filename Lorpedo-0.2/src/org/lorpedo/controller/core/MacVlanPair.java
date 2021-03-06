/**
*    Copyright 2011, Big Switch Networks, Inc. 
*    Originally created by David Erickson, Stanford University
* 
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

package org.lorpedo.controller.core;


public class MacVlanPair {
    private final Long mac;
    private final Short vlan;
    
    public MacVlanPair(Long mac, Short vlan) {
        this.mac = mac;
        this.vlan = vlan;
    }
    
    public long getMac() {
        return mac.longValue();
    }
    
    public short getVlan() {
        return vlan.shortValue();
    }
    
    public boolean equals(Object o) {
    	if ( o == null ) {
    		return false;
    	}
    	
    	if ( getClass() != o.getClass() ) {
    		return false;
    	}
    	
    	MacVlanPair other = (MacVlanPair) o;
    	return mac.equals(other.mac) && vlan.equals(other.vlan); 
    }
    
    public int hashCode() {
        return mac.hashCode() ^ vlan.hashCode();
    }
}