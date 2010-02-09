package com.android.bugreportsender;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class for parsing a bugreport into its sections.
 */
public final class BugReportParser {
    private static final int BUFFER_SIZE = 8*1024;
    private static final String SECTION_HEADER = "------";
    private static final int MAX_LINES = 1000; // just in case we miss the end of the section.

    // utility class
    private BugReportParser() {}

    public static String extractSystemLogs(InputStream in, String section) throws IOException {
        final String sectionWithHeader = SECTION_HEADER + " " + section;
        StringBuilder sb = new StringBuilder();
        // open a reader around the provided inputstream.
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"), BUFFER_SIZE);
        boolean inSection = false;
        int numLines = 0;
        // read file contents.  loop until we get to the appropriate section header.
        // once we reach that header, accumulate all lines until we get to the next section.
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (inSection) {
                // finish when we get to:
                // -----
                if (line.startsWith(SECTION_HEADER) || (numLines > MAX_LINES)) {
                    break;
                }
                sb.append(line);
                sb.append("\n");
                ++numLines;
            } else if (line.startsWith(sectionWithHeader)) {
                sb.append(line);
                sb.append("\n");
                inSection = true;
            }
        }
        return sb.toString();
    }
}
