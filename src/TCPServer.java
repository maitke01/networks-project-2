import java.io.*;
import java.net.*;

class TCPServer {
    private static volatile Socket client1Socket = null;
    private static volatile Socket client2Socket = null;
    private static volatile BufferedReader inFromClient1 = null;
    private static volatile BufferedReader inFromClient2 = null;
    private static volatile DataOutputStream outToClient1 = null;
    private static volatile DataOutputStream outToClient2 = null;
    private static final Object client1Lock = new Object();
    private static final Object client2Lock = new Object();

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
            BufferedReader inFromNew = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
            DataOutputStream outToNew = new DataOutputStream(newSocket.getOutputStream());

            outToNew.writeBytes("Username: ");
            outToNew.flush();
            String username = inFromNew.readLine();
            outToNew.writeBytes("Password: ");
            outToNew.flush();
            String password = inFromNew.readLine();

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
                BufferedReader reader;
                synchronized(client1Lock) {
                    socket = client1Socket;
                    reader = inFromClient1;
                }

                if(socket == null || reader == null) {
                    Thread.sleep(100);
                    continue;
                }

                String msg = reader.readLine();
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

                    synchronized(client2Lock) {
                        if(outToClient2 != null && client2Socket != null) {
                            outToClient2.writeBytes("FILE:" + filename + ":" + filesize + "\n");
                            outToClient2.flush();

                            byte[] buffer = new byte[4096];
                            int read, total = 0;
                            while(total < filesize) {
                                read = socket.getInputStream().read(buffer, 0, Math.min(buffer.length, filesize - total));
                                if(read == -1) break;
                                client2Socket.getOutputStream().write(buffer, 0, read);
                                total += read;
                            }
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
                BufferedReader reader;
                synchronized(client2Lock) {
                    socket = client2Socket;
                    reader = inFromClient2;
                }

                if(socket == null || reader == null) {
                    Thread.sleep(100);
                    continue;
                }

                String msg = reader.readLine();
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

                    synchronized(client1Lock) {
                        if(outToClient1 != null && client1Socket != null) {
                            outToClient1.writeBytes("FILE:" + filename + ":" + filesize + "\n");
                            outToClient1.flush();

                            byte[] buffer = new byte[4096];
                            int read, total = 0;
                            while(total < filesize) {
                                read = socket.getInputStream().read(buffer, 0, Math.min(buffer.length, filesize - total));
                                if(read == -1) break;
                                client1Socket.getOutputStream().write(buffer, 0, read);
                                total += read;
                            }
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