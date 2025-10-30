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
        outToClient1.flush();
        String user1 = inFromClient1.readLine();
        outToClient1.writeBytes("Password: ");
        outToClient1.flush();
        String pass1 = inFromClient1.readLine();

        if(!user1.equals("alice") || !pass1.equals("pass1")) {
            outToClient1.writeBytes("Login failed\n");
            outToClient1.flush();
            client1Socket.close();
            return;
        }
        outToClient1.writeBytes("Login successful! Waiting for second client...\n");
        outToClient1.flush();
        System.out.println(user1 + " logged in (online)");

        // Accept second client
        Socket client2Socket = welcomeSocket.accept();
        BufferedReader inFromClient2 = new BufferedReader(new InputStreamReader(client2Socket.getInputStream()));
        DataOutputStream outToClient2 = new DataOutputStream(client2Socket.getOutputStream());

        // Authenticate client 2
        outToClient2.writeBytes("Username: ");
        outToClient2.flush();
        String user2 = inFromClient2.readLine();
        outToClient2.writeBytes("Password: ");
        outToClient2.flush();
        String pass2 = inFromClient2.readLine();

        if(!user2.equals("bob") || !pass2.equals("pass2")) {
            outToClient2.writeBytes("Login failed\n");
            outToClient2.flush();
            client2Socket.close();
            return;
        }
        outToClient2.writeBytes("Login successful! Both clients connected.\n");
        outToClient2.flush();
        outToClient1.writeBytes("Second client connected! You can chat now.\n");
        outToClient1.flush();
        System.out.println(user2 + " logged in (online)");
        System.out.println("Both clients online: " + user1 + ", " + user2);

        // Thread to relay messages from client 1 to client 2
        Thread client1Listener = new Thread(() -> {
            try {
                while(true) {
                    String msg1 = inFromClient1.readLine();
                    if(msg1 == null) break;

                    if(msg1.startsWith("FILE:")) {
                        String[] parts = msg1.split(":");
                        String filename = parts[1];
                        int filesize = Integer.parseInt(parts[2]);
                        outToClient2.writeBytes("FILE:" + filename + ":" + filesize + "\n");
                        outToClient2.flush();
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
                        outToClient2.flush();
                    }
                }
            } catch (IOException e) {
                System.out.println("Client 1 disconnected");
            }
        });

        // Thread to relay messages from client 2 to client 1
        Thread client2Listener = new Thread(() -> {
            try {
                while(true) {
                    String msg2 = inFromClient2.readLine();
                    if(msg2 == null) break;

                    if(msg2.startsWith("FILE:")) {
                        String[] parts = msg2.split(":");
                        String filename = parts[1];
                        int filesize = Integer.parseInt(parts[2]);
                        outToClient1.writeBytes("FILE:" + filename + ":" + filesize + "\n");
                        outToClient1.flush();
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
                        outToClient1.flush();
                    }
                }
            } catch (IOException e) {
                System.out.println("Client 2 disconnected");
            }
        });

        // Start both listener threads
        client1Listener.start();
        client2Listener.start();

        // Wait for both to finish
        client1Listener.join();
        client2Listener.join();

        client1Socket.close();
        client2Socket.close();
        welcomeSocket.close();
        System.out.println("Server shutting down");
    }
}
