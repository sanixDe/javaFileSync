
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

public class SyncAdd extends Thread{
    private ArrayList<String> currFileList = new ArrayList<String>();
    public int clientNum = 0;
    public SyncAdd(int clientNum){
        this.clientNum = clientNum;
    }

    private void sendFiles(String clientFiles, int serverReceiverPort,String path){
        try{
            String []files = clientFiles.split(",");
            for(String fileName : files){
                Utils.log("Sending file:"+fileName);
                InetAddress serverIp=InetAddress.getByName("localhost");

                // start sending the file
                PacketBoundedBufferMonitor bufferMonitor=new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);
                InetAddress senderIp=InetAddress.getByName("localhost");

                PacketSender packetSender=new PacketSender(bufferMonitor,senderIp,Constants.CLIENT_SEND_UDP_PORT,serverIp,serverReceiverPort);
                packetSender.start();


                FileReader fileReader=new FileReader(bufferMonitor,fileName, path);
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

    public static void receiveHandle(int senderPort,String Path) {
        try {
            PacketBoundedBufferMonitor bm=new PacketBoundedBufferMonitor(Constants.MONITOR_BUFFER_SIZE);
            InetAddress senderIp=InetAddress.getByName("localhost");
            InetAddress receiverIp=InetAddress.getByName("localhost");

            receiveFile(bm,receiverIp,Constants.CLIENT_RECV_UDP_PORT,senderIp, senderPort,Path);// receive the file

        }catch(Exception e) {e.printStackTrace();}
    }

    public static void receiveFile(PacketBoundedBufferMonitor bm, InetAddress receiverIp,int receiverPort,InetAddress senderIp,int senderPort,String Path) {

        PacketReceiver packetReceiver=new PacketReceiver(bm,receiverIp,receiverPort,senderIp,senderPort);
        packetReceiver.start();

        FileWriter fileWriter=new FileWriter(bm,Path);
        fileWriter.start();
        try {
            packetReceiver.join();
            fileWriter.join();
        }
        catch (InterruptedException e) {e.printStackTrace();}
    }


    public void SyncAddWithServer(ArrayList<String> currentFiles,String path) {
        try{
            String request = "ADDITION SYNC # "+clientNum+Constants.CRLF;
            request+= Constants.CLIENT_SEND_UDP_PORT+Constants.CRLF;
            request+= Constants.CLIENT_RECV_UDP_PORT+Constants.CRLF;
            request+= String.join(",", currentFiles)+Constants.CRLF;
            request+= "STOP";
            Utils.log(">> Creating sockets (additionSync)...");

            InetAddress serverIp=InetAddress.getByName("localhost");
            Socket tcpSocket = new Socket(serverIp, Constants.SERVER_TCP_PORT);

            Scanner inputSocket =   new Scanner(tcpSocket.getInputStream());
            PrintWriter outputSocket = new PrintWriter(tcpSocket.getOutputStream(), true);

            // send request
            Utils.log(Constants.CRLF+">> Request:"+request);
            outputSocket.println(request+Constants.CRLF);

            int serverSenderPort =0, serverReceiverPort =0;
            String filesAddedAtServer = "",filesAddedAtClient = "";
            outputSocket.println(11111111);
            // receive the response
            String line=inputSocket.nextLine();
            outputSocket.println("Client Started Receiving");
            // get the fileTransmission details
            while(!line.equals("STOP")) {
                if (line.isEmpty()) {line=inputSocket.nextLine();continue;}
                Utils.log(line);
                if(line.startsWith("Server Sender Port:")) {
                    String [] items=line.split(":");
                    serverSenderPort=Integer.parseInt(items[items.length-1]);
                }
                if(line.startsWith("Server Receiver Port:")){
                    String [] items=line.split(":");
                    serverReceiverPort=Integer.parseInt(items[items.length-1]);
                }
                if(line.startsWith("serverFiles")){
                    String [] items=line.split(":");
                    filesAddedAtServer=items.length>1?items[items.length-1]:"";
                }
                if(line.startsWith("clientFiles")){
                    String [] items=line.split(":");
                    filesAddedAtClient=items.length>1?items[items.length-1]:"";
                }
                line=inputSocket.nextLine();
            }

            inputSocket.close();
            outputSocket.close();

            if(filesAddedAtServer.length()>0){
                SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
                Date date = new Date(System.currentTimeMillis());
                System.out.println(formatter.format(date));

                receiveHandle(serverSenderPort,path);
            }

            if(filesAddedAtClient.length()>0){
                sendFiles(filesAddedAtClient, serverReceiverPort, path);
            }

        }catch(Exception e){

        }



    }

    public void run(){
        String path;
        Utils.log("clientNum: "+clientNum);

        if(clientNum==2)path=Constants.CLIENT_FILE_ROOT2;
        else path=Constants.CLIENT_FILE_ROOT;

        currFileList = Utils.listFilesUsingJavaIO(path);
        Utils.log(currFileList);

        while(true){
            Utils.waitSec(4);
            Utils.log("Start SyncAddition");
            currFileList = Utils.listFilesUsingJavaIO(path);
            SyncAddWithServer(currFileList,path);
            Utils.log("End SyncAddition");
        }
    }
}
