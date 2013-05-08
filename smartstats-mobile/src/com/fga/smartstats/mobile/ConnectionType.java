package com.fga.smartstats.mobile;

public enum ConnectionType {

	WIFI("WIFI"), TRIG("3G"),BOTH("WIFI or 3G");
	
	private String label;
	private ConnectionType(String label){
		
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
}
