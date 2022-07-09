import jdk.jshell.execution.Util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

public class Listner extends Thread{
    private Socket client;
    public Listner(Socket client) {
        this.client = client;
    }
    public void run(){
        try{
            Scanner inputSocket = new Scanner(client.getInputStream());
            PrintWriter outputSocket = new PrintWriter(client.getOutputStream(), true);

            String actionType = Utils.getActionType(inputSocket);
            Utils.log("actionType: "+actionType);
            switch (actionType){
                case "DELETION SYNC # ":
                    deletionSyncFromClient(inputSocket, outputSocket);
                    break;
                case "ADDITION SYNC # ":
                    additionSyncFromClient(inputSocket, outputSocket, client);
                    break;
                case "MODIFICATION SYNC # ":
                    break;
                default:
                    throw new IllegalArgumentException("Invalid action type: " + actionType);
            }
        }
        catch(IOException io) {
                Utils.log(">> Fail to listen to requests!");
                System.exit(1);
            }
    }

    public synchronized static void deletionSyncFromClient(Scanner inputSocket, PrintWriter outputSocket) {
        String clientCurrentFiles=inputSocket.nextLine();
        String clientDeletedFiles=inputSocket.nextLine();

        ArrayList<String> serverFileList = Utils.listFilesUsingJavaIO(Constants.SERVER_FILE_ROOT);
        ArrayList<String> clientCurrentFileList = new ArrayList<String>(Arrays.asList(clientCurrentFiles.split(",")));
        ArrayList<String> clientDeletedFileList = new ArrayList<String>(Arrays.asList(clientDeletedFiles.split(",")));

        ArrayList<String> deletedFilesAtClient = Utils.getRemoved(serverFileList,clientCurrentFileList);
        deletedFilesAtClient.retainAll(clientDeletedFileList);
        ArrayList<String> deletedFilesAtServer = Utils.getRemoved(clientCurrentFileList,serverFileList);

        Utils.log("serverFileList : "+ serverFileList);
        Utils.log("clientCurrentFileList : "+ clientCurrentFileList);
        Utils.log("deletedFilesAtServer : "+ deletedFilesAtServer);
        Utils.log("deletedFilesAtClient : "+ deletedFilesAtClient);

        String response = "DELETION SYNC # "+Constants.CRLF +  String.join(",", deletedFilesAtServer);

        Utils.log(">> Response: "+response);

        outputSocket.println(response+Constants.CRLF);
        outputSocket.close();

        Utils.deleteFiles(Constants.SERVER_FILE_ROOT, deletedFilesAtClient);
    }

    public synchronized void additionSyncFromClient(Scanner inputSocket, PrintWriter outputSocket, Socket client) {
        try{
            int clientSendUDPPort = Integer.parseInt(inputSocket.nextLine());
            int clientRecvUDPPort = Integer.parseInt(inputSocket.nextLine());
            String clientFiles = inputSocket.nextLine();

            ArrayList<String> serverFileList = Utils.listFilesUsingJavaIO(Constants.SERVER_FILE_ROOT);
            ArrayList<String> clientFileList = new ArrayList<String>(Arrays.asList(clientFiles.split(",")));

            ArrayList<String> addedFilesAtClient = Utils.getRemoved(clientFileList, serverFileList);
            ArrayList<String> addedFilesAtServer = Utils.getRemoved(serverFileList, clientFileList);

            Utils.log("serverFileList : "+ serverFileList);
            Utils.log("clientFileList : "+ clientFileList);
            Utils.log("addedFilesAtServer : "+ addedFilesAtServer);
            Utils.log("addedFilesAtClient : "+ addedFilesAtClient);

            sendRecvFiles(client,inputSocket, outputSocket, clientSendUDPPort,clientRecvUDPPort,addedFilesAtServer,addedFilesAtClient);

        }catch(Exception e){e.printStackTrace();}

    }

    private void sendFilesToClient(String serverFiles, int clientReceiverPort){
        try{
            String []files = serverFiles.split(",");
            for(String fileName : files){
                Utils.log("Sending file:"+fileName);
                InetAddress serverIp=InetAddress.getByName("localhost");
                // start sending the file
                PacketBoundedBufferMonitor bufferMonitor=new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);
                InetAddress senderIp=InetAddress.getByName("localhost");

                PacketSender packetSender=new PacketSender(bufferMonitor,senderIp,Constants.SERVER_SEND_UDP_PORT,serverIp,clientReceiverPort);
                packetSender.start();

                FileReader fileReader=new FileReader(bufferMonitor,fileName, Constants.SERVER_FILE_ROOT);
                fileReader.start();

                try {
                    packetSender.join();
                    fileReader.join();
                }
                catch (InterruptedException e) {e.printStackTrace();}
            }
        }
        catch(Exception e){}
    }

    public void sendRecvFiles(Socket client,Scanner input,PrintWriter output, int clientSendUDPPort,int clientRecvUDPPort,ArrayList<String> addedFilesAtServer,ArrayList<String> addedFilesAtClient){

        String serverFiles = String.join(",",addedFilesAtServer);
        String clientFiles = String.join(",",addedFilesAtClient);

        String response = "ADDITION SYNC # " + Constants.CRLF;
        response+="Server Sender Port:"+Constants.SERVER_SEND_UDP_PORT + Constants.CRLF;
        response+="Server Receiver Port:"+Constants.SERVER_RECV_UDP_PORT + Constants.CRLF;
        response+="clientFiles:"+clientFiles + Constants.CRLF;
        response+="serverFiles:"+serverFiles + Constants.CRLF + "STOP";

        Utils.log(">> Response: "+response);

        output.println(response);
        output.close();

        if(clientFiles.length()>0){
            receiveHandle(client,clientSendUDPPort);
        }

        if(serverFiles.length()>0){
//            Utils.waitSec(1500);
            SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
            Date date = new Date(System.currentTimeMillis());
            System.out.println(formatter.format(date));

            sendFilesToClient(serverFiles, clientRecvUDPPort);
        }
    }

    public static void receiveHandle(Socket socket,int senderPort) {
        try {
            PacketBoundedBufferMonitor bm=new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);
            InetAddress senderIp=socket.getInetAddress();// get the IP of the sender
            InetAddress receiverIp=InetAddress.getByName("localhost");

            receiveFile(bm,receiverIp,Constants.SERVER_RECV_UDP_PORT,senderIp, senderPort);// receive the file

        }catch(Exception e) {e.printStackTrace();}
    }

    public static void receiveFile(PacketBoundedBufferMonitor bm, InetAddress receiverIp,int receiverPort,InetAddress senderIp,int senderPort) {

        PacketReceiver packetReceiver=new PacketReceiver(bm,receiverIp,receiverPort,senderIp,senderPort);
        packetReceiver.start();

        FileWriter fileWriter=new FileWriter(bm,Constants.SERVER_FILE_ROOT);
        fileWriter.start();
        try {
            packetReceiver.join();
            fileWriter.join();
        }
        catch (InterruptedException e) {e.printStackTrace();}
    }
}


