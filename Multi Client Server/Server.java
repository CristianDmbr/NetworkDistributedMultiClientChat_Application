import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket serverSocket;

    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    public void startServer() {
        try{
            while(!serverSocket.isClosed()){
                System.out.println("[SERVER] Waiting for client connection ...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] Client has connected !");

                ClientHandler clientHandler = new ClientHandler(clientSocket);

                Thread thread = new Thread(clientHandler);
                thread.start();



            }
        } catch (IOException e){
            System.out.println("[SERVER] Server is closed");
        }
    }
    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(2000);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}