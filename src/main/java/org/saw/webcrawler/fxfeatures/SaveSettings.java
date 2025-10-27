package org.saw.webcrawler.fxfeatures;
import java.util.prefs.Preferences;

public class SaveSettings {
//    https://www.youtube.com/watch?v=Uxe7ZkX_Msw
        private final Preferences PREFS = Preferences.userNodeForPackage(SaveSettings.class);
        private final String KEY_AUTO_SHUTDOWN = "AUTO_SHUTDOWN";

        public void saveAutoShutDown(boolean turnOn) {
            PREFS.putBoolean(KEY_AUTO_SHUTDOWN, turnOn);
            System.out.println(turnOn);
        }

        public boolean loadAutoShutDown() {
            return PREFS.getBoolean(KEY_AUTO_SHUTDOWN, false);
        }

}
