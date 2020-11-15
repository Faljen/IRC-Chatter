// To simply run multiple clients:
// In IntelliJ Run > Edit Configurations > Check "Allow parallel run" option

import java.io.IOException;
import java.awt.BorderLayout;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.BindException;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

class ClientForm extends JFrame {

    JTextArea textArea = null;
    public DefaultListModel<String> userList = null;

    public ClientForm(Consumer<String> send) {

        JTextField textField = new JTextField(15);
        textField.addActionListener(evt -> {
            send.accept(textField.getText());
            textField.setText("");
        });

        // text area to send messages
        textArea = new JTextArea(10, 30);
        textArea.setEditable(false);

        // users list
        userList = new DefaultListModel<String>();
        JList listPanel = new JList(userList);

        add(textField, BorderLayout.PAGE_END);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(new JScrollPane(listPanel), BorderLayout.EAST);

        pack();

        setVisible(true);
    }

    //append message to text area
    public void updateMessage(String msg) {
        textArea.append(msg + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    //add users to users list
    public void updateUser(String[] users) {
        userList.clear();
        Arrays.asList(users).forEach(user -> {
            userList.addElement(user);
        });
    }
}

public class ChatClient {

    private static DatagramSocket socket = null;
    private static InetAddress inetAddress = null;
    private static ClientForm clientForm = null;

    private static String username = null;
    private static String port = null;

    public static void main(String... args) throws IOException {

        initSendMessage();
        receiver();
    }

    private static void initSendMessage() throws IOException {

        socket = new DatagramSocket();
        inetAddress = InetAddress.getByName("127.0.0.1"); // host ip (localhost)

        // create client GUI
        clientForm = new ClientForm(text -> {
            sendMessage(text, false);

            // "> close": exit command
            if (text.equals("> close")) {
                System.out.println("!" + port);
                sendMessage("//close:" + port + "|" + username, true);
                socket.close(); // close socket
                System.exit(0); // end client
            }
        });
    }

    //send message to server
    private static void sendMessage(String message, boolean isCommand) {
        // convert to binary
        byte[] buf = ((isCommand == false ? username + ": " : "") + message + " ").getBytes();

        DatagramPacket datagramPacket = null;

        datagramPacket = new DatagramPacket(buf, buf.length, inetAddress, 10100);

        try {
            // send packet(message) to server
            socket.send(datagramPacket);
        } catch (IOException e) {
            System.out.println("Failed to send message!");
        }
    }

    //receive server messages
    private static void receiver() throws IOException {

        DatagramSocket sock = null;
        int startPortNumber = 10101;

        // get available port
        while (sock == null) {
            try {
                sock = new DatagramSocket(startPortNumber); // get socket with "available" port
                port = String.valueOf(startPortNumber);
            } catch (BindException be) { // if port is already in use
                startPortNumber++;
                continue;
            }
        }

        System.out.println("Socked opened on port: " + port);

        //get username
        do {
            username = JOptionPane.showInputDialog(null, "Choose your nickname");
        } while (username == null);

        sendMessage("//port:" + port + "|" + username, true);

        byte[] buf = null;
        DatagramPacket pack = null;

        String message = null;

        do {
            buf = new byte[256];
            pack = new DatagramPacket(buf, buf.length);

            sock.receive(pack);

            // convert message
            message = new String(buf).trim();
            System.out.println(message);

            // get datas
            String[] datas = message.split("\\&"); // [0]: users, [1]: msg
            String[] users = datas[0].substring(6).split("\\|");
            String msg = datas[1].substring(4);

            // update userlist
            clientForm.updateUser(users);

            // append message to text area
            clientForm.updateMessage(msg);
        } while (!message.equals("exit"));

        sock.close();
    }
}