/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.archquery;

/**
 * Java command line tool to return the CPU architecture of the host java VM.
 *
 * The goal is to be able to launch SWT based applications (DDMS, Traceview, Android) on any
 * type of OS.
 *
 * Because a 64 bit OS can run a 32 bit Virtual Machine, we need to query the VM itself to know
 * whether it's 32 or 64 bit to detect which swt.jar it should use (it contains native libraries).
 * Simply querying the OS is not enough.
 *
 * The other problem is that once a VM is launched it is impossible to change its classpath to
 * point the VM to the correct version of swt.jar.
 *
 * The solution is this small command line tool, running in the VM, and returning the value of
 * the 'os.arch' property. Based on the returned value, the script launching the SWT based
 * applications will configure the Java VM with the path to the correct swt.jar
 *
 * Because different VMs return different values for 32 and 64 bit version of x86 CPUs, the program
 * handles all the possible values and normalize the returned value.
 *
 * At this time, the normalized values are:
 * x86:    32 bit x86
 * x86_64: 64 bit x86
 * ppc:    PowerPC (WARNING: the SDK doesn't actually support this architecture).
 *
 *
 */
public final class Main {
    public static void main(String[] args) {

        for (String arg : args) {
            System.out.println(String.format("%1$s: %2$s", arg, System.getProperty(arg)));
        }

        if (args.length == 0) {
            // Values listed from http://lopica.sourceforge.net/os.html
            String arch = System.getProperty("os.arch");

            if (arch.equalsIgnoreCase("x86_64") || arch.equalsIgnoreCase("amd64")) {
                System.out.print("x86_64");

            } else if (arch.equalsIgnoreCase("x86")
                    || arch.equalsIgnoreCase("i386")
                    || arch.equalsIgnoreCase("i686")) {
                System.out.print("x86");

            } else if (arch.equalsIgnoreCase("ppc") || arch.equalsIgnoreCase("PowerPC")) {
                System.out.print("ppc");
            } else {
                System.out.print(arch);
            }
        }
    }
}
