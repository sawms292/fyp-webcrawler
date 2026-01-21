package org.saw.webcrawler.fxfeatures;

import java.io.IOException;

/**
 * Provides automatic system shutdown functionality based on user settings
 */
//https://www.youtube.com/watch?v=oTy-bxUUEps
public class AutoShutDown {
    /**
     * Settings handler used to load the auto-shutdown preference
     */
    private SaveSettings settings = new SaveSettings();

    /**
     * Checks if auto-shutdown is enabled in settings
     * @return true if auto-shutdown is enabled, else false
     */
    public boolean isAutoShutdownEnabled() {
        return settings.loadAutoShutDown();
    }

    /**
     * Executes the system shutdown command if auto-shutdown is enabled
     * @throws IOException if the shutdown command cannot be executed
     */
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
