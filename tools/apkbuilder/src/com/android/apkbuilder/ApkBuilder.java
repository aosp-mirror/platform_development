/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.apkbuilder;

import com.android.jarutils.DebugKeyProvider;
import com.android.jarutils.JavaResourceFilter;
import com.android.jarutils.SignedJarBuilder;
import com.android.jarutils.DebugKeyProvider.KeytoolException;
import com.android.prefs.AndroidLocation.AndroidLocationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Command line APK builder with signing support.
 */
public final class ApkBuilder {
    
    private final static Pattern PATTERN_JAR_EXT = Pattern.compile("^.+\\.jar$",
            Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_NATIVELIB_EXT = Pattern.compile("^.+\\.so$",
            Pattern.CASE_INSENSITIVE);
    
    private final static String NATIVE_LIB_ROOT = "lib/";

    /**
     * A File to be added to the APK archive.
     * <p/>This includes the {@link File} representing the file and its path in the archive.
     */
    public final static class ApkFile {
        String archivePath;
        File file;

        ApkFile(File file, String path) {
            this.file = file;
            this.archivePath = path;
        }
    }

    private JavaResourceFilter mResourceFilter = new JavaResourceFilter();
    private boolean mVerbose = false;
    private boolean mSignedPackage = true;
    /** the optional type of the debug keystore. If <code>null</code>, the default */
    private String mStoreType = null;

    /**
     * @param args
     */
    public static void main(String[] args) {
        new ApkBuilder().run(args);
    }
    
    public void setVerbose(boolean verbose) {
        mVerbose = verbose;
    }
    
    public void setSignedPackage(boolean signedPackage) {
        mSignedPackage = signedPackage;
    }

    private void run(String[] args) {
        if (args.length < 1) {
            printUsageAndQuit();
        }

        try {
            // read the first args that should be a file path
            File outFile = getOutFile(args[0]);
    
            ArrayList<FileInputStream> zipArchives = new ArrayList<FileInputStream>();
            ArrayList<File> archiveFiles = new ArrayList<File>();
            ArrayList<ApkFile> javaResources = new ArrayList<ApkFile>();
            ArrayList<FileInputStream> resourcesJars = new ArrayList<FileInputStream>();
            ArrayList<ApkFile> nativeLibraries = new ArrayList<ApkFile>();
    
            int index = 1;
            do {
                String argument = args[index++];
    
                if ("-v".equals(argument)) {
                    mVerbose = true;
                } else if ("-u".equals(argument)) {
                    mSignedPackage = false;
                } else if ("-z".equals(argument)) {
                    // quick check on the next argument.
                    if (index == args.length) printUsageAndQuit();
                    
                    try {
                        FileInputStream input = new FileInputStream(args[index++]);
                        zipArchives.add(input);
                    } catch (FileNotFoundException e) {
                        printAndExit(e.getMessage());
                    }
                } else if ("-f". equals(argument)) {
                    // quick check on the next argument.
                    if (index == args.length) printUsageAndQuit();
    
                    archiveFiles.add(getInputFile(args[index++]));
                } else if ("-rf". equals(argument)) {
                    // quick check on the next argument.
                    if (index == args.length) printUsageAndQuit();
    
                    processSourceFolderForResource(args[index++], javaResources);
                } else if ("-rj". equals(argument)) {
                    // quick check on the next argument.
                    if (index == args.length) printUsageAndQuit();
                    
                    processJarFolder(args[index++], resourcesJars);
                } else if ("-nf".equals(argument)) {
                    // quick check on the next argument.
                    if (index == args.length) printUsageAndQuit();
                    
                    String parameter = args[index++];
                    File f = new File(parameter);
    
                    // compute the offset to get the relative path
                    int offset = parameter.length();
                    if (parameter.endsWith(File.separator) == false) {
                        offset++;
                    }
    
                    processNativeFolder(offset, f, nativeLibraries);
                } else if ("-storetype".equals(argument)) {
                    // quick check on the next argument.
                    if (index == args.length) printUsageAndQuit();
                    
                    mStoreType  = args[index++];
                } else {
                    printAndExit("Unknown argument: " + argument);
                }
            } while (index < args.length);
            
            createPackage(outFile, zipArchives, archiveFiles, javaResources, resourcesJars,
                    nativeLibraries);
        } catch (IllegalArgumentException e) {
            printAndExit(e.getMessage());
        } catch (FileNotFoundException e) {
            printAndExit(e.getMessage());
        }
    }


    private File getOutFile(String filepath) {
        File f = new File(filepath);
        
        if (f.isDirectory()) {
            printAndExit(filepath + " is a directory!");
        }
        
        if (f.exists()) { // will be a file in this case.
            if (f.canWrite() == false) {
                printAndExit("Cannot write " + filepath);
            }
        } else {
            try {
                if (f.createNewFile() == false) {
                    printAndExit("Failed to create " + filepath);
                }
            } catch (IOException e) {
                printAndExit("Failed to create '" + filepath + "' : " + e.getMessage());
            }
        }
        
        return f;
    }

    public static File getInputFile(String filepath) throws IllegalArgumentException {
        File f = new File(filepath);
        
        if (f.isDirectory()) {
            throw new IllegalArgumentException(filepath + " is a directory!");
        }
        
        if (f.exists()) {
            if (f.canRead() == false) {
                throw new IllegalArgumentException("Cannot read " + filepath);
            }
        } else {
            throw new IllegalArgumentException(filepath + " does not exists!");
        }

        return f;
    }

    /**
     * Processes a source folder and adds its java resources to a given list of {@link ApkFile}.
     * @param folderPath the path to the source folder.
     * @param javaResources the list of {@link ApkFile} to fill.
     */
    public static void processSourceFolderForResource(String folderPath,
            ArrayList<ApkFile> javaResources) {
        
        File folder = new File(folderPath);
        
        if (folder.isDirectory()) {
            // file is a directory, process its content.
            File[] files = folder.listFiles();
            for (File file : files) {
                processFileForResource(file, null, javaResources);
            }
        } else {
            // not a directory? output error and quit.
            if (folder.exists()) {
                throw new IllegalArgumentException(folderPath + " is not a folder!");
            } else {
                throw new IllegalArgumentException(folderPath + " does not exist!");
            }
        }
    }
    
    public static void processJarFolder(String parameter, ArrayList<FileInputStream> resourcesJars)
            throws FileNotFoundException {
        File f = new File(parameter);
        if (f.isDirectory()) {
            String[] files = f.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return PATTERN_JAR_EXT.matcher(name).matches();
                }
            });

