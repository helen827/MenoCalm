package com.livemore.api.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
@Profile("dev")
public class LanDiscoveryResponder {

    private static final int DISCOVERY_PORT = 32145;
    private static final String REQUEST = "CHAOAN_DISCOVERY_REQUEST";
    private static final String RESPONSE_PREFIX = "CHAOAN_DISCOVERY_RESPONSE ";

    @Value("${server.port:8080}")
    private int port;

    private DatagramSocket socket;
    private Thread thread;

    @PostConstruct
    public void start() {
        try {
            socket = new DatagramSocket(DISCOVERY_PORT);
            socket.setReuseAddress(true);
        } catch (Exception ignored) {
            return;
        }

        thread = new Thread(this::loop, "chaoan-lan-discovery");
        thread.setDaemon(true);
        thread.start();
    }

    private void loop() {
        byte[] buf = new byte[512];
        while (socket != null && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8).trim();
                if (!REQUEST.equals(msg)) {
                    continue;
                }
                String ip = bestLocalIpv4();
                if (ip == null || ip.isBlank()) {
                    continue;
                }
                String body = RESPONSE_PREFIX + "http://" + ip + ":" + port;
                byte[] resp = body.getBytes(StandardCharsets.UTF_8);
                DatagramPacket reply = new DatagramPacket(resp, resp.length, packet.getAddress(), packet.getPort());
                socket.send(reply);
            } catch (Exception ignored) {
                // keep loop
            }
        }
    }

    private String bestLocalIpv4() throws SocketException {
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                continue;
            }
            for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                    String ip = addr.getHostAddress();
                    if (ip != null && (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("172."))) {
                        return ip;
                    }
                }
            }
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return null;
        }
    }

    @PreDestroy
    public void stop() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
        socket = null;
        thread = null;
    }
}
