import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
public class SyncDelete extends Thread{

    private ArrayList<String> currFileList = new ArrayList<String>();
    private ArrayList<String> prevFileList = new ArrayList<String>();
    public int clientNum = 0;

    public SyncDelete(int clientNum){
        this.clientNum = clientNum;
    }

    public void SyncDeletionWithServer(ArrayList<String> currentFiles, ArrayList<String> previousFiles,String path) {
        try{
            ArrayList<String> filesDeleted = Utils.getRemoved(previousFiles,currentFiles);

            String request = "DELETION SYNC # "+clientNum+Constants.CRLF;
            request+= String.join(",", currentFiles)+Constants.CRLF;
            request+= String.join(",", filesDeleted)+Constants.CRLF;
            request += Constants.CRLF;

            Utils.log(">> DELETION SYNC # CurrentFiles: "+String.join(",", currentFiles));
            Utils.log(">> DELETION SYNC # DeletedFiles: "+String.join(",", filesDeleted));

            Utils.log(">> DELETION SYNC # Creating sockets...");

            InetAddress serverIp=InetAddress.getByName("localhost");
            Socket tcpSocket = new Socket(serverIp, Constants.SERVER_TCP_PORT);

            Scanner inputSocket =   new Scanner(tcpSocket.getInputStream());
            PrintWriter outputSocket = new PrintWriter(tcpSocket.getOutputStream(), true);

            // send request
            Utils.log(Constants.CRLF+">> DELETION SYNC # Request:"+request);
            outputSocket.println(request+Constants.CRLF);

            // receive response
            String line=inputSocket.nextLine();

            String filesDeletedAtServer = "";
            while(true) {
                if (line.isEmpty()) {line=inputSocket.nextLine();continue;}
                if(line.startsWith("DELETION SYNC # ")){
                    filesDeletedAtServer=inputSocket.nextLine();
                    Utils.log(">> DELETION SYNC # Response: "+filesDeletedAtServer+Constants.CRLF);
                    break;
                }
            }
            inputSocket.close();
            outputSocket.close();
            Utils.log(filesDeletedAtServer.length());
            if(filesDeletedAtServer.length()>0){
                ArrayList<String> filesToDelete = new ArrayList<String>(Arrays.asList(filesDeletedAtServer.split(",")));
                ArrayList<String> filesAdded = Utils.getRemoved(currentFiles,previousFiles);
                filesToDelete = Utils.getRemoved(filesToDelete,filesAdded);
                Utils.deleteFiles(path, filesToDelete);
            }
        }
        catch(Exception e){}
    }

    public void run(){
        String path;
        Utils.log((clientNum==2));
        Utils.log(clientNum);
        Utils.log("clientNum:"+clientNum);
        if(clientNum==2)path=Constants.CLIENT_FILE_ROOT2;
        else{
            path=Constants.CLIENT_FILE_ROOT;
        }
        currFileList = Utils.listFilesUsingJavaIO(path);
        Utils.log(currFileList);

        while(true){
            Utils.waitSec(4);
            Utils.log("Start SyncDeletion");
            prevFileList = currFileList;
            currFileList = Utils.listFilesUsingJavaIO(path);
            SyncDeletionWithServer(currFileList, prevFileList,path);
            Utils.log("End SyncDeletion");
        }
    }
}
