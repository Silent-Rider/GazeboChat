package com.client;

import com.utility.Connection;
import com.utility.Message;
import com.utility.MessageType;

import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

class Client{
    private volatile static Connection connection;
    private static String newMessage;
    private static String userName;
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
//                    logger.error("Unknown interrupted exception before restarting a client");
                }
            }
    }

    private static void launch(){
        new Thread(ClientGUI::launch).start();
        synchronized (port){
            try {
                port.wait();
            } catch (InterruptedException e){
//                logger.error("Unknown interrupted exception before launching a client");
            }
        }
        new SocketThread(IP.toString(), port.get()).start();
    }

    static void sendTextMessage(String text){
        try {
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException e) {
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
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                if(connection != null && connection.isClosedIntentionally()) return;
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
                    default: throw new IOException("Unexpected message type");
                }
            }
        }

        private void clientMainLoop() throws IOException, ClassNotFoundException{
            while(true){
                Message message = connection.receive();
                if(message.getType() == null){
                    throw new IOException("Unexpected message type");
                }
                switch(message.getType()){
                    case TEXT: processIncomingMessage(message.getText()); break;
                    case USER_ADDED: informAboutAddingNewUser(message.getText()); break;
                    case USER_REMOVED: informAboutDeletingNewUser(message.getText()); break;
                    default:
                        throw new IOException("Unexpected message type");
                }
            }
        }

    }

}
