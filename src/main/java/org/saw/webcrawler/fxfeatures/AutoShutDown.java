package org.saw.webcrawler.fxfeatures;

import java.io.IOException;

//https://www.youtube.com/watch?v=oTy-bxUUEps
public class AutoShutDown {
    private SaveSettings settings = new SaveSettings();

    public boolean isAutoShutdownEnabled() {
        return settings.loadAutoShutDown();
    }

    public void shutdown() throws IOException {
        String operatingSystem = System.getProperty("os.name");
        if (operatingSystem.contains("Windows")) {
            Runtime runtime = Runtime.getRuntime();
            readInfo.logs.add(operatingSystem);
            System.out.println(operatingSystem);
            readInfo.logs.add(readInfo.timestamp() + "AutoShutDown file: Shutdown");
            System.out.println(readInfo.timestamp() + "AutoShutDown file: Shutdown");
            runtime.exec("shutdown -s -t 0");
        } else {
            readInfo.logs.add(readInfo.timestamp() + "AutoShutDown file: Shutdown not supported on this OS");
            System.out.println(readInfo.timestamp() + "AutoShutDown file: Shutdown not supported on this OS");
        }
    }

}
