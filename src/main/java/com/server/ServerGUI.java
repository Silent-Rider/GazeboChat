package com.server;

import com.utility.Connection;
import org.apache.logging.log4j.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ServerGUI {

    private static final Logger serverLogger = Server.serverLogger;
    private static JFrame menu = new JFrame();
    private static JFrame program = new JFrame();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static LocalTime startTime;
    private static final String USERS_COUNT = "Пользователей: %d";

    private static final int WIDTH = 1100;
    private static final int HEIGHT = 700;

    static{
        initFrame(menu, WIDTH, HEIGHT);
        initFrame(program, WIDTH-600, HEIGHT-400);
    }

    private static void initFrame(JFrame frame, int width, int height){
        frame.setTitle("GazeboServer");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setSize(new Dimension(width, height));
        frame.setLocationRelativeTo(null);
    }

    private static void adjustMenu(){
        Container container = menu.getContentPane();
        container.setBackground(new Color(198,255,198));
        container.setLayout(null);

        JLabel picture = null;
        try{
            FileInputStream imageInput = new FileInputStream("utility/server_label.jpg");
            picture = new JLabel(new ImageIcon(ImageIO.read(imageInput)));
        } catch (IOException e) {
            serverLogger.error("Cannot find \"server_label.jpg\"");
        }
        if(picture != null) picture.setBounds(300,15, 270, 270);

        JLabel title = new JLabel("Server");
        title.setFont(new Font("Constantia", Font.BOLD, 70));
        title.setBounds(590, 110, 300, 100);

        JLabel port = new JLabel("Введите порт сервера");
        port.setFont(new Font("Segoe Print", Font.BOLD, 23));
        port.setBounds(420, 315, 400, 30);

        JTextField portText = new JTextField();
        portText.setBounds(300, 350, 500, 50);
        portText.setFont(new Font("Times New Roman", Font.BOLD, 30));

        JButton launch = getLaunchButton(portText);

        JButton about = new JButton("О программе");
        about.setBounds(480, 600, 150, 35);
        about.setFont(new Font("Tahoma", Font.PLAIN, 17));
        about.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "Программа была разработана Silent Rider'ом", "О программе",
                JOptionPane.INFORMATION_MESSAGE));

        fillContainer(container, picture, title, port, portText, launch, about);
    }

    private static void adjustProgram(){
        Container container = program.getContentPane();
        container.setLayout(null);
        container.setBackground(new Color(179,179,217));

        JLabel title = new JLabel("Сервер запущен");
        title.setFont(new Font("Segoe Print", Font.BOLD, 25));
        title.setBounds(140, 45, 400, 35);

        JLabel time = new JLabel(FORMATTER.format(startTime));
        time.setFont(new Font("Times New Roman", Font.BOLD, 25));
        time.setBounds(200, 90, 200, 30);

        JLabel usersCount = new JLabel(String.format(USERS_COUNT, 0));
        usersCount.setFont(new Font("Times New Roman", Font.BOLD, 25));
        usersCount.setBounds(150, 130, 200, 30);

        Timer timer = timer(time, usersCount);
        JButton stop = getStopButton(timer);

        fillContainer(container, title, time, usersCount, stop);
        timer.start();
    }

    private static JButton getStopButton(Timer timer) {
        JButton stop = new JButton("Остановить сервер");
        stop.setFont(new Font("Tahoma", Font.BOLD, 25));
        stop.setBackground(new Color(255,83,83));
        stop.setBounds(85, 185, 330, 50);
        stop.addActionListener(e -> {
            try {
                Server.close();
            } catch (IOException exc){
                serverLogger.error("An IO error occurred while attempting to close server socket");
            }
            startTime = LocalTime.MIN;
            timer.stop();
            Server.connectionMap.clear();
            synchronized (Server.restartLock){
                Server.restartLock.notify();
            }
        });
        return stop;
    }

    private static JButton getLaunchButton(JTextField portText) {
        JButton launch = new JButton("Запустить сервер");
        launch.setBounds(380, 415, 330, 50);
        launch.setFont(new Font("Tahoma", Font.BOLD, 30));
        launch.setBackground(new Color(0,219,219));
        launch.addActionListener(e -> {
            if(Connection.checkServerPort(portText.getText())) {
                JOptionPane.showMessageDialog(null, "Введён некорректный порт сервера",
                        "Некорректный порт", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int port = Integer.parseInt(portText.getText());
            startTime = LocalTime.MIN;
            mainFrame();
            synchronized (Server.port){
                Server.port.set(port);
                Server.port.notify();
            }
        });
        return launch;
    }

    private static void mainFrame(){
        menu.setVisible(false);
        menu.dispose();
        menu = new JFrame();
        initFrame(menu, WIDTH, HEIGHT);
        adjustProgram();
        program.setVisible(true);
    }

    private static void fillContainer(Container container, JComponent... components){
        for(var component: components)
            container.add(component);
    }

    static void start(){
        program.setVisible(false);
        program.dispose();
        program = new JFrame();
        initFrame(program, WIDTH-600, HEIGHT-400);
        adjustMenu();
        menu.setVisible(true);
    }

    private static Timer timer(JLabel time, JLabel usersCount) {
       return new Timer(1000, e -> {
            LocalTime newTime = startTime.plusSeconds(1);
            time.setText(FORMATTER.format(newTime));
            startTime = newTime;
            usersCount.setText(String.format(USERS_COUNT, Server.connectionMap.size()));
        });
    }
}
