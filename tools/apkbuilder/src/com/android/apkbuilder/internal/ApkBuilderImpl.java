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

package com.android.apkbuilder.internal;

import com.android.apkbuilder.ApkBuilder.WrongOptionException;
import com.android.apkbuilder.ApkBuilder.ApkCreationException;
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
import java.util.Collection;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Command line APK builder with signing support.
 */
public final class ApkBuilderImpl {

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

    public void setVerbose(boolean verbose) {
        mVerbose = verbose;
    }

    public void setSignedPackage(boolean signedPackage) {
        mSignedPackage = signedPackage;
    }

    public void run(String[] args) throws WrongOptionException, FileNotFoundException,
            ApkCreationException {
        if (args.length < 1) {
            throw new WrongOptionException("No options specified");
        }

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
                if (index == args.length)  {
                    throw new WrongOptionException("Missing value for -z");
                }

                try {
                    FileInputStream input = new FileInputStream(args[index++]);
                    zipArchives.add(input);
                } catch (FileNotFoundException e) {
                    throw new ApkCreationException("-z file is not found");
                }
            } else if ("-f". equals(argument)) {
                // quick check on the next argument.
                if (index == args.length) {
                    throw new WrongOptionException("Missing value for -f");
                }

                archiveFiles.add(getInputFile(args[index++]));
            } else if ("-rf". equals(argument)) {
                // quick check on the next argument.
                if (index == args.length) {
                    throw new WrongOptionException("Missing value for -rf");
                }

                processSourceFolderForResource(args[index++], javaResources);
            } else if ("-rj". equals(argument)) {
                // quick check on the next argument.
                if (index == args.length) {
                    throw new WrongOptionException("Missing value for -rj");
                }

                processJarFolder(args[index++], resourcesJars);
            } else if ("-nf".equals(argument)) {
                // quick check on the next argument.
                if (index == args.length) {
                    throw new WrongOptionException("Missing value for -nf");
                }

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
                if (index == args.length) {
                    throw new WrongOptionException("Missing value for -storetype");
                }

                mStoreType  = args[index++];
            } else {
                throw new WrongOptionException("Unknown argument: " + argument);
            }
        } while (index < args.length);

        createPackage(outFile, zipArchives, archiveFiles, javaResources, resourcesJars,
                nativeLibraries);
    }


    private File getOutFile(String filepath) throws ApkCreationException {
        File f = new File(filepath);

        if (f.isDirectory()) {
            throw new ApkCreationException(filepath + " is a directory!");
        }

        if (f.exists()) { // will be a file in this case.
            if (f.canWrite() == false) {
                throw new ApkCreationException("Cannot write " + filepath);
            }
        } else {
            try {
                if (f.createNewFile() == false) {
                    throw new ApkCreationException("Failed to create " + filepath);
                }
            } catch (IOException e) {
                throw new ApkCreationException(
                        "Failed to create '" + filepath + "' : " + e.getMessage());
            }
        }

        return f;
    }

    public static File getInputFile(String filepath) throws ApkCreationException {
        File f = new File(filepath);

        if (f.isDirectory()) {
            throw new ApkCreationException(filepath + " is a directory!");
        }

        if (f.exists()) {
            if (f.canRead() == false) {
                throw new ApkCreationException("Cannot read " + filepath);
            }
        } else {
            throw new ApkCreationException(filepath + " does not exists!");
        }

        return f;
    }

    /**
     * Processes a source folder and adds its java resources to a given list of {@link ApkFile}.
     * @param folderPath the path to the source folder.
     * @param javaResources the list of {@link ApkFile} to fill.
     * @throws ApkCreationException
     */
    public static void processSourceFolderForResource(String folderPath,
            ArrayList<ApkFile> javaResources) throws ApkCreationException {

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
                throw new ApkCreationException(folderPath + " is not a folder!");
            } else {
                throw new ApkCreationException(folderPath + " does not exist!");
            }
        }
    }

    public static void processJarFolder(String parameter, Collection<FileInputStream> resourcesJars)
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
     * @param javaResources the Collection of {@link ApkFile} object to fill.
     */
    private static void processFileForResource(File file, String path,
            Collection<ApkFile> javaResources) {
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
     * @param nativeLibraries the collection to add native libraries to.
     */
    public static void processNativeFolder(int offset, File f,
            Collection<ApkFile> nativeLibraries) {
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
     * @param outFile the package file to create
     * @param zipArchives the list of zip archive
     * @param files the list of files to include in the archive
     * @param javaResources the list of java resources from the source folders.
     * @param resourcesJars the list of jar files from which to take java resources
     * @throws ApkCreationException
     */
    public void createPackage(File outFile, Iterable<? extends FileInputStream> zipArchives,
            Iterable<? extends File> files, Iterable<? extends ApkFile> javaResources,
            Iterable<? extends FileInputStream> resourcesJars,
            Iterable<? extends ApkFile> nativeLibraries) throws ApkCreationException {

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
                    throw new ApkCreationException("Unable to get debug signature key");
                }

                // compare the certificate expiration date
                if (certificate != null && certificate.getNotAfter().compareTo(new Date()) < 0) {
                    // TODO, regenerate a new one.
                    throw new ApkCreationException("Debug Certificate expired on " +
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
                throw new ApkCreationException(e.getMessage() +
                        "\nJAVA_HOME seems undefined, setting it will help locating keytool automatically\n" +
                        "You can also manually execute the following command\n:" +
                        e.getCommandLine());
            } else {
                throw new ApkCreationException(e.getMessage() +
                        "\nJAVA_HOME is set to: " + e.getJavaHome() +
                        "\nUpdate it if necessary, or manually execute the following command:\n" +
                        e.getCommandLine());
            }
        } catch (AndroidLocationException e) {
            throw new ApkCreationException(e);
        } catch (Exception e) {
            throw new ApkCreationException(e);
        }
    }
}
