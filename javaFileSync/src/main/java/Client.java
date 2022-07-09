import java.io.IOException;
import java.util.ArrayList;

public class Client {
    public Client() {}

    public static void main(String[] args) throws IOException {
        int clientNum = Integer.parseInt(args[0]);

        SyncDelete syncDelete = new SyncDelete(clientNum);
        SyncAdd syncAdd = new SyncAdd(clientNum);

        syncDelete.start();
        syncAdd.start();
        try {
            syncDelete.join();
            syncAdd.join();
        }
        catch (InterruptedException e) {e.printStackTrace();}
    }

}
