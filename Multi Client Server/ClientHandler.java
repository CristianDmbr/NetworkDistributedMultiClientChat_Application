import jdk.internal.icu.impl.BMPSet;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
public class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler>clientHandlers= new ArrayList<>();
    public static BMPSet activeClients;
    private Socket socket;
    BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    public ClientHandler(Socket socket) {
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            clientHandlers.add(this);
            if (clientHandlers.size() == 1) { // First client becomes the coordinator
                bufferedWriter.write("[SERVER] You are the coordinator");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } else { // Not the first client
                ClientHandler coordinator =clientHandlers.get(0);
                bufferedWriter.write("[SERVER] " + coordinator.clientUsername + " is the coordinator");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
            broadcastMessage("[SERVER] " + clientUsername + " has entered the chat !");
        } catch (IOException e) {
            closeEverything(socket,bufferedWriter,bufferedReader);
        }
    }
    public void sendMessageTo(String recipient, String messageToSend) {
        for (ClientHandler clientHandler :clientHandlers) {
            try {
                if (clientHandler.clientUsername.equals(recipient)) {
                    clientHandler.bufferedWriter.write("[PRIVATE] " + clientUsername + " : " + messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                    break;
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedWriter, bufferedReader);
            }
        }
    }
    public void pingClient(String clientToPing) {
        boolean clientFound = false;
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.clientUsername.trim().equals(clientToPing.trim())) {
                try {
                    clientHandler.bufferedWriter.write("[SERVER] You were pinged by " + clientUsername);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                    clientFound = true; // set flag to true once a matching client is found
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!clientFound) {
            try {
                // Get the coordinator's ClientHandler instance and use its bufferedWriter to send the message
                ClientHandler coordinator = clientHandlers.get(0);
                coordinator.bufferedWriter.write("[SERVER] Client " + clientToPing + " not found.");
                coordinator.bufferedWriter.newLine();
                coordinator.bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void broadcastMessage(String messageToSend) {
        if (messageToSend.contains("ping ")) {
            String[] parts = messageToSend.split("\\s+", 2);
            String clientToPing = parts[1];
            pingClient(clientToPing);
            return;
        } else if (messageToSend.contains("private")) {
            String[] parts = messageToSend.split("\\s+", 3);
            String recipient = parts[1];
            String message = parts[2];
            sendMessageTo(recipient, message);
        } else if (messageToSend.contains("who")) {
            try {
                StringBuilder response = new StringBuilder();
                response.append("[SERVER] Connected clients:\n");
                for (ClientHandler handler : clientHandlers) {
                    response.append(handler.clientUsername);
                    response.append(" (port ");
                    response.append(handler.socket.getPort());
                    response.append(")\n");
                }
                bufferedWriter.write(response.toString());
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything(socket, bufferedWriter, bufferedReader);
            }
        } else {
            for (ClientHandler clientHandler : clientHandlers) {
                try {
                    if (!clientHandler.clientUsername.equals(clientUsername)) {
                        if (messageToSend.contains("@all")) {
                            clientHandler.bufferedWriter.write(messageToSend.substring(5));
                        } else {
                            clientHandler.bufferedWriter.write(messageToSend);
                        }
                        clientHandler.bufferedWriter.newLine();
                        clientHandler.bufferedWriter.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("[SERVER] " + clientUsername + " has left the chat.");
        try {
            socket.close();
            bufferedReader.close();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        String messageFromClient;
        while(socket.isConnected()){
            try {
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient.equals("exit")) {
                    removeClientHandler();
                    break;
                }
                broadcastMessage(messageFromClient);

            } catch (IOException e){
                System.out.println("[SERVER] Error occurred ... closing connection");
                closeEverything(socket,bufferedWriter,bufferedReader);
                break;
            }
        }
    }
    public void closeEverything(Socket socket, BufferedWriter bufferedWriter, BufferedReader bufferedReader) {
        removeClientHandler();
        try {
            if(socket != null) {
                socket.close();
            }
            if (bufferedReader != bufferedReader) {
                bufferedReader.close();
            }
            if (bufferedWriter != bufferedWriter) {
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}