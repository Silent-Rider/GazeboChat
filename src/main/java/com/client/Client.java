package com.client;

import com.utility.Connection;
import com.utility.Message;
import com.utility.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

class Client{
    private volatile static Connection connection;
    private static String newMessage;
    private static String userName;
    static final Logger clientLogger = LogManager.getLogger(Client.class);
    static final StringBuffer IP = new StringBuffer();
    static final AtomicInteger port = new AtomicInteger();
    static final Set<String> allUserNames = new CopyOnWriteArraySet<>();
    static final Object restartLock = new Object();

    public static void main(String[] args) {
        while(true)
            synchronized (restartLock){
                launch();
                try{
                    restartLock.wait();
                } catch (InterruptedException e){
                    clientLogger.error("Unknown interrupted exception before restarting a client");
                }
            }
    }

    private static void launch(){
        new Thread(ClientGUI::launch).start();
        synchronized (port){
            try {
                port.wait();
            } catch (InterruptedException e){
                clientLogger.error("Unknown interrupted exception before launching a client");
            }
        }
        new SocketThread(IP.toString(), port.get()).start();
    }

    static void sendTextMessage(String text){
        try {
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException e) {
            clientLogger.error("An error occurred while sending message to server");
        }
    }

    static String getNewMessage() {
        return newMessage;
    }

    static Connection getConnection() {
        return connection;
    }

    static String getUserName() {
        return userName;
    }

    static void setNewMessage(String newMessage) {
        Client.newMessage = newMessage;
    }

    static void addUser(String userName){
        allUserNames.add(userName);
    }

    static void deleteUser(String userName){
        allUserNames.remove(userName);
    }

    static class SocketThread extends Thread{

        private final String IP;
        private final int port;

        private SocketThread(String IP, int port){
            this.IP = IP;
            this.port = port;
        }
        @Override
        public void run(){
            try {
                Socket socket = new Socket(IP,port);
                clientLogger.info("A new connection with server has been established");
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            }
            catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
                clientLogger.info("The connection with server was closed");
                if(connection != null && connection.isClosedIntentionally()) return;
                if(e instanceof IllegalArgumentException) clientLogger.error("Cause of closure: {}", (e.getMessage()));
                notifyConnectionStatusChanged(false);
            }
        }

        private void processIncomingMessage(String message){
            setNewMessage(message);
            ClientGUI.refreshMessages();
        }

        private void informAboutAddingNewUser(String userName){
            addUser(userName);
            ClientGUI.refreshUsers();
        }

        private void informAboutDeletingNewUser(String userName){
            deleteUser(userName);
            ClientGUI.refreshUsers();
        }

        private void notifyConnectionStatusChanged(boolean clientConnected){
            ClientGUI.notifyConnectionStatusChanged(clientConnected);
        }

        private void clientHandshake() throws IOException, ClassNotFoundException{
            while(true){
                Message message = connection.receive();
                switch (message.getType()){
                    case NAME_REQUEST: userName = ClientGUI.getUserName();
                    connection.send(new Message(MessageType.USER_NAME, userName));
                    continue;
                    case NAME_ACCEPTED:
                        notifyConnectionStatusChanged(true);
                        return;
                    default: throw new IllegalArgumentException("Unexpected message type");
                }
            }
        }

        private void clientMainLoop() throws IOException, ClassNotFoundException{
            while(true){
                Message message = connection.receive();
                if(message.getType() == null){
                    throw new IllegalArgumentException("Unexpected message type");
                }
                switch(message.getType()){
                    case TEXT: processIncomingMessage(message.getText()); break;
                    case USER_ADDED: informAboutAddingNewUser(message.getText()); break;
                    case USER_REMOVED: informAboutDeletingNewUser(message.getText()); break;
                    default:
                        throw new IllegalArgumentException("Unexpected message type");
                }
            }
        }

    }

}
