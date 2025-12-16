package com.example.proyecto_iot.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class NetworkUtils {
    private NetworkUtils() {}

    /** Devuelve true si el dispositivo puede abrir socket TCP contra host:port (útil para saber si VPN/UPV “de verdad” funciona) */
    public static boolean canReach(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

