package com.solab.example;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.solab.example.protos.MaxProto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;


public class ProtobufHandler extends SimpleChannelInboundHandler<MaxProto.Request> {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Timer ipTimer = Metrics.timer("maxmind.reqs", "query", "ip");
	private final Timer phoneTimer = Metrics.timer("maxmind.reqs", "query", "phone");
	private final Counter goodIpMeter = Metrics.counter("maxmind.lookup", "query", "ip", "result", "ok");
	private final Counter badIpMeter = Metrics.counter("maxmind.lookup", "query", "ip", "result", "bad");
	private final Counter goodPhoneMeter = Metrics.counter("maxmind.lookup", "query", "phone", "result", "ok");
	private final Counter badPhoneMeter = Metrics.counter("maxmind.lookup", "query", "phone", "result", "bad");
	private final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
	private final PhoneNumberOfflineGeocoder GEOCODER = PhoneNumberOfflineGeocoder.getInstance();
	private final Lookups ipLookup;

	ProtobufHandler(Lookups lookups) {
		this.ipLookup = lookups;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MaxProto.Request msg) throws Exception {
		MaxProto.Response.Builder builder = MaxProto.Response.newBuilder().setId(msg.getId());

		switch (msg.getSearchCase()) {
			case IP:
				log.debug("IP lookup {}", msg.getIp());
				ipTimer.record(() -> {
					ipLookup.ipLookup(msg.getIp(), builder);
					if (Utils.nonempty(builder.getError())) {
						badIpMeter.increment();
					}
					else {
						goodIpMeter.increment();
					}
				});
				break;
			case TIME:
				log.debug("Time request");
				processGetTime(msg.getTime(), builder);
				break;
			case PHONE:
				log.debug("Phone lookup {}", msg.getPhone());
				phoneTimer.record(() -> {
					phoneLookup(msg.getPhone(), builder);
					if (Utils.nonempty(builder.getError())) {
						badPhoneMeter.increment();
					}
					else {
						goodPhoneMeter.increment();
					}
				});
				break;
			default:
				builder.setError("You must set one of ip, time or phone fields.");
		}
		ctx.channel().writeAndFlush(builder.build());
	}

	private void processGetTime(long timeSent, MaxProto.Response.Builder resp) {
		final long curTime = System.currentTimeMillis();
		MaxProto.Time t = MaxProto.Time.newBuilder()
			.setTimeSent(timeSent).setServerTime(curTime)
			.setStt(curTime - timeSent)
			.build();
		resp.setTime(t);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.error("Something bad happened", cause);
		// We don't close the channel because we can keep serving requests.
	}

	private void phoneLookup(String telephone, MaxProto.Response.Builder resp) {
		Phonenumber.PhoneNumber number;
		try {
			number = PHONE_UTIL.parseAndKeepRawInput(telephone, null);
		}
		catch (NumberParseException e) {
			log.error("Error while doing a telephone lookup for {}: {}", telephone, e.getMessage());
			resp.setError(e.getErrorType() + ". " + e.getMessage() + " (" + telephone + ")");
			return;
		}
		if (!PHONE_UTIL.isValidNumber(number)) {
			resp.setError("Invalid number: " + telephone);
			return;
		}
		resp.setLoc(MaxProto.Location.newBuilder()
			.setCountryIso(PHONE_UTIL.getRegionCodeForNumber(number))
			.setCountryName(GEOCODER.getDescriptionForNumber(number, Locale.ENGLISH, ""))
			.setSubdivisionName(GEOCODER.getDescriptionForNumber(number, Locale.ENGLISH))
			.build());
	}

}
