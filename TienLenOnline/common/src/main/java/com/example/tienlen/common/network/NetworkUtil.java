package com.example.tienlen.common.network;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Tiện ích dò tìm địa chỉ IP mạng LAN phục vụ kết nối đa máy qua Java RMI.
 */
public class NetworkUtil {

    /**
     * Dò tìm địa chỉ IPv4 chính của máy trong mạng LAN (Wi-Fi hoặc Ethernet).
     */
    public static String getLocalLanIp() {
        // Cách 1: Thử mở socket UDP giả định để lấy IP routing chính
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            InetAddress localAddr = socket.getLocalAddress();
            if (localAddr instanceof Inet4Address && !localAddr.isLoopbackAddress() && !localAddr.getHostAddress().equals("0.0.0.0")) {
                return localAddr.getHostAddress();
            }
        } catch (Exception ignored) {
        }

        // Cách 2: Quét tất cả các card mạng đang hoạt động (Wi-Fi / Ethernet)
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        // Ưu tiên dải mạng cục bộ tiêu chuẩn LAN
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Cách 3: Lấy từ getLocalHost()
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            if (!host.isBlank() && !host.startsWith("127.")) {
                return host;
            }
        } catch (Exception ignored) {
        }

        return "127.0.0.1";
    }

    /**
     * Trả về danh sách tất cả các địa chỉ IPv4 của các card mạng đang hoạt động trên máy
     */
    public static List<String> getAllLanIps() {
        List<String> list = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (!list.contains(ip)) {
                            list.add(ip);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (list.isEmpty()) {
            list.add("127.0.0.1");
        }
        return list;
    }
}
