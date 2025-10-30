import java.io.*;
import java.net.*;

class UDPClient {
    private static volatile boolean isRunning = true;
    private static DatagramSocket clientSocket;
    private static InetAddress serverAddress;
    private static int serverPort = 9876;

    public static void main(String args[]) throws Exception {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        clientSocket = new DatagramSocket();
        serverAddress = InetAddress.getByName("localhost");

        System.out.print("Username: ");
        String username = inFromUser.readLine();
        sendMessage(username);

        System.out.print("Password: ");
        String password = inFromUser.readLine();
        sendMessage(password);

        String loginResult = receiveMessage();
        System.out.println(loginResult);

        if (loginResult.contains("failed")) {
            clientSocket.close();
            return;
        }

        System.out.println("Type messages to chat. Type 'FILE:filename' to send a file.");

        Thread serverListener = new Thread(() -> {
            try {
                while (isRunning) {
                    String incoming = receiveMessage();
                    if (incoming == null) {
                        System.out.println("Connection lost");
                        isRunning = false;
                        break;
                    }

                    if (incoming.startsWith("FILE:")) {
                        String[] parts = incoming.split(":");
                        String filename = parts[1];
                        String content = parts.length > 2 ? parts[2] : "";
                        System.out.println("Received file: " + filename);
                        System.out.println("File content: " + content);
                    } else {
                        System.out.println(incoming);
                    }
                }
            } catch (Exception e) {
                if (isRunning) {
                    System.out.println("Error reading from server: " + e.getMessage());
                }
            }
        });

        Thread userInputHandler = new Thread(() -> {
            try {
                while (isRunning) {
                    String message = inFromUser.readLine();
                    if (message == null) {
                        isRunning = false;
                        break;
                    }

                    if (message.startsWith("FILE:")) {
                        String filename = message.substring(5).trim();
                        File file = new File(filename);
                        if (file.exists()) {
                            BufferedReader fileReader = new BufferedReader(new FileReader(file));
                            StringBuilder content = new StringBuilder();
                            String line;
                            while ((line = fileReader.readLine()) != null) {
                                content.append(line);
                            }
                            fileReader.close();
                            sendMessage("FILE:" + file.getName() + ":" + content.toString());
                            System.out.println("File sent: " + filename);
                        } else {
                            System.out.println("File not found: " + filename);
                        }
                    } else {
                        sendMessage(message);
                    }
                }
            } catch (Exception e) {
                if (isRunning) {
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

    private static void sendMessage(String message) throws IOException {
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
        clientSocket.send(sendPacket);
    }

    private static String receiveMessage() throws IOException {
        byte[] receiveData = new byte[65507];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        return new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
    }
}