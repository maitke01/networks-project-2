import java.io.*;
import java.net.*;

class UDPServer {
    private static DatagramSocket serverSocket;
    private static InetAddress client1Addr, client2Addr;
    private static int client1Port, client2Port;
    private static String user1, user2;

    public static void main(String args[]) throws Exception {
        serverSocket = new DatagramSocket(9876);
        System.out.println("Server started. Waiting for clients...");

        byte[] receiveData = new byte[65507];

        DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(packet);
        user1 = new String(packet.getData(), 0, packet.getLength()).trim();
        client1Addr = packet.getAddress();
        client1Port = packet.getPort();

        serverSocket.receive(packet);
        String pass1 = new String(packet.getData(), 0, packet.getLength()).trim();

        if (!user1.equals("alice") || !pass1.equals("pass1")) {
            sendMessage(client1Addr, client1Port, "Login failed");
            return;
        }
        sendMessage(client1Addr, client1Port, "Login successful! Waiting for second client...");
        System.out.println(user1 + " logged in (online)");

        serverSocket.receive(packet);
        user2 = new String(packet.getData(), 0, packet.getLength()).trim();
        client2Addr = packet.getAddress();
        client2Port = packet.getPort();

        serverSocket.receive(packet);
        String pass2 = new String(packet.getData(), 0, packet.getLength()).trim();

        if (!user2.equals("bob") || !pass2.equals("pass2")) {
            sendMessage(client2Addr, client2Port, "Login failed");
            return;
        }
        sendMessage(client2Addr, client2Port, "Login successful! Both clients connected.");
        sendMessage(client1Addr, client1Port, "Second client connected! You can chat now.");
        System.out.println(user2 + " logged in (online)");
        System.out.println("Both clients online: " + user1 + ", " + user2);

        while (true) {
            DatagramPacket pkt = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(pkt);
            String msg = new String(pkt.getData(), 0, pkt.getLength()).trim();
            InetAddress senderAddr = pkt.getAddress();
            int senderPort = pkt.getPort();

            if (senderAddr.equals(client1Addr) && senderPort == client1Port) {
                if (msg.startsWith("FILE:")) {
                    sendMessage(client2Addr, client2Port, msg);
                    System.out.println("File transferred from " + user1);
                } else {
                    sendMessage(client2Addr, client2Port, "[" + user1 + "]: " + msg);
                }
            } else if (senderAddr.equals(client2Addr) && senderPort == client2Port) {
                if (msg.startsWith("FILE:")) {
                    sendMessage(client1Addr, client1Port, msg);
                    System.out.println("File transferred from " + user2);
                } else {
                    sendMessage(client1Addr, client1Port, "[" + user2 + "]: " + msg);
                }
            }
        }
    }

    private static void sendMessage(InetAddress addr, int port, String msg) throws IOException {
        byte[] data = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
        serverSocket.send(packet);
    }
}