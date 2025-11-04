import java.io.*;
import java.net.*;

class TCPClient {
    private static volatile boolean isRunning = true;

    private static String readPrompt(BufferedReader reader) throws IOException {
        StringBuilder prompt = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            prompt.append((char) ch);
            if (ch == ' ' && prompt.toString().endsWith(": ")) {
                break;
            }
        }
        return prompt.toString();
    }

    public static void main(String argv[]) throws Exception {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket = new Socket("localhost", 6789);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        System.out.print(readPrompt(inFromServer));
        System.out.flush();
        String username = inFromUser.readLine();
        outToServer.writeBytes(username + '\n');
        outToServer.flush();

        System.out.print(readPrompt(inFromServer));
        System.out.flush();
        String password = inFromUser.readLine();
        outToServer.writeBytes(password + '\n');
        outToServer.flush();

        String loginResult = inFromServer.readLine();
        System.out.println(loginResult);

        if(loginResult.contains("failed")) {
            clientSocket.close();
            return;
        }

        if(loginResult.contains("Waiting for second client")) {
            String secondClientMsg = inFromServer.readLine();
            if(secondClientMsg != null) {
                System.out.println(secondClientMsg);
            }
        }

        System.out.println("Type messages to chat. Type 'FILE:filename' to send a file.");

        Thread serverListener = new Thread(() -> {
            try {
                while(isRunning) {
                    String incoming = inFromServer.readLine();
                    if(incoming == null) {
                        System.out.println("Connection closed by server");
                        isRunning = false;
                        break;
                    }

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
                        System.out.println(incoming);
                    }
                }
            } catch (IOException e) {
                if(isRunning) {
                    System.out.println("Error reading from server: " + e.getMessage());
                }
            }
        });

        Thread userInputHandler = new Thread(() -> {
            try {
                while(isRunning) {
                    String message = inFromUser.readLine();
                    if(message == null) {
                        isRunning = false;
                        break;
                    }

                    if(message.startsWith("FILE:")) {
                        String filename = message.substring(5).trim();
                        File file = new File(filename);
                        if(file.exists()) {
                            outToServer.writeBytes("FILE:" + file.getName() + ":" + file.length() + "\n");
                            outToServer.flush();
                            FileInputStream fis = new FileInputStream(file);
                            byte[] buffer = new byte[4096];
                            int read;
                            while((read = fis.read(buffer)) > 0) {
                                clientSocket.getOutputStream().write(buffer, 0, read);
                            }
                            clientSocket.getOutputStream().flush();
                            fis.close();
                            System.out.println("File sent: " + filename);
                        } else {
                            System.out.println("File not found: " + filename);
                        }
                    } else {
                        outToServer.writeBytes(message + '\n');
                        outToServer.flush();
                    }
                }
            } catch (IOException e) {
                if(isRunning) {
                    System.out.println("Error sending to server: " + e.getMessage());
                }
            }
        });

        serverListener.start();
        userInputHandler.start();

        serverListener.join();
        userInputHandler.join();

        clientSocket.close();
        System.out.println("Client disconnected");
    }
}