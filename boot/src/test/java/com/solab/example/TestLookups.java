package com.solab.example;

import com.solab.example.protos.MaxProto;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test lookups with the new server.
 * These tests use the Spring Boot Test facility, which starts
 * the service and then runs the tests so all we need to do
 * is send some messages.
 *
 * @author Enrique Zamudio
 *         Date: 1/19/17 2:11 PM
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestLookups {

    private static AtomicInteger counter = new AtomicInteger();

    private static MaxProto.Request.Builder req() {
        return MaxProto.Request.newBuilder().setId(counter.incrementAndGet());
    }

    @Test
    public void testUnset() throws IOException {
        MaxProto.Response resp = send(req().build());
        Assert.assertEquals(MaxProto.Response.ResultCase.ERROR, resp.getResultCase());
    }

    @Test
    public void testIpLookupMX() throws IOException {
        MaxProto.Response resp = send(req().setIp("72.229.28.185").build());
        Assert.assertEquals("MX", resp.getLoc().getCountryIso());
    }

    @Test
    public void testIpLookupError() throws IOException {
        MaxProto.Response resp = send(req().setIp("trash").build());
        Assert.assertEquals(MaxProto.Response.ResultCase.ERROR, resp.getResultCase());
    }


    @Test
    public void testIpLookupCambridge() throws IOException {
        MaxProto.Response resp = send(req().setIp("131.111.131.1").build());
        Assert.assertEquals("CDMX", resp.getLoc().getCityName());
    }


    @Test
    public void testTimeLookup() throws IOException {
        final long now = System.currentTimeMillis();
        MaxProto.Response resp = send(req().setTime(now).build());
        Assert.assertEquals(MaxProto.Response.ResultCase.TIME, resp.getResultCase());
        Assert.assertEquals(now, resp.getTime().getTimeSent());
        Assert.assertTrue(resp.getTime().getServerTime() >= now);
    }

    @Test
    public void testPhoneLookupErrors() throws IOException {
        MaxProto.Response resp = send(req().setPhone("asdf").build());
        Assert.assertEquals(MaxProto.Response.ResultCase.ERROR, resp.getResultCase());
        resp = send(req().setPhone("+5215591996109asdf").build());
        Assert.assertEquals(MaxProto.Response.ResultCase.ERROR, resp.getResultCase());
    }

    @Test
    public void testPhoneLookupMX() throws IOException {
        MaxProto.Response resp = send(req().setPhone("+5215591996109").build());
        Assert.assertEquals("MX", resp.getLoc().getCountryIso());
        resp = send(req().setPhone("+527773170794").build());
        Assert.assertEquals("MX", resp.getLoc().getCountryIso());
    }

    @Test
    public void testPhoneLookupCA() throws IOException {
        MaxProto.Response resp = send(req().setPhone("+16046811111").build());
        Assert.assertEquals("CA", resp.getLoc().getCountryIso());
    }

    @Test
    public void testPhoneLookupUS() throws IOException {
        MaxProto.Response resp = send(req().setPhone("+16504556864").build());
        Assert.assertEquals("US", resp.getLoc().getCountryIso());
    }

    private int readBytes(InputStream inputStream, byte[] buf, int numBytes) throws IOException {
        int totalRead = 0;
        while (totalRead < numBytes) {
            int nr = inputStream.read(buf, totalRead, numBytes - totalRead);
            if (nr == -1) {
                return -1;
            }
            totalRead += nr;
        }
        return totalRead;
    }

    /** Sends a request to the server, reads the response, closes the socket. */
    private MaxProto.Response send(MaxProto.Request request) throws IOException {
        Socket sock = new Socket("127.0.0.1", 9999);
        byte[] buf = new byte[4];
        final int l = request.getSerializedSize();
        buf[0] = 0;
        buf[1] = (byte)((l & 0xff0000) >> 8);
        buf[2] = (byte)((l & 0xff00) >> 8);
        buf[3] = (byte)(l & 0xff);
        sock.getOutputStream().write(buf);
        request.writeTo(sock.getOutputStream());
        sock.getOutputStream().flush();

        // Read the header
        if (readBytes(sock.getInputStream(), buf, 4) == -1) {
            sock.close();
            throw new IOException("Couldn't read complete length header");
        }
        int mlen = ((buf[0] & 0xff) << 24) | ((buf[1] & 0xff) << 16) |
                ((buf[2] & 0xff) << 8) | (buf[3] & 0xff);

        // Read the data
        buf = new byte[mlen];
        if (readBytes(sock.getInputStream(), buf, mlen) == -1) {
            sock.close();
            throw new IOException("Couldn't read complete response");
        }

        MaxProto.Response resp = MaxProto.Response.parser().parseFrom(buf);
        sock.close();
        return resp;
    }

}
