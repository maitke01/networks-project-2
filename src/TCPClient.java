import java.io.*;
import java.net.*;
class TCPClient {
    public static void main(String argv[]) throws Exception
    {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket = new Socket("localhost", 6789);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Login
        System.out.print(inFromServer.readLine());
        String username = inFromUser.readLine();
        outToServer.writeBytes(username + '\n');

        System.out.print(inFromServer.readLine());
        String password = inFromUser.readLine();
        outToServer.writeBytes(password + '\n');

        String loginResult = inFromServer.readLine();
        System.out.println(loginResult);

        if(loginResult.contains("failed")) {
            clientSocket.close();
            return;
        }

        // Check for second client message
        if(clientSocket.getInputStream().available() > 0) {
            System.out.println(inFromServer.readLine());
        }

        // Chat loop
        System.out.println("Type messages to chat. Type 'FILE:filename' to send a file.");
        while(true) {
            // Check for incoming messages
            if(clientSocket.getInputStream().available() > 0) {
                String incoming = inFromServer.readLine();
                if(incoming.startsWith("FILE:")) {
                    String[] parts = incoming.split(":");
                    String filename = parts[1];
                    int filesize = Integer.parseInt(parts[2]);
                    System.out.println("Receiving file: " + filename);

                    byte[] buffer = new byte[4096];
                    int read, total = 0;
                    String fileContent = "";
                    while(total < filesize) {
                        read = clientSocket.getInputStream().read(buffer, 0, Math.min(buffer.length, filesize - total));
                        if(read == -1) break;
                        fileContent += new String(buffer, 0, read);
                        total += read;
                    }
                    System.out.println("File content: " + fileContent);
                } else {
                    System.out.println("received: " + incoming);
                }
            }

            // Check for user input
            if(System.in.available() > 0) {
                String message = inFromUser.readLine();

                if(message.startsWith("FILE:")) {
                    String filename = message.substring(5);
                    File file = new File(filename);
                    if(file.exists()) {
                        outToServer.writeBytes("FILE:" + file.getName() + ":" + file.length() + "\n");
                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[4096];
                        int read;
                        while((read = fis.read(buffer)) > 0) {
                            clientSocket.getOutputStream().write(buffer, 0, read);
                        }
                        fis.close();
                        System.out.println("File sent: " + filename);
                    } else {
                        System.out.println("File not found");
                    }
                } else {
                    outToServer.writeBytes(message + '\n');
                }
            }

            Thread.sleep(100);
        }
    }
}