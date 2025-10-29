import java.io.*;
import java.net.*;
class TCPServer {
    public static void main(String argv[]) throws Exception
    {
        ServerSocket welcomeSocket = new ServerSocket(6789);
        System.out.println("Server started. Waiting for clients...");

        // Accept first client
        Socket client1Socket = welcomeSocket.accept();
        BufferedReader inFromClient1 = new BufferedReader(new InputStreamReader(client1Socket.getInputStream()));
        DataOutputStream outToClient1 = new DataOutputStream(client1Socket.getOutputStream());

        // Authenticate client 1
        outToClient1.writeBytes("Username: ");
        String user1 = inFromClient1.readLine();
        outToClient1.writeBytes("Password: ");
        String pass1 = inFromClient1.readLine();

        if(!user1.equals("alice") || !pass1.equals("pass1")) {
            outToClient1.writeBytes("Login failed\n");
            client1Socket.close();
            return;
        }
        outToClient1.writeBytes("Login successful! Waiting for second client...\n");
        System.out.println(user1 + " logged in (online)");

        // Accept second client
        Socket client2Socket = welcomeSocket.accept();
        BufferedReader inFromClient2 = new BufferedReader(new InputStreamReader(client2Socket.getInputStream()));
        DataOutputStream outToClient2 = new DataOutputStream(client2Socket.getOutputStream());

        // Authenticate client 2
        outToClient2.writeBytes("Username: ");
        String user2 = inFromClient2.readLine();
        outToClient2.writeBytes("Password: ");
        String pass2 = inFromClient2.readLine();

        if(!user2.equals("bob") || !pass2.equals("pass2")) {
            outToClient2.writeBytes("Login failed\n");
            client2Socket.close();
            return;
        }
        outToClient2.writeBytes("Login successful! Both clients connected.\n");
        outToClient1.writeBytes("Second client connected! You can chat now.\n");
        System.out.println(user2 + " logged in (online)");
        System.out.println("Both clients online: " + user1 + ", " + user2);

        // Relay messages between clients
        while(true) {
            String msg1 = inFromClient1.readLine();
            if(msg1 != null) {
                if(msg1.startsWith("FILE:")) {
                    String[] parts = msg1.split(":");
                    String filename = parts[1];
                    int filesize = Integer.parseInt(parts[2]);
                    outToClient2.writeBytes("FILE:" + filename + ":" + filesize + "\n");
                    byte[] buffer = new byte[4096];
                    int read, total = 0;
                    while(total < filesize) {
                        read = client1Socket.getInputStream().read(buffer, 0, Math.min(buffer.length, filesize - total));
                        if(read == -1) break;
                        client2Socket.getOutputStream().write(buffer, 0, read);
                        total += read;
                    }
                    System.out.println("File transferred: " + filename);
                } else {
                    outToClient2.writeBytes("[" + user1 + "]: " + msg1 + "\n");
                }
            }

            String msg2 = inFromClient2.readLine();
            if(msg2 != null) {
                if(msg2.startsWith("FILE:")) {
                    String[] parts = msg2.split(":");
                    String filename = parts[1];
                    int filesize = Integer.parseInt(parts[2]);
                    outToClient1.writeBytes("FILE:" + filename + ":" + filesize + "\n");
                    byte[] buffer = new byte[4096];
                    int read, total = 0;
                    while(total < filesize) {
                        read = client2Socket.getInputStream().read(buffer, 0, Math.min(buffer.length, filesize - total));
                        if(read == -1) break;
                        client1Socket.getOutputStream().write(buffer, 0, read);
                        total += read;
                    }
                    System.out.println("File transferred: " + filename);
                } else {
                    outToClient1.writeBytes("[" + user2 + "]: " + msg2 + "\n");
                }
            }
        }
    }
}
