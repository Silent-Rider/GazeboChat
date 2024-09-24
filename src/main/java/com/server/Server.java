package com.server;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.utility.Connection;
import com.utility.Message;
import com.utility.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {

    private static final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    static volatile boolean isRunning = false;
    static final Logger logger = LogManager.getLogger(Server.class);

    static void launch(int port) {
        Thread mainThread = new Thread(() -> {
            try(ServerSocket server = new ServerSocket(port)) {
                isRunning = true;
                while (isRunning) {
                    Socket socket = server.accept();
                    new Handler(socket).start();
                }
            } catch(IOException e){
                logger.error("An IO error occurred while either creating server socket or accepting client request. " +
                        "The message of the error: {}", e.getMessage());
            }
        });
        mainThread.start();
    }

    public static void sendBroadcastMessage(Message message){
        for(String name: connectionMap.keySet()){
            try {
                connectionMap.get(name).send(message);
            }
            catch (IOException e){
                logger.error("An error occurred while communicating with the remote address {}",
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
            logger.info("A new connection has been established at {}", socket.getRemoteSocketAddress());
            String userName = null;
            try(Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUserAboutOthers(connection, userName);
                serverMainLoop(connection, userName);
            } catch (IOException | ClassNotFoundException e) {
                logger.error("An error occurred while communicating with the remote address {}. Error's message: {}",
                        socket.getRemoteSocketAddress(), e.getMessage());
            }
            if(userName != null){
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }
            logger.info("The connection with the remote address {} was closed", socket.getRemoteSocketAddress());

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
            while(true){
                Message message = connection.receive();
                if(message.getType() == MessageType.TEXT) {
                    message = new Message(MessageType.TEXT, String.format("%s: %s", userName, message.getText()));
                    sendBroadcastMessage(message);
                }
                else logger.error("An error occurred. The type of the message isn't text.");
            }
        }
    }

}
