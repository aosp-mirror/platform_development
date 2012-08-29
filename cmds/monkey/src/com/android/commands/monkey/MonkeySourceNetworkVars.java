/*
 * Copyright 2009, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.commands.monkey;

import android.hardware.display.DisplayManagerGlobal;
import android.os.Build;
import android.os.SystemClock;
import android.view.Display;
import android.util.DisplayMetrics;

import com.android.commands.monkey.MonkeySourceNetwork.CommandQueue;
import com.android.commands.monkey.MonkeySourceNetwork.MonkeyCommand;
import com.android.commands.monkey.MonkeySourceNetwork.MonkeyCommandReturn;

import java.lang.Integer;
import java.lang.Float;
import java.lang.Long;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MonkeySourceNetworkVars {
    /**
     * Interface to get the value of a var.
     */
    private static interface VarGetter {
        /**
         * Get the value of the var.
         * @returns the value of the var.
         */
        public String get();
    }

    private static class StaticVarGetter implements VarGetter {
        private final String value;

        public StaticVarGetter(String value) {
            this.value = value;
        }

        public String get() {
            return value;
        }
    }

    // Use a TreeMap to keep the keys sorted so they get displayed nicely in listvar
    private static final Map<String, VarGetter> VAR_MAP = new TreeMap<String, VarGetter>();

    static {
        VAR_MAP.put("build.board", new StaticVarGetter(Build.BOARD));
        VAR_MAP.put("build.brand", new StaticVarGetter(Build.BRAND));
        VAR_MAP.put("build.device", new StaticVarGetter(Build.DEVICE));
        VAR_MAP.put("build.display", new StaticVarGetter(Build.DISPLAY));
        VAR_MAP.put("build.fingerprint", new StaticVarGetter(Build.FINGERPRINT));
        VAR_MAP.put("build.host", new StaticVarGetter(Build.HOST));
        VAR_MAP.put("build.id", new StaticVarGetter(Build.ID));
        VAR_MAP.put("build.model", new StaticVarGetter(Build.MODEL));
        VAR_MAP.put("build.product", new StaticVarGetter(Build.PRODUCT));
        VAR_MAP.put("build.tags", new StaticVarGetter(Build.TAGS));
        VAR_MAP.put("build.brand", new StaticVarGetter(Long.toString(Build.TIME)));
        VAR_MAP.put("build.type", new StaticVarGetter(Build.TYPE));
        VAR_MAP.put("build.user", new StaticVarGetter(Build.USER));
        VAR_MAP.put("build.cpu_abi", new StaticVarGetter(Build.CPU_ABI));
        VAR_MAP.put("build.manufacturer", new StaticVarGetter(Build.MANUFACTURER));
        VAR_MAP.put("build.version.incremental", new StaticVarGetter(Build.VERSION.INCREMENTAL));
        VAR_MAP.put("build.version.release", new StaticVarGetter(Build.VERSION.RELEASE));
        VAR_MAP.put("build.version.sdk", new StaticVarGetter(Integer.toString(Build.VERSION.SDK_INT)));
        VAR_MAP.put("build.version.codename", new StaticVarGetter(Build.VERSION.CODENAME));

        // Display
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(Display.DEFAULT_DISPLAY);
        VAR_MAP.put("display.width", new StaticVarGetter(Integer.toString(display.getWidth())));
        VAR_MAP.put("display.height", new StaticVarGetter(Integer.toString(display.getHeight())));

        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        VAR_MAP.put("display.density", new StaticVarGetter(Float.toString(dm.density)));

        // am.  note that the current activity information isn't valid
        // until the first activity gets launched after the monkey has
        // been started.
        VAR_MAP.put("am.current.package", new VarGetter() {
                public String get() {
                    return Monkey.currentPackage;
                }
            });
        VAR_MAP.put("am.current.action", new VarGetter() {
                public String get() {
                    if (Monkey.currentIntent == null) {
                        return null;
                    }
                    return Monkey.currentIntent.getAction();
                }
            });
        VAR_MAP.put("am.current.comp.class", new VarGetter() {
                public String get() {
                    if (Monkey.currentIntent == null) {
                        return null;
                    }
                    return Monkey.currentIntent.getComponent().getClassName();
                }
            });
        VAR_MAP.put("am.current.comp.package", new VarGetter() {
                public String get() {
                    if (Monkey.currentIntent == null) {
                        return null;
                    }
                    return Monkey.currentIntent.getComponent().getPackageName();
                }
            });
        VAR_MAP.put("am.current.data", new VarGetter() {
                public String get() {
                    if (Monkey.currentIntent == null) {
                        return null;
                    }
                    return Monkey.currentIntent.getDataString();
                }
            });
        VAR_MAP.put("am.current.categories", new VarGetter() {
                public String get() {
                    if (Monkey.currentIntent == null) {
                        return null;
                    }
                    StringBuffer sb = new StringBuffer();
                    for (String cat : Monkey.currentIntent.getCategories()) {
                        sb.append(cat).append(" ");
                    }
                    return sb.toString();
                }
            });

        // clock
        VAR_MAP.put("clock.realtime", new VarGetter() {
                public String get() {
                    return Long.toString(SystemClock.elapsedRealtime());
                }
            });
        VAR_MAP.put("clock.uptime", new VarGetter() {
                public String get() {
                    return Long.toString(SystemClock.uptimeMillis());
                }
            });
        VAR_MAP.put("clock.millis", new VarGetter() {
                public String get() {
                    return Long.toString(System.currentTimeMillis());
                }
            });
        VAR_MAP.put("monkey.version", new VarGetter() {
                public String get() {
                    return Integer.toString(MonkeySourceNetwork.MONKEY_NETWORK_VERSION);
                }
            });
    }

    /**
     * Command to list the "vars" that the monkey knows about.
     */
    public static class ListVarCommand implements MonkeySourceNetwork.MonkeyCommand {
        // listvar
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            Set<String> keys = VAR_MAP.keySet();
            StringBuffer sb = new StringBuffer();
            for (String key : keys) {
                sb.append(key).append(" ");
            }
            return new MonkeyCommandReturn(true, sb.toString());
        }
    }

    /**
     * Command to get the value of a var.
     */
    public static class GetVarCommand implements MonkeyCommand {
        // getvar varname
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            if (command.size() == 2) {
                VarGetter getter = VAR_MAP.get(command.get(1));
                if (getter == null) {
                    return new MonkeyCommandReturn(false, "unknown var");
                }
                return new MonkeyCommandReturn(true, getter.get());
            }
            return MonkeySourceNetwork.EARG;
        }
    }
}
