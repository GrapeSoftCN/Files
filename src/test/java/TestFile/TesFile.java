package TestFile;

import httpServer.booter;
import nlogger.nlogger;

public class TesFile {
    public static void main(String[] args) {
        booter booter = new booter();
        try {
            System.out.println("GrapeFile");
            System.setProperty("AppName", "GrapeFile");
            booter.start(1006);
        } catch (Exception e) {
            nlogger.logout(e);
        }
    }
}
