/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkstats;

import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility class to send "ping" usage reports to the server. */
public class SdkStatsService {

    /** Minimum interval between ping, in milliseconds. */
    private static final long PING_INTERVAL_MSEC = 86400 * 1000;  // 1 day

    /* Text strings displayed in the opt-out dialog. */
    private static final String WINDOW_TITLE_TEXT =
        "Android SDK";

    private static final String HEADER_TEXT =
        "Thanks for using the Android SDK!";

    private static final String NOTICE_TEXT =
        "We know you just want to get started but please read this first.";

    /** Used in the preference pane (PrefsDialog) as well. */
    public static final String BODY_TEXT =
        "By choosing to send certain usage statistics to Google, you can " +
        "help us improve the Android SDK.  These usage statistics let us " +
        "measure things like active usage of the SDK and let us know things " +
        "like which versions of the SDK are in use and which tools are the " +
        "most popular with developers.  This limited data is not associated " +
        "with personal information about you, is examined on an aggregate " +
        "basis, and is maintained in accordance with the " +
        "<a href=\"http://www.google.com/intl/en/privacy.html\">Google " +
        "Privacy Policy</a>.";

    /** Used in the preference pane (PrefsDialog) as well. */
    public static final String CHECKBOX_TEXT =
        "Send usage statistics to Google.";

    private static final String FOOTER_TEXT =
        "If you later decide to change this setting, you can do so in the " +
        "\"ddms\" tool under \"File\" > \"Preferences\" > \"Usage Stats\".";

    private static final String BUTTON_TEXT =
        "   Proceed   ";

    /** List of Linux browser commands to try, in order (see openUrl). */
    private static final String[] LINUX_BROWSERS = new String[] {
        "firefox -remote openurl(%URL%,new-window)",  // $NON-NLS-1$ running FF
        "mozilla -remote openurl(%URL%,new-window)",  // $NON-NLS-1$ running Moz
        "firefox %URL%",                              // $NON-NLS-1$ new FF
        "mozilla %URL%",                              // $NON-NLS-1$ new Moz
        "kfmclient openURL %URL%",                    // $NON-NLS-1$ Konqueror
        "opera -newwindow %URL%",                     // $NON-NLS-1$ Opera
    };

    public final static String PING_OPT_IN = "pingOptIn"; //$NON-NLS-1$
    public final static String PING_TIME = "pingTime"; //$NON-NLS-1$
    public final static String PING_ID = "pingId"; //$NON-NLS-1$


    private static PreferenceStore sPrefStore;

