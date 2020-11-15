import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class ChatServer {

    private static DatagramSocket socket = null;
    private static InetAddress inetAddress = null;

    //users
    public static HashMap<String, String> users = new HashMap<String, String>();

    public static void main(String... args) throws IOException {

        socket = new DatagramSocket();
        inetAddress = InetAddress.getByName("127.0.0.1");

        try (DatagramSocket sock = new DatagramSocket(10100)) {

            byte[] buf = null;
            DatagramPacket datagramPacket = null;

            String message = null;

            System.out.println("Server is running...");

            while (true) {

                buf = new byte[256];
                datagramPacket = new DatagramPacket(buf, buf.length);

                // wait until receive
                sock.receive(datagramPacket);

                // convert message
                message = new String(buf).trim();
                System.out.println(message);


                if (message.matches("^//port:.*")) {
                    // if user coming
                    String[] userdata = message.substring(7).split("\\|"); // get port(0) and username(1) using regular expression
                    users.put(userdata[0], userdata[1]); // set user data
                    sendMessage(userdata[1] + " enter the chat!");
                } else if (message.matches("^//close:.*")) {
                    // if user leaving
                    String[] userdata = message.substring(8).split("\\|"); // get userdata (port: [0], username: [1])
                    users.remove(userdata[0]); // using port number
                    sendMessage(userdata[1] + " leave the chat!");
                } else {
                    // send message to users
                    sendMessage(message);
                }
            }
        }
    }

    //send message to all clients
    private static void sendMessage(String message) {
        // convert to binary
        byte[] buf = ("users=" + String.join("|", users.values()) + "&msg=" + message + " ").getBytes();
        users.keySet().forEach(key -> {
            try {
                socket.send(new DatagramPacket(buf, buf.length, inetAddress, Integer.parseInt(key)));
            } catch (IOException e) {
                System.out.println("Failed to send message!");
            }
        });
    }
}