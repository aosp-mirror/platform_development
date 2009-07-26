/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.sdk;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.build.Messages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Wrapper to access dex.jar through reflection.
 * <p/>Since there is no proper api to call the method in the dex library, this wrapper is going
 * to access it through reflection.
 */
public final class DexWrapper {
    
    private final static String DEX_MAIN = "com.android.dx.command.dexer.Main"; //$NON-NLS-1$
    private final static String DEX_CONSOLE = "com.android.dx.command.DxConsole"; //$NON-NLS-1$
    private final static String DEX_ARGS = "com.android.dx.command.dexer.Main$Arguments"; //$NON-NLS-1$
    
    private final static String MAIN_RUN = "run"; //$NON-NLS-1$
    
    private Method mRunMethod;

    private Constructor<?> mArgConstructor;
    private Field mArgOutName;
    private Field mArgVerbose;
    private Field mArgJarOutput;
    private Field mArgFileNames;

    private Field mConsoleOut;
    private Field mConsoleErr;
    
    /**
     * Loads the dex library from a file path.
     * 
     * The loaded library can be used via
     * {@link DexWrapper#run(String, String[], boolean, PrintStream, PrintStream)}.
     * 
     * @param osFilepath the location of the dex.jar file.
     * @return an IStatus indicating the result of the load.
     */
    public synchronized IStatus loadDex(String osFilepath) {
        try {
            File f = new File(osFilepath);
            if (f.isFile() == false) {
                return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, String.format(
                        Messages.DexWrapper_s_does_not_exists, osFilepath));
            }
            URL url = f.toURL();
    
            URLClassLoader loader = new URLClassLoader(new URL[] { url },
                    DexWrapper.class.getClassLoader());
            
            // get the classes.
            Class<?> mainClass = loader.loadClass(DEX_MAIN);
            Class<?> consoleClass = loader.loadClass(DEX_CONSOLE);
            Class<?> argClass = loader.loadClass(DEX_ARGS);
            
            try {
                // now get the fields/methods we need
                mRunMethod = mainClass.getMethod(MAIN_RUN, argClass);
                
                mArgConstructor = argClass.getConstructor();
                mArgOutName = argClass.getField("outName"); //$NON-NLS-1$
                mArgJarOutput = argClass.getField("jarOutput"); //$NON-NLS-1$
                mArgFileNames = argClass.getField("fileNames"); //$NON-NLS-1$
                mArgVerbose = argClass.getField("verbose"); //$NON-NLS-1$
                
                mConsoleOut = consoleClass.getField("out"); //$NON-NLS-1$
                mConsoleErr = consoleClass.getField("err"); //$NON-NLS-1$
                
            } catch (SecurityException e) {
                return createErrorStatus(Messages.DexWrapper_SecuryEx_Unable_To_Find_API, e);
            } catch (NoSuchMethodException e) {
                return createErrorStatus(Messages.DexWrapper_SecuryEx_Unable_To_Find_Method, e);
            } catch (NoSuchFieldException e) {
                return createErrorStatus(Messages.DexWrapper_SecuryEx_Unable_To_Find_Field, e);
            }

            return Status.OK_STATUS;
        } catch (MalformedURLException e) {
            // really this should not happen.
            return createErrorStatus(
                    String.format(Messages.DexWrapper_Failed_to_load_s, osFilepath), e);
        } catch (ClassNotFoundException e) {
            return createErrorStatus(
                    String.format(Messages.DexWrapper_Failed_to_load_s, osFilepath), e);
        }
    }
    
    /**
     * Runs the dex command.
     * @param osOutFilePath the OS path to the outputfile (classes.dex
     * @param osFilenames list of input source files (.class and .jar files)
     * @param verbose verbose mode.
     * @param outStream the stdout console
     * @param errStream the stderr console
     * @return the integer return code of com.android.dx.command.dexer.Main.run()
     * @throws CoreException
     */
    public synchronized int run(String osOutFilePath, String[] osFilenames,
            boolean verbose, PrintStream outStream, PrintStream errStream) throws CoreException {
        
        try {
            // set the stream
            mConsoleErr.set(null /* obj: static field */, errStream);
            mConsoleOut.set(null /* obj: static field */, outStream);
            
            // create the Arguments object.
            Object args = mArgConstructor.newInstance();
            mArgOutName.set(args, osOutFilePath);
            mArgFileNames.set(args, osFilenames);
            mArgJarOutput.set(args, false);
            mArgVerbose.set(args, verbose);
            
            // call the run method
            Object res = mRunMethod.invoke(null /* obj: static method */, args);
            
            if (res instanceof Integer) {
                return ((Integer)res).intValue();
            }
        
            return -1;
        } catch (IllegalAccessException e) {
            throw new CoreException(createErrorStatus(
                    String.format(Messages.DexWrapper_Unable_To_Execute_Dex_s, e.getMessage()), e));
        } catch (InstantiationException e) {
            throw new CoreException(createErrorStatus(
                    String.format(Messages.DexWrapper_Unable_To_Execute_Dex_s, e.getMessage()), e));
        } catch (InvocationTargetException e) {
            throw new CoreException(createErrorStatus(
                    String.format(Messages.DexWrapper_Unable_To_Execute_Dex_s, e.getMessage()), e));
        }
    }
    
    private static IStatus createErrorStatus(String message, Exception e) {
        AdtPlugin.log(e, message);
        AdtPlugin.printErrorToConsole(Messages.DexWrapper_Dex_Loader, message);
        
        return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, message, e);
    }
}
