import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    public Utils(){}
    public static void waitSec(int sec){
        try {
            TimeUnit.SECONDS.sleep(sec);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static void deleteFiles(String directory, ArrayList<String>filesToDelete){
        for(int i=0;i<filesToDelete.size();i++){
            System.out.println(i);
            //https://www.w3schools.com/java/java_files_delete.asp
            File file = new File(directory+filesToDelete.get(i));
            if (file.delete()) {
                System.out.println("Deleted the file: " + file.getName());
            } else {
                System.out.println("Failed to delete the file: "+ file.getName());
            }
        }
    }

    public static ArrayList<String> getRemoved(ArrayList<String>first, ArrayList<String>last){
        ArrayList<String> duplicate = new ArrayList<>(first);
        duplicate.removeAll(last);
        return duplicate;
    }

    public static synchronized ArrayList<String> listFilesUsingJavaIO(String dir) {
        File directoryPath = new File(dir);
        String filesList[] = directoryPath.list();
        ArrayList<String> List = new ArrayList<String>();
        for(String fileName : filesList)List.add(fileName);
        return List;
    }

    public static void log(Object line) {
        System.out.println(line);
    }

    public static String getActionType(Scanner inputSocket){

        String line=inputSocket.nextLine();
        String actionType = "";
        while(true) {
            if (line.isEmpty()) {line=inputSocket.nextLine();continue;}
            if(line.startsWith("DELETION SYNC # ")){
                actionType="DELETION SYNC # ";
                break;
            }
            if(line.startsWith("ADDITION SYNC # ")){
                actionType="ADDITION SYNC # ";
                break;
            }
            line=inputSocket.nextLine();
        }
        return actionType;
    }
}
