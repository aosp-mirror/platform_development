/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.monkeyrunner;

import com.google.common.collect.Maps;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/*
 * Custom Logging Formatter for MonkeyRunner that generates all log
 * messages on a single line.
 */
public class MonkeyFormatter extends Formatter {
    public static final Formatter DEFAULT_INSTANCE = new MonkeyFormatter();

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyMMdd HH:mm:ss.SSS");

    private static Map<Level, String> LEVEL_TO_STRING_CACHE = Maps.newHashMap();

    private static final String levelToString(Level level) {
        String levelName = LEVEL_TO_STRING_CACHE.get(level);
        if (levelName == null) {
            levelName = level.getName().substring(0, 1);
            LEVEL_TO_STRING_CACHE.put(level, levelName);
        }
        return levelName;
    }

    private static String getHeader(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        sb.append(FORMAT.format(new Date(record.getMillis()))).append(":");
        sb.append(levelToString(record.getLevel())).append(" ");

        sb.append("[").append(Thread.currentThread().getName()).append("] ");

        String loggerName = record.getLoggerName();
        if (loggerName != null) {
            sb.append("[").append(loggerName).append("]");
        }
        return sb.toString();
    }

    private class PrintWriterWithHeader extends PrintWriter {
        private final ByteArrayOutputStream out;
        private final String header;

        public PrintWriterWithHeader(String header) {
            this(header, new ByteArrayOutputStream());
        }

        public PrintWriterWithHeader(String header, ByteArrayOutputStream out) {
            super(out, true);
            this.header = header;
            this.out = out;
        }

        @Override
        public void println(Object x) {
            print(header);
            super.println(x);
        }

        @Override
        public void println(String x) {
            print(header);
            super.println(x);
        }

        @Override
        public String toString() {
            return out.toString();
        }
    }

    @Override
    public String format(LogRecord record) {
        Throwable thrown = record.getThrown();
        String header = getHeader(record);

        StringBuilder sb = new StringBuilder();
        sb.append(header);
        sb.append(" ").append(formatMessage(record));
        sb.append("\n");

        // Print the exception here if we caught it
        if (thrown != null) {

            PrintWriter pw = new PrintWriterWithHeader(header);
            thrown.printStackTrace(pw);
            sb.append(pw.toString());
        }

        return sb.toString();
    }
}
