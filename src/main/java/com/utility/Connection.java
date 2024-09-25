package com.utility;

import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;

public class Connection implements Closeable {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private volatile boolean isClosedIntentionally = false;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void send(Message message) throws IOException{
        synchronized (out) {
            out.writeObject(message);
        }
    }

    public Message receive() throws IOException, ClassNotFoundException{
        synchronized (in) {
            return (Message) in.readObject();
        }
    }

    public SocketAddress getRemoteSocketAddress(){
        return socket.getRemoteSocketAddress();
    }

    public boolean isClosedIntentionally(){
        return isClosedIntentionally;
    }

    public static boolean checkServerAddress(String IP) {
        return IP.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|localhost");
    }

    public static boolean checkServerPort(String portString) {
        try{
            int port = Integer.parseInt(portString);
            return port < 0 || port > 65535;
        } catch (NumberFormatException e){
            return true;
        }
    }

    public void close() throws IOException{
        isClosedIntentionally = true;
        socket.close();
        out.close();
        in.close();
    }
}
