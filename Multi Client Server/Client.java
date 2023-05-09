import java.io.*;
import java.net.Socket;
import java.util.Scanner;
public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private boolean pinged;
    public Client(Socket socket, String username) {
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
            this.pinged = false;
        } catch (IOException e) {
            System.out.println("[SERVER] Error occurred ... closing connection");
            closeEverything(socket, bufferedWriter, bufferedReader);
        }
    }
    public void sendMessage() {
        try {
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();
                if (messageToSend.equals("exit")) {
                    bufferedWriter.write(username + " has left the chat.");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    closeEverything(socket, bufferedWriter, bufferedReader);
                    System.exit(0);
                } else if (messageToSend.startsWith("private")) {
                    String[] parts = messageToSend.split("\\s+", 3);
                    String recipient = parts[1];
                    String message = parts[2];
                    bufferedWriter.write("private " + recipient + " " + message);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                } else if (messageToSend.startsWith("PING")) {
                    String[] parts = messageToSend.split("\\s+");
                    if (parts.length < 2) {
                        System.out.println("Invalid input format. Correct format: PING <client_id>");
                        continue;
                    }
                    String recipientStr = parts[1];
                    int recipient = -1;
                    try {
                        recipient = Integer.parseInt(recipientStr);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid client ID: " + recipientStr);
                        continue;
                    }
                    String message = "You are pinged by the coordinator";
                    bufferedWriter.write("private " + recipient + " " + message);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    int finalRecipient = recipient;
                    Thread pingResponseThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(10000);
                                if (!pinged) {
                                    System.out.println("Client " + finalRecipient + " did not respond to PING in time. Removing from the server.");
                                    closeEverything(socket, new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), new BufferedReader(new InputStreamReader(socket.getInputStream())));
                                    return;
                                }
                                pinged = false;
                            } catch (InterruptedException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    pingResponseThread.start();
                    pingResponseThread.join();
                } else {
                    bufferedWriter.write(username + " : " + messageToSend);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Error occurred ... closing connection");
            closeEverything(socket,bufferedWriter, bufferedReader);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;
                while (socket.isConnected()) {
                    try {
                        msgFromGroupChat = bufferedReader.readLine();
                        System.out.println(msgFromGroupChat);
                        if (msgFromGroupChat.equals("PONG")) {
                            pinged = true;
                        }
                    } catch (IOException e) {
                        closeEverything(socket,bufferedWriter,bufferedReader);
                    }
                }
                pinged = false;
            }
        }).start();
    }
    public void closeEverything(Socket socket, BufferedWriter bufferedWriter, BufferedReader bufferedReader) {
        try {
            if(socket != null) {
                socket.close();
            }
            if(bufferedWriter != null) {
                bufferedWriter.close();
            }
            if(bufferedReader != null) {
                bufferedReader.close();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static int getNumber() {
        Scanner portGuess = new Scanner(System.in);
        int portNumber;
        while(true) {
            System.out.println("[SERVER] Enter the PORT number :");
            int number = Integer.parseInt(portGuess.nextLine());
            if (number != 2000) {
                System.out.println("[SERVER] There is no server listening on PORT : (" + number + "). Enter another one.");
            } else {
                portNumber = 2000;
                break;
            }
        } return portNumber;
    }
    public static void presentClients() throws IOException {
        for (ClientHandler clientHandler : ClientHandler.clientHandlers) {
            String messageToSend = clientHandler.bufferedReader.readLine();
            if(messageToSend.equals("WHO")) {
                System.out.println(ClientHandler.clientHandlers);
            }
        }
    }
    public static void main (String[] args) throws IOException{
        Scanner scanner = new Scanner(System.in);
        System.out.println("[SERVER] Enter your username :");
        String username = scanner.nextLine();
        Socket socket = new Socket("localhost",getNumber());
        //In case the client enters the correct port they will be notified and it creates a new client object.
        System.out.println("[SERVER] You have connected to the server!");
        Client client = new Client(socket,username);
        client.presentClients();
        client.listenForMessage();
        client.sendMessage();
    }
}