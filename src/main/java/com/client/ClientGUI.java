package com.client;

import com.utility.Connection;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;

public class ClientGUI {

    private static final Logger clientLogger = Client.clientLogger;
    private static final JTextField textField = new JTextField();
    private static final JTextArea messages = new JTextArea();
    private static final JTextArea users = new JTextArea();
    private static JFrame menu = new JFrame();
    private static JFrame program = new JFrame();

    private static final int WIDTH = 1100;
    private static final int HEIGHT = 700;

    static{
        initFrame(menu);
        initFrame(program);
    }

    private static void initFrame(JFrame frame){
        frame.setTitle("GazeboChat");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setSize(new Dimension(WIDTH, HEIGHT));
        frame.setLocationRelativeTo(null);
    }

    private static void adjustMenu(){
        Container container = menu.getContentPane();
        container.setBackground(new Color(198,255,198));
        container.setLayout(null);

        JLabel picture = null;
        try{
            FileInputStream imageInput = new FileInputStream("utility/client_label.png");
            picture = new JLabel(new ImageIcon(ImageIO.read(imageInput)));
        } catch (IOException e) {
            clientLogger.error("Cannot find \"client_label.png\"");
        }
        if(picture != null) picture.setBounds(300,5, 512, 331);

        JLabel IP = new JLabel("Введите IP-адрес сервера");
        IP.setFont(new Font("Segoe Print", Font.BOLD, 23));
        IP.setBounds(170, 360, 320, 30);

        JTextField IPText = new JTextField();
        IPText.setBounds(500, 360, 370, 40);
        IPText.setFont(new Font("Times New Roman", Font.BOLD, 30));

        JLabel port = new JLabel("Введите порт сервера");
        port.setFont(new Font("Segoe Print", Font.BOLD, 23));
        port.setBounds(170, 420, 300, 30);

        JTextField portText = new JTextField();
        portText.setBounds(500, 420, 370, 40);
        portText.setFont(new Font("Times New Roman", Font.BOLD, 30));

        JButton connect = getConnectButton(IPText, portText);

        JButton about = new JButton("О программе");
        about.setBounds(480, 600, 150, 35);
        about.setFont(new Font("Tahoma", Font.PLAIN, 17));
        about.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "Программа была разработана Silent Rider'ом", "О программе",
                JOptionPane.INFORMATION_MESSAGE));

        fillContainer(container, picture, IP, IPText, port, portText, connect, about);
    }

    private static void adjustProgram(){
        Container container = program.getContentPane();
        container.setLayout(null);

        textField.setEditable(false);
        messages.setEditable(false);
        users.setEditable(false);

        users.setFont(new Font("Constantia", Font.BOLD, 30));
        users.setBounds(820, 43, 260, 550);
        users.setBackground(new Color(255,216,176));

        messages.setFont(new Font("Times New Roman", Font.PLAIN, 25));
        JScrollPane messagesScroll = new JScrollPane(messages);
        messagesScroll.setBounds(5, 10, 805, 585);

        textField.setFont(new Font("Times New Roman", Font.PLAIN, 25));
        textField.setBounds(5, 603, 625, 55);
        textField.setBorder(new LineBorder(Color.BLACK, 2));
        textField.addActionListener(e -> {
            Client.sendTextMessage(textField.getText());
            textField.setText("");
        });

        JLabel usersTitle = new JLabel("Участники чата");
        usersTitle.setFont(new Font("Segoe Print", Font.BOLD, 25));
        usersTitle.setBounds(840, 5, 250, 30);

        JButton reset = getResetButton();

        JButton send = new JButton("Отправить");
        send.setFont(new Font("Segoe Print", Font.BOLD, 21));
        send.setBackground(new Color(145,255,34));
        send.setBounds(640, 603, 170, 55);
        send.addActionListener(e -> {
            Client.sendTextMessage(textField.getText());
            textField.setText("");
        });

        fillContainer(container, textField, messagesScroll, users, usersTitle, reset, send);
    }

    private static JButton getResetButton() {
        JButton reset = new JButton("Главное меню");
        reset.setFont(new Font("Segoe Print", Font.BOLD, 23));
        reset.setBackground(new Color(255,83,83));
        reset.setBounds(845, 603, 215, 55);
        reset.addActionListener(e -> {
            try {
                if(Client.getConnection() != null)
                    Client.getConnection().close();
            } catch (IOException exc){
                clientLogger.error("An IO error occurred while attempting to close socket");
            }
            Client.allUserNames.remove(Client.getUserName());
            Client.IP.setLength(0);
            synchronized (Client.restartLock){
                Client.restartLock.notify();
            }
        });
        return reset;
    }

    private static JButton getConnectButton(JTextField IPText, JTextField portText) {
        JButton connect = new JButton("Подключиться");
        connect.setBounds(400, 500, 300, 50);
        connect.setFont(new Font("Tahoma", Font.BOLD, 30));
        connect.setBackground(new Color(123,244,43));
        connect.addActionListener(e -> {
            if(!Connection.checkServerAddress(IPText.getText())) {
                JOptionPane.showMessageDialog(null, "Введён некорректный IP-адрес сервера",
                        "Некорректный IP-адрес", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String IP = IPText.getText();
            if(Connection.checkServerPort(portText.getText())) {
                JOptionPane.showMessageDialog(null, "Введён некорректный порт сервера",
                        "Некорректный порт", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int port = Integer.parseInt(portText.getText());
            synchronized (Client.port){
                Client.port.set(port);
                Client.IP.append(IP);
                Client.port.notify();
            }
        });
        return connect;
    }

    static String getUserName() {
        String name = JOptionPane.showInputDialog(menu,"Введите ваше имя:","Конфигурация клиента",
                JOptionPane.QUESTION_MESSAGE);
        if(name.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Имя не может быть пустым", "Пустое имя",
                    JOptionPane.INFORMATION_MESSAGE);
            name = getUserName();
        }
        else if(Client.allUserNames.contains(name)) {
            JOptionPane.showMessageDialog(null, "Данное имя уже занято", "Занятое имя",
                    JOptionPane.INFORMATION_MESSAGE);
            name = getUserName();
        }
        return name;
    }

    static void notifyConnectionStatusChanged(boolean clientConnected) {
        mainFrame();
        textField.setEditable(clientConnected);
        if (clientConnected) JOptionPane.showMessageDialog(null,"Соединение с сервером установлено",
                    "Gazebo", JOptionPane.INFORMATION_MESSAGE);
        else
            JOptionPane.showMessageDialog(null,"Произошла ошибка при подключении к серверу",
                    "Gazebo", JOptionPane.ERROR_MESSAGE);
    }

    static void refreshMessages() {
        messages.append(Client.getNewMessage() + "\n");
    }

    static void refreshUsers() {
        StringBuilder sb = new StringBuilder();
        for (String userName : Client.allUserNames)
            sb.append(userName).append("\n");
        users.setText(sb.toString());
    }

    private static void fillContainer(Container container, JComponent... components){
        for(var component: components)
            container.add(component);
    }

    private static void mainFrame(){
        menu.setVisible(false);
        menu.dispose();
        menu = new JFrame();
        initFrame(menu);
        adjustProgram();
        program.setVisible(true);
    }

    static void launch(){
        program.setVisible(false);
        program.dispose();
        program = new JFrame();
        initFrame(program);
        adjustMenu();
        menu.setVisible(true);
    }
}
