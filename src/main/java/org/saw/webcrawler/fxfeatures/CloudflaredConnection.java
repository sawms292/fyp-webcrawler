package org.saw.webcrawler.fxfeatures;


import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class CloudflaredConnection {
//    private static final Logger log = LoggerFactory.getLogger(CloudflaredConnection.class);

    //  Check cloudflared connected situation
    private static boolean connected = false;

//  ProcessBuilder did not interpret string as classpath
//  Tunnel Directory
    private static final URL TUNNELDIRECTORY = CloudflaredConnection.class.getClassLoader().getResource("bin/");
    private static String tunnelDirectorySecond = System.getProperty("user.dir") + "\\bin";
//    private static String tunnelDirectoryThird = System.getProperty("user.dir") + "\\webcrawler\\src\\main\\resources\\bin";
//  End of tunnel directory

//  Powershell checking cloudflared available or not
    private static final ProcessBuilder POWFORPID = new ProcessBuilder("powershell.exe", "-Command", "Get-Process -Name cloudflared -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Id");


//  Cloudflared tunnel command
    private static ProcessBuilder CMDFORTUNNEL = new ProcessBuilder("cloudflared.exe", "access", "tcp",
            "--hostname", "mysql.shenguum.com",
            "--url", "localhost:3306");
//  End of cloudflared tunnel command

//  Other variable
    private static Process tunnelProcess;
    private static Process pidProcess;
    private static boolean manualCloseTunnel = false;
    private static File cloudflaredDir;

//  pid number
    private static long tunnelPidLong;

    private static Thread tunnelThread;
    /**
     * @return pid number
     */
    private static long powershellPid() {
        long pidLongInside = 0;
            try {
            pidProcess = POWFORPID.start();
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(pidProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if(!line.isEmpty() && line.matches("\\d+")) {
                        pidLongInside = Long.parseLong(line);
                        int exitCode = pidProcess.waitFor();
                        if (exitCode == 0) {
                            System.out.println("pid: " + pidLongInside);
                            return pidLongInside;
                        }
                    }
                }
            }
            }catch (IOException | NumberFormatException |InterruptedException e){
                System.out.println("CloudflaredConnection file-powershellPid : " + e.getMessage());
                return -1;
            }
            return -1;
    }

    /**
     * start and checking tunnel situation
     */
    public static synchronized void getTunnelDirectoryAndRun() {
        if(tunnelThread != null && tunnelThread.isAlive()){
            return;
        }

        manualCloseTunnel = false;
        tunnelThread = new Thread(() -> {
            try{
                long pidLong = powershellPid();
            if(pidLong == -1 && !manualCloseTunnel){
                if(tunnelTurnOn()){
                    tunnelProcess = CMDFORTUNNEL.start();
                    tunnelPidLong = tunnelProcess.pid();
                    connected = true;
                }
            }else{
                tunnelPidLong = pidLong;
                System.out.println("pid: " + tunnelPidLong);
                connected = true;
                }


                while(connected && !manualCloseTunnel){
                    tunnelPidLong = powershellPid();
                    if (!tunnelExists() && !manualCloseTunnel){
                        tunnelProcess = CMDFORTUNNEL.start();
                        readInfo.logs.add(readInfo.timestamp() + "CloudflaredConnection file: Tunnel Process Started");
                        System.out.println(readInfo.timestamp() + "CloudflaredConnection file: Tunnel Process Started");
                    }
                        readInfo.logs.add(readInfo.timestamp() + "CloudflaredConnection file: Tunnel Process Alive");
                        System.out.println(readInfo.timestamp() + "CloudflaredConnection file: Tunnel Process Alive");

                        Thread.sleep(5000);
                    for (int i = 0; i < 10; i++) {
                        if (!connected || manualCloseTunnel) break;
                        Thread.sleep(1000);
                    }
                }
        }catch (IOException ex){
            readInfo.logs.add(readInfo.timestamp() + "CloudflaredConnection file: IOException error: at getTunnelDirectoryAndRun: " + ex.getMessage());
            ex.printStackTrace();
        }catch(InterruptedException exx){
            readInfo.logs.add(readInfo.timestamp() + "CloudflaredConnection file: InterruptedException error: at getTunnelDirectoryAndRun: " + exx.getMessage());
            exx.printStackTrace();
        }
        });
        tunnelThread.setDaemon(true);
        tunnelThread.start();
    }

    private static boolean tunnelTurnOn(){
        boolean found = false;
        try {
        if (TUNNELDIRECTORY != null && "file".equals(TUNNELDIRECTORY.getProtocol())) {

                readInfo.logs.add(readInfo.timestamp() + "CloudflaredConnection file-tunnelTurnOn-1: Tunnel Directory Found at filesystem");
                System.out.println(readInfo.timestamp() + "CloudflaredConnection file-tunnelTurnOn-1: Tunnel Directory Found at filesystem");
                cloudflaredDir = new File(TUNNELDIRECTORY.toURI());
                CMDFORTUNNEL.directory(cloudflaredDir);
                found = true;
                return found;

        }else if (TUNNELDIRECTORY != null && "jar".equals(TUNNELDIRECTORY.getProtocol())) {
                readInfo.logs.add(readInfo.timestamp() + "CloudflaredConnection file-tunnelTurnOn-2: Tunnel Directory Found at jar");
                System.out.println(readInfo.timestamp() + "CloudflaredConnection file-tunnelTurnOn-2: Tunnel Directory Found at jar");
                InputStream in = CloudflaredConnection.class.getResourceAsStream("/bin/cloudflared.exe");
                if (in == null) throw new IOException("cloudflared.exe not found in resources/bin!");

                File tempExe = new File(System.getProperty("java.io.tmpdir"), "cloudflared.exe");
                try (FileOutputStream out = new FileOutputStream(tempExe)) {
                    in.transferTo(out);
                }
                tempExe.setExecutable(true);

                CMDFORTUNNEL = new ProcessBuilder(tempExe.getAbsolutePath(), "access", "tcp",
                        "--hostname", "mysql.shenguum.com",
                        "--url", "localhost:3306");
                CMDFORTUNNEL.directory(tempExe.getParentFile()); // optional
                found = true;
                return found;
        }else if(System.getProperty("user.dir" +"\\bin") != null){
                readInfo.logs.add(readInfo.timestamp() + "CloudflaredConnection file-tunnelTurnOn-3: No Tunnel Directory Found");
                System.out.println(readInfo.timestamp() + "CloudflaredConnection file-tunnelTurnOn-3: No Tunnel Directory Found");
                cloudflaredDir = new File(tunnelDirectorySecond);
                CMDFORTUNNEL.directory(cloudflaredDir);
                found = true;
                return found;
        }
        }catch(Exception e){
            readInfo.logs.add(readInfo.timestamp() + "CloudflaredConnection file-tunnelTurnOn-1-2-3: " + e.getMessage());
            System.out.println(readInfo.timestamp() + "CloudflaredConnection file-tunnelTurnOn-1-2-3: " + e.getMessage());
            found = false;
            return found;
        }
        return found;
    }


    /**
     * Turn off cloudflared tunnel
     */
    public static void turnOffProcess() {
        try {
            connected = false;
            if(tunnelExists()){
                // Kill by PID (Windows only)
                manualCloseTunnel = true;
                Process killer = new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(tunnelPidLong)).start();
                killer.waitFor(1, TimeUnit.SECONDS);
                readInfo.logs.add(readInfo.timestamp() + "CloudflaredConnection file: Tunnel Process Stopped");
                System.out.println(readInfo.timestamp() + "CloudflaredConnection file: Tunnel stopped (taskkill).");
            }
            if (tunnelThread != null) {
                tunnelThread.interrupt(); // wake it up from sleep
                tunnelThread = null;
            }
        } catch (Exception e) {
            readInfo.logs.add(readInfo.timestamp() + "CloudflaredConnection file: IOException error: at tunnelExists: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @return check tunnel exists or not
     */
    public static boolean tunnelExists() {
        boolean found = false;
        if(tunnelProcess != null && tunnelProcess.isAlive()) {
            found = true;
            return found;
        }
        if (tunnelPidLong > 0) {
            return ProcessHandle.of(tunnelPidLong)
                    .map(ProcessHandle::isAlive)
                    .orElse(false);
        }
        found = false;
        return found;

    }
}
