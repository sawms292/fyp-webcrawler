package org.saw.webcrawler.fxfeatures;
import java.util.prefs.Preferences;

/**
 * Stored shutdown settings using Java Preferences
 */
public class SaveSettings {
    /**
     * Preferences node associated with this class
     *  Used to store user settings
     */
//    https://www.youtube.com/watch?v=Uxe7ZkX_Msw
        private final Preferences PREFS = Preferences.userNodeForPackage(SaveSettings.class);

    /**
     * Preference key for the auto-shutdown setting
     */
        private final String KEY_AUTO_SHUTDOWN = "AUTO_SHUTDOWN";

    /**
     * Saves the auto-shutdown setting
     * @param turnOn -> true to enable auto-shutdown, false to disable
     */
        public void saveAutoShutDown(boolean turnOn) {
            PREFS.putBoolean(KEY_AUTO_SHUTDOWN, turnOn);
            System.out.println(turnOn);
            readInfo.logs.add(readInfo.timestamp() + "SaveSettings result :" +turnOn+ "\n");
        }

    /**
     * Loads the auto-shutdown setting
     * @return true if auto-shutdown is enabled,else false
     */
        public boolean loadAutoShutDown() {
            return PREFS.getBoolean(KEY_AUTO_SHUTDOWN, false);
        }

}
