package com.solab.example;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.solab.example.protos.MaxProto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class Lookups {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public void ipLookup(String ip, MaxProto.Response.Builder resp) {
		InetAddress ipAddress;
		try {
			ipAddress = InetAddress.getByName(ip);
		}
		catch (UnknownHostException e) {
			log.error("Lookup: Unknown host {}", ip);
			resp.setError("UnknownHostException: " + e.getMessage());
			return;
		}

		MaxProto.Location.Builder loc = MaxProto.Location.newBuilder();
		loc.setCountryIso("MX").setCountryName("Mexico");
		loc.setCityName("CDMX");
		loc.setPostal("11800");
		resp.setLoc(loc.build());
	}

}