    /**
     * Send a "ping" to the Google toolbar server, if enough time has
     * elapsed since the last ping, and if the user has not opted out.
     * If this is the first time, notify the user and offer an opt-out.
     * Note: UI operations (if any) are synchronous, but the actual ping
     * (if any) is sent in a <i>non-daemon</i> background thread.
     *
     * @param app name to report in the ping
     * @param version to report in the ping
     * @param display an optional {@link Display} object to use, or null, if a new one should be
     * created.
     */
    public static void ping(final String app, final String version, final Display display) {
        // Validate the application and version input.
        final String normalVersion = normalizeVersion(app, version);

        // Unique, randomly assigned ID for this installation.
        PreferenceStore prefs = getPreferenceStore();
        if (prefs != null) {
            if (!prefs.contains(PING_ID)) {
                // First time: make up a new ID.  TODO: Use something more random?
                prefs.setValue(PING_ID, new Random().nextLong());

                // Also give them a chance to opt out.
                prefs.setValue(PING_OPT_IN, getUserPermission(display));
                try {
                    prefs.save();
                }
                catch (IOException ioe) {
                }
            }

            // If the user has not opted in, do nothing and quietly return.
            if (!prefs.getBoolean(PING_OPT_IN)) {
                // user opted out.
                return;
            }

            // If the last ping *for this app* was too recent, do nothing.
            String timePref = PING_TIME + "." + app;  // $NON-NLS-1$
            long now = System.currentTimeMillis();
            long then = prefs.getLong(timePref);
            if (now - then < PING_INTERVAL_MSEC) {
                // too soon after a ping.
                return;
            }

            // Record the time of the attempt, whether or not it succeeds.
            prefs.setValue(timePref, now);
            try {
                prefs.save();
            }
            catch (IOException ioe) {
            }

            // Send the ping itself in the background (don't block if the
            // network is down or slow or confused).
            final long id = prefs.getLong(PING_ID);
            new Thread() {
                @Override
                public void run() {
                    try {
                        actuallySendPing(app, normalVersion, id);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Returns the DDMS {@link PreferenceStore}.
     */
    public static synchronized PreferenceStore getPreferenceStore() {
        if (sPrefStore == null) {
            // get the location of the preferences
            String homeDir = null;
            try {
                homeDir = AndroidLocation.getFolder();
            } catch (AndroidLocationException e1) {
                // pass, we'll do a dummy store since homeDir is null
            }

            if (homeDir != null) {
                String rcFileName = homeDir + "ddms.cfg"; //$NON-NLS-1$

                // also look for an old pref file in the previous location
                String oldPrefPath = System.getProperty("user.home") //$NON-NLS-1$
                    + File.separator + ".ddmsrc"; //$NON-NLS-1$
                File oldPrefFile = new File(oldPrefPath);
                if (oldPrefFile.isFile()) {
                    try {
                        PreferenceStore oldStore = new PreferenceStore(oldPrefPath);
                        oldStore.load();

                        oldStore.save(new FileOutputStream(rcFileName), "");
                        oldPrefFile.delete();

                        PreferenceStore newStore = new PreferenceStore(rcFileName);
                        newStore.load();
                        sPrefStore = newStore;
                    } catch (IOException e) {
                        // create a new empty store.
                        sPrefStore = new PreferenceStore(rcFileName);
                    }
                } else {
                    sPrefStore = new PreferenceStore(rcFileName);

                    try {
                        sPrefStore.load();
                    } catch (IOException e) {
                        System.err.println("Error Loading Preferences");
                    }
                }
            } else {
                sPrefStore = new PreferenceStore();
            }
        }

        return sPrefStore;
    }

    /**
     * Unconditionally send a "ping" request to the Google toolbar server.
     *
     * @param app name to report in the ping
     * @param version to report in the ping (dotted numbers, no more than four)
     * @param id of the local installation
     * @throws IOException if the ping failed
     */
    @SuppressWarnings("deprecation")
    private static void actuallySendPing(String app, String version, long id)
        throws IOException {
        // Detect and report the host OS.
        String os = System.getProperty("os.name");          // $NON-NLS-1$
        if (os.startsWith("Mac OS")) {                      // $NON-NLS-1$
            os = "mac";                                     // $NON-NLS-1$
            String osVers = getVersion();
            if (osVers != null) {
                os = os + "-" + osVers;                     // $NON-NLS-1$
            }
        } else if (os.startsWith("Windows")) {              // $NON-NLS-1$
            os = "win";                                     // $NON-NLS-1$
            String osVers = getVersion();
            if (osVers != null) {
                os = os + "-" + osVers;                     // $NON-NLS-1$
            }
        } else if (os.startsWith("Linux")) {                // $NON-NLS-1$
            os = "linux";                                   // $NON-NLS-1$
        } else {
            // Unknown -- surprising -- send it verbatim so we can see it.
            os = URLEncoder.encode(os);
        }

        // Include the application's name as part of the as= value.
        // Share the user ID for all apps, to allow unified activity reports.

        URL url = new URL(
            "http",                                         // $NON-NLS-1$
            "tools.google.com",                             // $NON-NLS-1$
            "/service/update?as=androidsdk_" + app +        // $NON-NLS-1$
                "&id=" + Long.toHexString(id) +             // $NON-NLS-1$
                "&version=" + version +                     // $NON-NLS-1$
                "&os=" + os);                               // $NON-NLS-1$

        // Discard the actual response, but make sure it reads OK
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Believe it or not, a 404 response indicates success:
        // the ping was logged, but no update is configured.
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK &&
            conn.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
            throw new IOException(
                conn.getResponseMessage() + ": " + url);    // $NON-NLS-1$
        }
    }

    /**
     * Returns the version of the os if it is defined as X.Y, or null otherwise.
     * <p/>
     * Example of returned versions can be found at http://lopica.sourceforge.net/os.html
     * <p/>
     * This method removes any exiting micro versions.
     */
    private static String getVersion() {
        Pattern p = Pattern.compile("(\\d+)\\.(\\d+).*"); // $NON-NLS-1$
        String osVers = System.getProperty("os.version"); // $NON-NLS-1$
        Matcher m = p.matcher(osVers);
        if (m.matches()) {
            return m.group(1) + "." + m.group(2);         // $NON-NLS-1$
        }

        return null;
    }

    /**
     * Prompt the user for whether they want to opt out of reporting.
     * @return whether the user allows reporting (they do not opt out).
     */
    private static boolean getUserPermission(Display display) {
        // Whether the user gave permission (size-1 array for writing to).
        // Initialize to false, set when the user clicks the button.
        final boolean[] permission = new boolean[] { false };

        boolean dispose = false;
        if (display == null) {
            display = new Display();
            dispose = true;
        }

        final Display currentDisplay = display;
        final boolean disposeDisplay = dispose;

        display.syncExec(new Runnable() {
            public void run() {
                final Shell shell = new Shell(currentDisplay, SWT.TITLE | SWT.BORDER);
                shell.setText(WINDOW_TITLE_TEXT);
                shell.setLayout(new GridLayout(1, false)); // 1 column

                // Take the default font and scale it up for the title.
                final Label title = new Label(shell, SWT.CENTER | SWT.WRAP);
                final FontData[] fontdata = title.getFont().getFontData();
                for (int i = 0; i < fontdata.length; i++) {
                    fontdata[i].setHeight(fontdata[i].getHeight() * 4 / 3);
                }
                title.setFont(new Font(currentDisplay, fontdata));
                title.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                title.setText(HEADER_TEXT);

                final Label notice = new Label(shell, SWT.WRAP);
                notice.setFont(title.getFont());
                notice.setForeground(new Color(currentDisplay, 255, 0, 0));
                notice.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                notice.setText(NOTICE_TEXT);

                final Link text = new Link(shell, SWT.WRAP);
                text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                text.setText(BODY_TEXT);
                text.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        openUrl(event.text);
                    }
                });

                final Button checkbox = new Button(shell, SWT.CHECK);
                checkbox.setSelection(true); // Opt-in by default.
                checkbox.setText(CHECKBOX_TEXT);

                final Link footer = new Link(shell, SWT.WRAP);
                footer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                footer.setText(FOOTER_TEXT);

                final Button button = new Button(shell, SWT.PUSH);
                button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
                button.setText(BUTTON_TEXT);
                button.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        permission[0] = checkbox.getSelection();
                        shell.close();
                    }
                });

                // Size the window to a fixed width, as high as necessary,
                // centered.
                final Point size = shell.computeSize(450, SWT.DEFAULT, true);
                final Rectangle screen = currentDisplay.getClientArea();
                shell.setBounds(screen.x + screen.width / 2 - size.x / 2, screen.y + screen.height
                        / 2 - size.y / 2, size.x, size.y);

                shell.open();
                while (!shell.isDisposed()) {
                    if (!currentDisplay.readAndDispatch())
                        currentDisplay.sleep();
                }

                if (disposeDisplay) {
                    currentDisplay.dispose();
                }
            }
        });

        return permission[0];
    }

    /**
     * Open a URL in an external browser.
     * @param url to open - MUST be sanitized and properly formed!
     */
    public static void openUrl(final String url) {
        // TODO: consider using something like BrowserLauncher2
        // (http://browserlaunch2.sourceforge.net/) instead of these hacks.

        // SWT's Program.launch() should work on Mac, Windows, and GNOME
        // (because the OS shell knows how to launch a default browser).
        if (!Program.launch(url)) {
            // Must be Linux non-GNOME (or something else broke).
            // Try a few Linux browser commands in the background.
            new Thread() {
                @Override
                public void run() {
                    for (String cmd : LINUX_BROWSERS) {
                        cmd = cmd.replaceAll("%URL%", url);  // $NON-NLS-1$
                        try {
                            Process proc = Runtime.getRuntime().exec(cmd);
                            if (proc.waitFor() == 0) break;  // Success!
                        } catch (InterruptedException e) {
                            // Should never happen!
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            // Swallow the exception and try the next browser.
                        }
                    }

                    // TODO: Pop up some sort of error here?
                    // (We're in a new thread; can't use the existing Display.)
                }
            }.start();
        }
    }

    /**
     * Validate the supplied application version, and normalize the version.
     * @param app to report
     * @param version supplied by caller
     * @return normalized dotted quad version
     */
    private static String normalizeVersion(String app, String version) {
        // Application name must contain only word characters (no punctuaation)
        if (!app.matches("\\w+")) {
            throw new IllegalArgumentException("Bad app name: " + app);
        }

        // Version must be between 1 and 4 dotted numbers
        String[] numbers = version.split("\\.");
        if (numbers.length > 4) {
            throw new IllegalArgumentException("Bad version: " + version);
        }
        for (String part: numbers) {
            if (!part.matches("\\d+")) {
                throw new IllegalArgumentException("Bad version: " + version);
            }
        }

        // Always output 4 numbers, even if fewer were supplied (pad with .0)
        StringBuffer normal = new StringBuffer(numbers[0]);
        for (int i = 1; i < 4; i++) {
            normal.append(".").append(i < numbers.length ? numbers[i] : "0");
        }
        return normal.toString();
    }
}