            for (String file : files) {
                String path = f.getAbsolutePath() + File.separator + file;
                FileInputStream input = new FileInputStream(path);
                resourcesJars.add(input);
            }
        } else {
            FileInputStream input = new FileInputStream(parameter);
            resourcesJars.add(input);
        }
    }

    
    /**
     * Processes a {@link File} that could be a {@link ApkFile}, or a folder containing
     * java resources.
     * @param file the {@link File} to process.
     * @param path the relative path of this file to the source folder. Can be <code>null</code> to
     * identify a root file. 
     * @param javaResources the list of {@link ApkFile} object to fill.
     */
    private static void processFileForResource(File file, String path,
            ArrayList<ApkFile> javaResources) {
        if (file.isDirectory()) {
            // a directory? we check it
            if (JavaResourceFilter.checkFolderForPackaging(file.getName())) {
                // if it's valid, we append its name to the current path.
                if (path == null) {
                    path = file.getName();
                } else {
                    path = path + "/" + file.getName();
                }

                // and process its content.
                File[] files = file.listFiles();
                for (File contentFile : files) {
                    processFileForResource(contentFile, path, javaResources);
                }
            }
        } else {
            // a file? we check it
            if (JavaResourceFilter.checkFileForPackaging(file.getName())) {
                // we append its name to the current path
                if (path == null) {
                    path = file.getName();
                } else {
                    path = path + "/" + file.getName();
                }

                // and add it to the list.
                javaResources.add(new ApkFile(file, path));
            }
        }
    }
    
    /**
     * Process a {@link File} for native library inclusion.
     * @param offset the length of the root folder (used to compute relative path)
     * @param f the {@link File} to process
     * @param nativeLibraries the array to add native libraries.
     */
    public static void processNativeFolder(int offset, File f, ArrayList<ApkFile> nativeLibraries) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            
            if (children != null) {
                for (File child : children) {
                    processNativeFolder(offset, child, nativeLibraries);
                }
            }
        } else if (f.isFile()) {
            if (PATTERN_NATIVELIB_EXT.matcher(f.getName()).matches()) {
                String path = NATIVE_LIB_ROOT + 
                        f.getAbsolutePath().substring(offset).replace('\\', '/');
                
                nativeLibraries.add(new ApkFile(f, path));
            }
        }
    }

    /**
     * Creates the application package
     * @param outFile 
     * @param zipArchives
     * @param resourcesJars 
     * @param files 
     * @param javaResources 
     * keystore type of the Java VM is used.
     */
    public void createPackage(File outFile, ArrayList<FileInputStream> zipArchives,
            ArrayList<File> files, ArrayList<ApkFile> javaResources,
            ArrayList<FileInputStream> resourcesJars, ArrayList<ApkFile> nativeLibraries) {
        
        // get the debug key
        try {
            SignedJarBuilder builder;

            if (mSignedPackage) {
                System.err.println(String.format("Using keystore: %s",
                        DebugKeyProvider.getDefaultKeyStoreOsPath()));
                
                
                DebugKeyProvider keyProvider = new DebugKeyProvider(
                        null /* osKeyPath: use default */,
                        mStoreType, null /* IKeyGenOutput */);
                PrivateKey key = keyProvider.getDebugKey();
                X509Certificate certificate = (X509Certificate)keyProvider.getCertificate();
                
                if (key == null) {
                    throw new IllegalArgumentException("Unable to get debug signature key");
                }
                
                // compare the certificate expiration date
                if (certificate != null && certificate.getNotAfter().compareTo(new Date()) < 0) {
                    // TODO, regenerate a new one.
                    throw new IllegalArgumentException("Debug Certificate expired on " +
                            DateFormat.getInstance().format(certificate.getNotAfter()));
                }

                builder = new SignedJarBuilder(
                        new FileOutputStream(outFile.getAbsolutePath(), false /* append */), key,
                        certificate);
            } else {
                builder = new SignedJarBuilder(
                        new FileOutputStream(outFile.getAbsolutePath(), false /* append */),
                        null /* key */, null /* certificate */);
            }

            // add the archives
            for (FileInputStream input : zipArchives) {
                builder.writeZip(input, null /* filter */);
            }

            // add the single files
            for (File input : files) {
                // always put the file at the root of the archive in this case
                builder.writeFile(input, input.getName());
                if (mVerbose) {
                    System.err.println(String.format("%1$s => %2$s", input.getAbsolutePath(),
                            input.getName()));
                }
            }
            
            // add the java resource from the source folders.
            for (ApkFile resource : javaResources) {
                builder.writeFile(resource.file, resource.archivePath);
                if (mVerbose) {
                    System.err.println(String.format("%1$s => %2$s",
                            resource.file.getAbsolutePath(), resource.archivePath));
                }
            }

            // add the java resource from jar files.
            for (FileInputStream input : resourcesJars) {
                builder.writeZip(input, mResourceFilter);
            }
            
            // add the native files
            for (ApkFile file : nativeLibraries) {
                builder.writeFile(file.file, file.archivePath);
                if (mVerbose) {
                    System.err.println(String.format("%1$s => %2$s", file.file.getAbsolutePath(),
                            file.archivePath));
                }
            }
            
            // close and sign the application package.
            builder.close();
        } catch (KeytoolException e) {
            if (e.getJavaHome() == null) {
                throw new IllegalArgumentException(e.getMessage() + 
                        "\nJAVA_HOME seems undefined, setting it will help locating keytool automatically\n" +
                        "You can also manually execute the following command\n:" +
                        e.getCommandLine());
            } else {
                throw new IllegalArgumentException(e.getMessage() + 
                        "\nJAVA_HOME is set to: " + e.getJavaHome() +
                        "\nUpdate it if necessary, or manually execute the following command:\n" +
                        e.getCommandLine());
            }
        } catch (AndroidLocationException e) {
            throw new IllegalArgumentException(e);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void printUsageAndQuit() {
        // 80 cols marker:  01234567890123456789012345678901234567890123456789012345678901234567890123456789
        System.err.println("A command line tool to package an Android application from various sources.");
        System.err.println("Usage: apkbuilder <out archive> [-v][-u][-storetype STORE_TYPE] [-z inputzip]");
        System.err.println("            [-f inputfile] [-rf input-folder] [-rj -input-path]");
        System.err.println("");
        System.err.println("    -v      Verbose.");
        System.err.println("    -u      Creates an unsigned package.");
        System.err.println("    -storetype Forces the KeyStore type. If ommited the default is used.");
        System.err.println("");
        System.err.println("    -z      Followed by the path to a zip archive.");
        System.err.println("            Adds the content of the application package.");
        System.err.println("");
        System.err.println("    -f      Followed by the path to a file.");
        System.err.println("            Adds the file to the application package.");
        System.err.println("");
        System.err.println("    -rf     Followed by the path to a source folder.");
        System.err.println("            Adds the java resources found in that folder to the application");
        System.err.println("            package, while keeping their path relative to the source folder.");
        System.err.println("");
        System.err.println("    -rj     Followed by the path to a jar file or a folder containing");
        System.err.println("            jar files.");
        System.err.println("            Adds the java resources found in the jar file(s) to the application");
        System.err.println("            package.");
        System.err.println("");
        System.err.println("    -nf     Followed by the root folder containing native libraries to");
        System.err.println("            include in the application package.");
        
        System.exit(1);
    }
    
    private void printAndExit(String... messages) {
        for (String message : messages) {
            System.err.println(message);
        }
        System.exit(1);
    }
}
