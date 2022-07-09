import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    public Server(){}
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket=null;
            try {
                serverSocket = new ServerSocket(Constants.SERVER_TCP_PORT);
            } catch (IOException ioEx) {
                Utils.log("\n>> Unable to set up port!");
                System.exit(1);
            }

            Utils.log("\r\n>> Ready to accept requests");
            while(true){
                Socket client = serverSocket.accept();
                Utils.log("\n>> New request is accepted."+Constants.CRLF+client.toString());

                Listner instance = new Listner(client);
                instance.start();
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
}
