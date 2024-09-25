package com.server;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.utility.Connection;
import com.utility.Message;
import com.utility.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {

    private static volatile boolean isStoppedIntentionally = false;
    private static ServerSocket serverSocket;
    static final Logger serverLogger = LogManager.getLogger(Server.class);
    static final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    static final AtomicInteger port = new AtomicInteger();
    static final Object restartLock = new Object();

    public static void main(String[] args) {
        while(true)
            synchronized (restartLock){
                launch();
                try{
                    restartLock.wait();
                } catch (InterruptedException e){
                    serverLogger.error("Unknown interrupted exception before restarting a server");
                }
            }
    }

    private static void launch() {
        new Thread(ServerGUI::start).start();
        synchronized (port){
            try {
                port.wait();
            } catch (InterruptedException e){
                serverLogger.error("Unknown interrupted exception before launching a server");
            }
        }
        new Thread(() -> {
            try(ServerSocket server = new ServerSocket(port.get())) {
                serverSocket = server;
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Handler(socket).start();
                }
            } catch(IOException e){
                if(isStoppedIntentionally) return;
                serverLogger.error("An IO error occurred while establishing connection with a client");
            }
        }).start();
    }

    public static void sendBroadcastMessage(Message message){
        for(String name: connectionMap.keySet()){
            try {
                connectionMap.get(name).send(message);
            }
            catch (IOException e){
                serverLogger.error("An error occurred while sending message to remote address {}",
                        connectionMap.get(name).getRemoteSocketAddress());
            }
        }
    }

    private static class Handler extends Thread {

        private final Socket socket;

        public Handler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run(){
            serverLogger.info("A new connection has been established at {}", socket.getRemoteSocketAddress());
            String userName = null;
            try(Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUserAboutOthers(connection, userName);
                serverMainLoop(connection, userName);
            } catch (IOException | ClassNotFoundException e) {
                serverLogger.info("The connection with the remote address {} was closed", socket.getRemoteSocketAddress());
            }
            if(userName != null){
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while(true){
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message message = connection.receive();
                if(message.getType() != MessageType.USER_NAME)
                    continue;
                String name = message.getText();
                if(connectionMap.containsKey(name))
                    continue;
                connectionMap.put(name,connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return name;
            }
        }

        private void notifyUserAboutOthers(Connection connection, String userName) throws IOException{
            for(String name: connectionMap.keySet()){
                if(name.equals(userName)) continue;
                Message message = new Message(MessageType.USER_ADDED, name);
                connection.send(message);
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while (true) {
                Message message = connection.receive();
                if(message.getText().isEmpty()) continue;
                if (message.getType() == MessageType.TEXT) {
                    message = new Message(MessageType.TEXT, String.format("%s: %s", userName, message.getText()));
                    sendBroadcastMessage(message);
                } else serverLogger.error("An error occurred. The type of the message isn't text.");
            }
        }
    }

    public static void close() throws IOException{
        isStoppedIntentionally = true;
        serverSocket.close();
    }
}
