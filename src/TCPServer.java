import java.io.*;
import java.net.*;

class TCPServer {
    private static volatile Socket client1Socket = null;
    private static volatile Socket client2Socket = null;
    private static volatile DataInputStream inFromClient1 = null;
    private static volatile DataInputStream inFromClient2 = null;
    private static volatile DataOutputStream outToClient1 = null;
    private static volatile DataOutputStream outToClient2 = null;
    private static final Object client1Lock = new Object();
    private static final Object client2Lock = new Object();

    private static String readLine(DataInputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = in.read()) != -1) {
            if (ch == '\n') break;
            if (ch != '\r') sb.append((char) ch);
        }
        if (ch == -1 && sb.length() == 0) return null;
        return sb.toString();
    }

    public static void main(String argv[]) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(6789);
        System.out.println("Server started. Waiting for clients...");

        // Thread to handle client1 (alice)
        Thread client1Handler = new Thread(() -> handleClient1());
        client1Handler.start();

        // Thread to handle client2 (bob)
        Thread client2Handler = new Thread(() -> handleClient2());
        client2Handler.start();

        // Main loop to accept connections
        while(true) {
            Socket newSocket = welcomeSocket.accept();
            DataInputStream inFromNew = new DataInputStream(newSocket.getInputStream());
            DataOutputStream outToNew = new DataOutputStream(newSocket.getOutputStream());

            outToNew.writeBytes("Username: ");
            outToNew.flush();
            String username = readLine(inFromNew);
            outToNew.writeBytes("Password: ");
            outToNew.flush();
            String password = readLine(inFromNew);

            if(username.equals("alice") && password.equals("pass1")) {
                synchronized(client1Lock) {
                    if(client1Socket != null) {
                        try { client1Socket.close(); } catch(IOException e) {}
                    }
                    client1Socket = newSocket;
                    inFromClient1 = inFromNew;
                    outToClient1 = outToNew;
                    outToClient1.writeBytes("Login successful!\n");
                    outToClient1.flush();
                    System.out.println("alice logged in (online)");

                    synchronized(client2Lock) {
                        if(client2Socket != null && outToClient2 != null) {
                            try {
                                outToClient2.writeBytes("alice reconnected.\n");
                                outToClient2.flush();
                            } catch(IOException e) {}
                        }
                    }
                }
            } else if(username.equals("bob") && password.equals("pass2")) {
                synchronized(client2Lock) {
                    if(client2Socket != null) {
                        try { client2Socket.close(); } catch(IOException e) {}
                    }
                    client2Socket = newSocket;
                    inFromClient2 = inFromNew;
                    outToClient2 = outToNew;
                    outToClient2.writeBytes("Login successful!\n");
                    outToClient2.flush();
                    System.out.println("bob logged in (online)");

                    synchronized(client1Lock) {
                        if(client1Socket != null && outToClient1 != null) {
                            try {
                                outToClient1.writeBytes("bob reconnected.\n");
                                outToClient1.flush();
                            } catch(IOException e) {}
                        }
                    }
                }
            } else {
                outToNew.writeBytes("Login failed\n");
                outToNew.flush();
                newSocket.close();
            }
        }
    }

    private static void handleClient1() {
        while(true) {
            try {
                Socket socket;
                DataInputStream reader;
                synchronized(client1Lock) {
                    socket = client1Socket;
                    reader = inFromClient1;
                }

                if(socket == null || reader == null) {
                    Thread.sleep(100);
                    continue;
                }

                String msg = readLine(reader);
                if(msg == null) {
                    System.out.println("alice disconnected (offline)");
                    synchronized(client1Lock) {
                        try { if(client1Socket != null) client1Socket.close(); } catch(IOException e) {}
                        client1Socket = null;
                        inFromClient1 = null;
                        outToClient1 = null;
                    }
                    synchronized(client2Lock) {
                        if(outToClient2 != null) {
                            try {
                                outToClient2.writeBytes("alice disconnected.\n");
                                outToClient2.flush();
                            } catch(IOException e) {}
                        }
                    }
                    continue;
                }

                if(msg.startsWith("FILE:")) {
                    String[] parts = msg.split(":");
                    String filename = parts[1];
                    int filesize = Integer.parseInt(parts[2]);

                    // Read the file bytes from client using the same DataInputStream
                    byte[] fileData = new byte[filesize];
                    reader.readFully(fileData);

                    synchronized(client2Lock) {
                        if(outToClient2 != null && client2Socket != null) {
                            outToClient2.writeBytes("FILE:" + filename + ":" + filesize + "\n");
                            outToClient2.flush();
                            client2Socket.getOutputStream().write(fileData);
                            client2Socket.getOutputStream().flush();
                            System.out.println("File transferred: " + filename);
                        }
                    }
                } else {
                    synchronized(client2Lock) {
                        if(outToClient2 != null) {
                            outToClient2.writeBytes("[alice]: " + msg + "\n");
                            outToClient2.flush();
                        }
                    }
                }
            } catch(IOException e) {
                System.out.println("alice disconnected (offline)");
                synchronized(client1Lock) {
                    try { if(client1Socket != null) client1Socket.close(); } catch(IOException ex) {}
                    client1Socket = null;
                    inFromClient1 = null;
                    outToClient1 = null;
                }
                synchronized(client2Lock) {
                    if(outToClient2 != null) {
                        try {
                            outToClient2.writeBytes("alice disconnected.\n");
                            outToClient2.flush();
                        } catch(IOException ex) {}
                    }
                }
            } catch(InterruptedException e) {
                break;
            }
        }
    }

    private static void handleClient2() {
        while(true) {
            try {
                Socket socket;
                DataInputStream reader;
                synchronized(client2Lock) {
                    socket = client2Socket;
                    reader = inFromClient2;
                }

                if(socket == null || reader == null) {
                    Thread.sleep(100);
                    continue;
                }

                String msg = readLine(reader);
                if(msg == null) {
                    System.out.println("bob disconnected (offline)");
                    synchronized(client2Lock) {
                        try { if(client2Socket != null) client2Socket.close(); } catch(IOException e) {}
                        client2Socket = null;
                        inFromClient2 = null;
                        outToClient2 = null;
                    }
                    synchronized(client1Lock) {
                        if(outToClient1 != null) {
                            try {
                                outToClient1.writeBytes("bob disconnected.\n");
                                outToClient1.flush();
                            } catch(IOException e) {}
                        }
                    }
                    continue;
                }

                if(msg.startsWith("FILE:")) {
                    String[] parts = msg.split(":");
                    String filename = parts[1];
                    int filesize = Integer.parseInt(parts[2]);

                    // Read the file bytes from client using the same DataInputStream
                    byte[] fileData = new byte[filesize];
                    reader.readFully(fileData);

                    synchronized(client1Lock) {
                        if(outToClient1 != null && client1Socket != null) {
                            outToClient1.writeBytes("FILE:" + filename + ":" + filesize + "\n");
                            outToClient1.flush();
                            client1Socket.getOutputStream().write(fileData);
                            client1Socket.getOutputStream().flush();
                            System.out.println("File transferred: " + filename);
                        }
                    }
                } else {
                    synchronized(client1Lock) {
                        if(outToClient1 != null) {
                            outToClient1.writeBytes("[bob]: " + msg + "\n");
                            outToClient1.flush();
                        }
                    }
                }
            } catch(IOException e) {
                System.out.println("bob disconnected (offline)");
                synchronized(client2Lock) {
                    try { if(client2Socket != null) client2Socket.close(); } catch(IOException ex) {}
                    client2Socket = null;
                    inFromClient2 = null;
                    outToClient2 = null;
                }
                synchronized(client1Lock) {
                    if(outToClient1 != null) {
                        try {
                            outToClient1.writeBytes("bob disconnected.\n");
                            outToClient1.flush();
                        } catch(IOException ex) {}
                    }
                }
            } catch(InterruptedException e) {
                break;
            }
        }
    }
}