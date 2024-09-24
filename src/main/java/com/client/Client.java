package com.client;

import com.utility.Connection;
import com.utility.Message;
import com.utility.MessageType;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class Client{
    private volatile static Connection connection;
    private static final Set<String> allUserNames = new HashSet<>();
    private static String newMessage;

    static void launch(String IP, int port) {
        new SocketThread(IP, port).start();
    }

    static void sendTextMessage(String text){
        try {
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException e) {
        }
    }

    static Set<String> getAllUserNames() {
        return Collections.unmodifiableSet(allUserNames);
    }

    static String getNewMessage() {
        return newMessage;
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

    public static Connection getConnection() {
        return connection;
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
                    case NAME_REQUEST: String name = ClientGUI.getUserName();
                    connection.send(new Message(MessageType.USER_NAME, name));
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
