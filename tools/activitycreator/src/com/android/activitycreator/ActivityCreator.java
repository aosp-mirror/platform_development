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

package com.android.activitycreator;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Creates the basic files needed to get an Android project up and running. Also
 * allows creation of IntelliJ project files.
 *
 * @hide
 */
public class ActivityCreator {
    // FIXME: target platform must be provided by the user
    private final String mTargetPlatform = "android-1.1";

    /** Whether we are in silent mode (i.e.: don't print any non-error messages) */
    private boolean mSilent;
    
    /** Path to tools */
    private String mToolsDir;
    
    /** Path to SDK */
    private String mSdkDir;
    
    /** Path to target platform's template folder */
    private String mTemplateDir;

    /** Path to tools/lib for the build templates */
    private String mLibDir;
    
    /** Path to output */
    private String mOutDir;
    
    /** IDE to generate for */
    private String mIde;
    
    /** Data used for the "alias" mode */
    private String mAliasData;
    
    /** Application label used in the "alias" mode */
    private String mApplicationLabel;
    
    /** Package of Activity */
    private String mPackageFull;
    
    /**
     * Constructs object.
     * @param args arguments passed to the program
     */
    public ActivityCreator(String[] args) {
        mSilent = false;
        
        mIde = "";
        
        initPathVars();
        parseArgs(args);
        
        if (isAliasProject()) {
            setupAliasProject();
        } else {
            setupProject();
        }
    }

    /**
     * Initializes the path variables based on location of this class.
     */
    private void initPathVars() {
        /* We get passed a property for the tools dir */
        String toolsDirProp = System.getProperty("com.android.activitycreator.toolsdir");
        if (toolsDirProp == null) {
            // for debugging, it's easier to override using the process environment
            toolsDirProp = System.getenv("com.android.activitycreator.toolsdir");
        }
        if (toolsDirProp == null) {
            printHelpAndExit("ERROR: The tools directory property is not set, please make sure you are executing activitycreator or activitycreator.bat");
        }

        /* Absolute path */
        File toolsDir = new File(toolsDirProp);
        try {
            mToolsDir = toolsDir.getCanonicalPath();
        } catch (IOException e) {
            printHelpAndExit("ERROR: Could not determine the tools directory.");
        }
        toolsDir = new File(mToolsDir);
        
        mSdkDir = toolsDir.getParent();
        mLibDir = mToolsDir + File.separator + "lib";
        mTemplateDir = mSdkDir + File.separator + "platforms" + File.separator +
                mTargetPlatform + File.separator + "templates";
        try {
            mOutDir = new File("").getCanonicalPath();
        } catch (IOException e) {
            printHelpAndExit("ERROR: Could not determine the current directory.");
        }
        
        if (!toolsDir.exists()) {
            printHelpAndExit("ERROR: Tools directory does not exist.");
        }
        
        if (!(new File(mSdkDir).exists())) {
            printHelpAndExit("ERROR: SDK directory does not exist.");
        }

        if (!(new File(mTemplateDir).exists())) {
            printHelpAndExit("ERROR: Target platform templates directory does not exist.");
        }

        if (!(new File(mLibDir).exists())) {
            printHelpAndExit("ERROR: Library directory does not exist.");
        }
    }

    /**
     * Parses command-line arguments, or prints help/usage and exits if error.
     * @param args arguments passed to the program
     */
    private void parseArgs(String[] args) {
        final int numArgs = args.length;
        
        try {
            int argPos = 0;
            for (; argPos < numArgs; argPos++) {
                final String arg = args[argPos];
                if (arg.equals("-o") || arg.equals("-out") || arg.equals("--out")) {
                    argPos++;
                    mOutDir = args[argPos];
                } else if (arg.equals("-d") || arg.equals("-data") || arg.equals("--data")) {
                    argPos++;
                    mAliasData = args[argPos];
                } else if (arg.equals("-l") || arg.equals("-label") || arg.equals("--label")) {
                    argPos++;
                    mApplicationLabel = args[argPos];
                } else if (arg.equals("-i") || arg.equals("-ide") || arg.equals("--ide")) {
                    argPos++;
                    mIde = args[argPos].toLowerCase();
                } else if (arg.equals("-h") || arg.equals("-help") || arg.equals("--help")) {
                    printHelpAndExit(null);
                } else if (arg.equals("-s") || arg.equals("-silent") || arg.equals("--silent")) {
                    mSilent = true;
                } else {
                    if (mPackageFull == null) {
                        mPackageFull = extractPackageFromManifest(args[argPos]);
                        if (mPackageFull == null) {
                            mPackageFull = args[argPos];
                        }
                    } else {
                        /* Package has already been set, so this is an extra argument */
                        printHelpAndExit("ERROR: Too many arguments: %1$s", args[argPos]);
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            /* Any OOB triggers help */
            printHelpAndExit("ERROR: Not enough arguments.");
        }

        if (isStringEmpty(mPackageFull)) {
            printHelpAndExit("ERROR: Please enter a package.");
        }

        if (isStringEmpty(mOutDir)) {
            printHelpAndExit("ERROR: Please enter an output directory.");
        }
        
        // we need both application label and url for the "alias" mode
        if (isStringEmpty(mAliasData) ^ isStringEmpty(mApplicationLabel)) {
            printHelpAndExit("ERROR: Alias projects require both --data and --label.");
        }
        
        if (mIde.equals("eclipse")) {
            printHelpAndExit("ERROR: For Eclipse support, please install the Eclipse ADT plugin and use its New Project Wizard.");
        }
    }
    
    /**
     * Prints the help/usage and exits.
     * @param errorFormat Optional error message to print prior to usage using String.format 
     * @param args Arguments for String.format
     */
    private void printHelpAndExit(String errorFormat, Object... args) {
        if (errorFormat != null) {
            System.err.println(String.format(errorFormat, args));
        }
        
        /*
         * usage should fit in 80 columns
         *   12345678901234567890123456789012345678901234567890123456789012345678901234567890
         */
        final String usage = "\n" +
            "Activity Creator Script.\n" +
            "\n" +
            "Usage:\n" +
            "  activitycreator --out outdir [--ide intellij] your.package.name.ActivityName\n" +
            "  activitycreator --out outdir [--ide intellij] path/to/AndroidManifest.xml\n" +
            "  activitycreator --out outdir --data data --label app_label your.package.name\n" +
            "\n" +
            "With both the --data and --label options, ActivityCreator creates the structure\n" +
            "of an 'alias' Android application.\n" +
            "    An Alias project is an application with no code that simply launches an\n" +
            "    android.intent.action.VIEW intent with the provided data.\n" +
            "    The following will be created (existing files will not be modified):\n" +
            "      - AndroidManifest.xml: The application manifest file.\n" +
            "      - build.xml: An Ant script to build/package the application.\n" +
            "      - res/values/strings.xml: an XML file defining the application label\n" +
            "          string resource.\n" +
            "      - res/xml/alias.xml: an XML file defining the VIEW intent and its data.\n " +
            "\n" +
            "Without --data and --label, ActivityCreator creates the structure of a minimal\n" +
            "Android application.\n" +
            "    The following will be created (existing files will not be modified):\n" +
            "      - AndroidManifest.xml: The application manifest file.\n" +
            "      - build.xml: An Ant script to build/package the application.\n" +
            "      - res : The resource directory.\n" +
            "      - src : The source directory.\n" +
            "      - src/your/package/name/ActivityName.java the Activity java class.\n" +
            "          packageName is a fully qualified java Package in the format\n" +
            "          <package1>.<package2>... (with at least two components).\n" +
            "      - bin : The output folder for the build script.\n" +
            "\n" +
            "Options:\n" +
            " -o <folder>, --out <folder>\n" +
            "         Specifies where to create the files/folders.\n" +
            " -i intellij, --ide intellij\n" +
            "         Creates project files for IntelliJ (non alias application only)\n" +
            " -d <data-string>, --data <data-string>\n" +
            "         The data passed to the VIEW intent. For instance, this can be a url,\n" +
            "         such as http://www.android.com\n" +
            " -l <app-label>, --label <app-label>\n" +
            "         The name the alias application will have in the HOME screen.\n" +
            " -h, --help\n" +
            "         Display this help.\n" +
            " -s, --silent\n" +
            "         Silent mode.\n" +
            "\n" +
            "For Eclipse support, please use the ADT plugin.\n";
        
        println(usage);
        System.exit(1);
    }
    
    /**
     * Installs a destination file that is based on a code template file at the source.
     * For each match of each key in keywords will be replaced with its
     * corresponding value in the destination file.
     * 
     * Invokes {@link #installProjectTemplate(String, String, Map, boolean, String)} with
     * the main project output directory (#mOutDir) as the last argument.
     * 
     * @param source the name of to the source template file
     * @param dest the path to the destination file
     * @param keywords in the destination file, the keys will be replaced by their values
     * @param force True to force writing the file even if it already exists  
     * 
     * @see #installProjectTemplate(String, String, Map, boolean, String)
     */
    private void installProjectTemplate(String source, String dest,
            Map<String, String> keywords, boolean force) {
        installProjectTemplate(source, dest, keywords, force, mOutDir);
    }
    
    /**
     * Installs a destination file that is based on a code template file at the source.
     * For each match of each key in keywords will be replaced with its
     * corresponding value in the destination file.
     * 
     * @param source the name of to the source template file
     * @param dest the path to the destination file
     * @param keywords in the destination file, the keys will be replaced by their values
     * @param force True to force writing the file even if it already exists  
     * @param outDir the output directory to copy the template file to
     */
    private void installProjectTemplate(String source, String dest,
            Map<String, String> keywords, boolean force, String outDir) {
        final String sourcePath = mTemplateDir + File.separator + source;
        final String destPath = outDir + File.separator + dest;

        installFullPathTemplate(sourcePath, destPath, keywords, force);
    }

    /**
     * Installs a destination file that is based on a build template file at the source.
     * For each match of each key in keywords will be replaced with its
     * corresponding value in the destination file.
     * 
     * Invokes {@link #installBuildTemplate(String, String, Map, boolean, String)} with
     * the main project output directory (#mOutDir) as the last argument.
     * 
     * @param source the name of to the source template file
     * @param dest the path to the destination file
     * @param keywords in the destination file, the keys will be replaced by their values
     * @param force True to force writing the file even if it already exists
     * 
     * @see #installBuildTemplate(String, String, Map, boolean, String)
     */
    private void installBuildTemplate(String source, String dest,
            Map<String, String> keywords, boolean force) {
        installBuildTemplate(source, dest, keywords, force, mOutDir);
    }

    /**
     * Installs a destination file that is based on a build template file at the source.
     * For each match of each key in keywords will be replaced with its
     * corresponding value in the destination file.
     * 
     * @param source the name of to the source template file
     * @param dest the path to the destination file
     * @param keywords in the destination file, the keys will be replaced by their values
     * @param force True to force writing the file even if it already exists  
     * @param outDir the output directory to copy the template file to
     */
    private void installBuildTemplate(String source, String dest,
            Map<String, String> keywords, boolean force, String outDir) {
        final String sourcePath = mLibDir + File.separator + source;
        final String destPath = outDir + File.separator + dest;

        installFullPathTemplate(sourcePath, destPath, keywords, force);
    }

    /**
     * Installs a destination file that is based on the template file at source.
     * For each match of each key in keywords will be replaced with its
     * corresponding value in the destination file.
     * 
     * @param sourcePath the full path to the source template file
     * @param destPath the full path to the destination file
     * @param keywords in the destination file, the keys will be replaced by their values
     * @param force True to force writing the file even if it already exists  
     */
    private void installFullPathTemplate(String sourcePath, String destPath,
            Map<String, String> keywords, boolean force) {
        final File destPathFile = new File(destPath);
        if (!force && destPathFile.exists()) {
            println("WARNING! The file %1$s already exists and will not be overwritten!\n",
                    destPathFile.getName());
            return;
        }
        
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(destPathFile));
            BufferedReader in = new BufferedReader(new FileReader(sourcePath));
            String line;
            
            while ((line = in.readLine()) != null) {
                for (String key : keywords.keySet()) {
                    line = line.replace(key, keywords.get(key));
                }
                
                out.write(line);
                out.newLine();
            }
            
            out.close();
            in.close();
        } catch (Exception e) {
            printHelpAndExit("ERROR: Could not access %1$s: %2$s", destPath, e.getMessage());
        }
        
        println("Added file %1$s", destPath);
    }

    /**
     * Set up the Android-related files
     */
    private void setupProject() {
        String packageName = null;
        String activityName = null;
        String activityTestName = null;
        try {
            /* Grab package and Activity names */
            int lastPeriod = mPackageFull.lastIndexOf('.');
            packageName = mPackageFull.substring(0, lastPeriod);
            if (lastPeriod < mPackageFull.length() - 1) {
                activityName = mPackageFull.substring(lastPeriod+1);
                activityTestName = activityName + "Test";
            }
            
            if (packageName.indexOf('.') == -1) {
                printHelpAndExit("ERROR: Package name must be composed of at least two java identifiers.");
            }
        } catch (RuntimeException e) {
            printHelpAndExit("ERROR: Invalid package or activity name.");
        }
        
        println("Package: %1$s", packageName);
        println("Output directory: %1$s", mOutDir);
        String testsOutDir = mOutDir + File.separator + "tests";
        println("Tests directory: %1$s", testsOutDir);

        if (activityName != null) {
            println("Activity name: %1$s", activityName);
        }
        if (activityTestName != null) {
            println("ActivityTest name: %1$s", activityTestName);
        }
        
        final HashMap<String, String> keywords = createBaseKeywordMap();

        addTargetKeywords(keywords);

        keywords.put("PACKAGE", packageName);
        if (activityName != null) {
            keywords.put("ACTIVITY_NAME", activityName);
        }

        final String packagePath =
                stripString(packageName.replace(".", File.separator),
                        File.separatorChar);
        keywords.put("PACKAGE_PATH", packagePath);

        /* Other files that are always created */
        
        /* Make Activity java file */
        final String srcDir = "src" + File.separator + packagePath;
        createDirs(srcDir);
        if (isDirEmpty(srcDir, "Java") && activityName != null) {
            installProjectTemplate("java_file.template", srcDir + File.separator
                    + activityName + ".java", keywords, false /*force*/);
        }
        createDirs("bin");
        createDirs("libs");
        createDirs("res");

        /* Make ActivityTest java file */
        createDirs(srcDir, testsOutDir);
        if (isDirEmpty(srcDir, "Java", testsOutDir) && activityTestName != null) {
            installProjectTemplate("java_tests_file.template", srcDir + File.separator
                    + activityTestName + ".java", keywords, false, testsOutDir);
        }
        createDirs("bin", testsOutDir);
        createDirs("libs", testsOutDir);
        createDirs("res", testsOutDir);

        /* Make res files */
        final String valuesDir = "res" + File.separator + "values";
        createDirs(valuesDir);
        if (isDirEmpty(valuesDir, "Resource Values")) {
            installProjectTemplate("strings.template", valuesDir + File.separator
                    + "strings.xml", keywords, false /*force*/);
        }
        
        final String layoutDir = "res" + File.separator + "layout";
        createDirs(layoutDir);
        if (isDirEmpty(layoutDir, "Resource Layout")) {
            installProjectTemplate("layout.template", layoutDir + File.separator
                    + "main.xml", keywords, false /*force*/);
        }
        
        /* Make AndroidManifest.xml and build.xml files */
        installProjectTemplate("AndroidManifest.template", "AndroidManifest.xml",
                keywords, false /*force*/);
        
        installBuildTemplate("build.template", "build.xml", keywords, false /*force*/);
        installBuildTemplate("default.properties.template", "default.properties", keywords,
                true /*force*/);

        /* Make AndroidManifest.xml and build.xml files for tests */
        installProjectTemplate("AndroidManifest.tests.template", "AndroidManifest.xml",
                keywords, false /*force*/, testsOutDir);
        
        installBuildTemplate("build.template", "build.xml", keywords, false /*force*/, testsOutDir);
        installBuildTemplate("default.properties.template", "default.properties", keywords,
                true /*force*/, testsOutDir);

        if (mIde.equals("intellij")) {
            /* IntelliJ files */
            if (activityName != null) {
                installProjectTemplate("iml.template", activityName + ".iml", keywords,
                        false /*force*/);
                installProjectTemplate("ipr.template", activityName + ".ipr", keywords,
                        false /*force*/);
                installProjectTemplate("iws.template", activityName + ".iws", keywords,
                        false /*force*/);
            }
        } else if (!isStringEmpty(mIde)) {
            println("WARNING: Unknown IDE option \"%1$s\". No IDE files generated.",
                    mIde);
        }
    }

    
    /**
     * Sets up the files for an alias project.
     */
    private void setupAliasProject() {
        println("Package: %1$s", mPackageFull);
        println("Output directory: %1$s", mOutDir);
        println("URL: %1$s", mAliasData);
        println("Application label: %1$s", mApplicationLabel);
        
        if (mIde != null) {
            println("Alias project: ignoring --ide option.");
        }
        
        final HashMap<String, String> keywords = createBaseKeywordMap();
        keywords.put("PACKAGE", mPackageFull);
        keywords.put("ALIASDATA", mAliasData);

        // since strings.xml uses ACTIVITY_NAME for the application label we use it as well.
        // since we'll use a different AndroidManifest template this is not a problem.
        keywords.put("ACTIVITY_NAME", mApplicationLabel);
        
        /* Make res files */
        final String xmlDir = "res" + File.separator + "xml";
        createDirs(xmlDir);
        if (isDirEmpty(xmlDir, "Resource Xml")) {
            installProjectTemplate("alias.template", xmlDir + File.separator + "alias.xml",
                    keywords, false /*force*/);
        }

        final String valuesDir = "res" + File.separator + "values";
        createDirs(valuesDir);
        if (isDirEmpty(valuesDir, "Resource Values")) {
            installProjectTemplate("strings.template", valuesDir + File.separator
                    + "strings.xml", keywords, false /*force*/);
        }

        
        /* Make AndroidManifest.xml and build.xml files */
        installProjectTemplate("AndroidManifest.alias.template", "AndroidManifest.xml", keywords,
                false /*force*/);
        
        installBuildTemplate("build.alias.template", "build.xml", keywords, false /*force*/);
        installBuildTemplate("default.properties.template", "default.properties", keywords, true /*force*/);
    }

    
    private HashMap<String, String> createBaseKeywordMap() {
        final HashMap<String, String> keywords = new HashMap<String, String>();
        
        // When the tools & sdk folder on Windows get written to a properties file,
        // we need to transform \ in /, otherwise it gets interpreted as an escape character.
        // This is OK since ant can understand / as a separator even under Windows.
        // References:
        // - http://ant.apache.org/manual/CoreTasks/property.html
        // - http://java.sun.com/j2se/1.4.2/docs/api/java/util/Properties.html#load(java.io.InputStream)
        keywords.put("ANDROID_SDK_TOOLS",  mToolsDir.replace('\\', '/'));
        keywords.put("ANDROID_SDK_FOLDER", mSdkDir.replace('\\', '/'));
        
        return keywords;
    }
    
    private void addTargetKeywords(HashMap<String, String> keywords) {
        // FIXME: get this from the target selection
        keywords.put("TARGET_MODE",  "platform");
        keywords.put("TARGET_API",  "1"); // this is potentially wrong but since it's only used
                                          // when editing a project config, this is ok for now.
        keywords.put("TARGET_NAME",  "android"); // this is only used in add-on mode.
        keywords.put("TARGET_FOLDER",  mTargetPlatform);
        keywords.put("TARGET_MODE",  "platform");
    }

    
    /**
     * Called first.
     * @param args arguments passed to the program
     */
    public static void main(String[] args) {
        new ActivityCreator(args);
    }

    /**
     * Prints a message unless silence is enabled.
     * @param format Format for String.format
     * @param args Arguments for String.format
     */
    public void println(String format, Object... args) {
        if (!mSilent) {
            System.out.println(String.format(format, args));
        }
    }
    
    /**
     * Checks whether a string is "empty" (null or trimmed length == 0)
     * @param s the string to check
     * @return true if empty
     */
    public static boolean isStringEmpty(String s) {
        return (s == null) || (s.trim().length() == 0);
    }
    
    /**
     * Creates the path in the output directory along with any parent paths
     * that don't exist.
     *
     * Invokes ActivityCreator#createDirs(String, String) with
     * the main project output directory (#mOutDir) as the last argument.
     *
     * @param path the directory out/path that is created.
     *
     * @see com.android.activitycreator.ActivityCreator#createDirs(String, String)
     */
    public void createDirs(String path) {
        createDirs(path, mOutDir);
    }
    
    /**
     * Creates the path in the output directory along with any parent paths
     * that don't exist.
     * 
     * @param path the directory out/path that is created.
     * @param dir the directory in which the path to be created
     */
    public void createDirs(String path, String dir) {
        final File pathFile = new File(dir + File.separator + path);
        boolean existedBefore = true;

        if (!pathFile.exists()) {
            if (!pathFile.mkdirs()) {
                printHelpAndExit("ERROR: Could not create directory: %1$s", pathFile);
            }
            existedBefore = false;
        }

        if (pathFile.isDirectory()) {
            if (!pathFile.canWrite()) {
                printHelpAndExit("ERROR: Path is not writable: %1$s", pathFile);
            }
        } else {
            printHelpAndExit("ERROR: Path is not a directory: %1$s", pathFile);
        }

        if (!existedBefore) {
            try {
                println("Created directory %1$s", pathFile.getCanonicalPath());
            } catch (IOException e) {
                printHelpAndExit("ERROR: Could not determine canonical path of created directory");
            }
        }
    }

    /**
     * Checks whether the path in the output directory is empty
     *
     * Invokes ActivityCreator#isDirEmpty(String, String, String) with
     * the main project output directory (#mOutDir) as the last argument.
     *
     * @param path the out/path directory that is checked
     * @param message the logical name for what this path points to (used in
     *        warning message)
     * @return whether the directory is empty
     * @see com.android.activitycreator.ActivityCreator#isDirEmpty(String, String, String) 
     */
    public boolean isDirEmpty(String path, String message) {
        return isDirEmpty(path, message, mOutDir);
    }
    
    /**
     * Checks whether the path in the output directory is empty
     *
     * @param path the out/path directory that is checked
     * @param message the logical name for what this path points to (used in
     *        warning message)
     * @param outDir the output director to check
     * @return whether the directory is empty
     */
    public boolean isDirEmpty(String path, String message, String outDir) {
        File pathFile = new File(outDir + File.separator + path);

        String[] pathListing = pathFile.list();
        if ((pathListing != null) && (pathListing.length > 0)) {
            println("WARNING: There are already some %1$s files present. None will be created!",
                    message);
            return false;
        }

        return true;
    }

    /**
     * Strips the string of beginning and trailing characters (multiple
     * characters will be stripped, example stripString("..test...", '.')
     * results in "test";
     * 
     * @param s the string to strip
     * @param strip the character to strip from beginning and end
     * @return the stripped string or the empty string if everything is stripped.
     */
    public static String stripString(String s, char strip) {
        final int sLen = s.length();
        int newStart = 0, newEnd = sLen - 1;
        
        while (newStart < sLen && s.charAt(newStart) == strip) {
          newStart++;
        }
        while (newEnd >= 0 && s.charAt(newEnd) == strip) {
          newEnd--;
        }
        
        /*
         * newEnd contains a char we want, and substring takes end as being
         * exclusive
         */
        newEnd++;
        
        if (newStart >= sLen || newEnd < 0) {
            return "";
        }
        
        return s.substring(newStart, newEnd);
    }
    
    /**
     * Returns true if the project is an alias project.
     * <p/>
     * Alias projects require both the --url and the --label options.
     * @return boolean true if the project requested is an alias project
     */
    private boolean isAliasProject() {
        return (!isStringEmpty(mAliasData) && !isStringEmpty(mApplicationLabel));
    }

    /**
     * Extracts a "full" package & activity name from an AndroidManifest.xml. 
     * @param osManifestPath The OS path to the AndroidManifest.xml 
     * @return A full "package.ActivtyName" if this is a valid manifest,
     *         or "package." (with a dot at the end) if there's no activity,
     *         or null if there's no valid package namespace.
     */
    private String extractPackageFromManifest(String osManifestPath) {
        File f = new File(osManifestPath);
        if (!f.isFile()) {
            return null;
        }
        
        try {
            final String nsPrefix = "android";
            final String nsURI = "http://schemas.android.com/apk/res/android";
            
            XPath xpath = XPathFactory.newInstance().newXPath();
            
            xpath.setNamespaceContext(new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    if (nsPrefix.equals(prefix)) {
                        return nsURI;
                    }
                    return XMLConstants.NULL_NS_URI;
                }

                public String getPrefix(String namespaceURI) {
                    if (nsURI.equals(namespaceURI)) {
                        return nsPrefix;
                    }
                    return null;
                }

                @SuppressWarnings("unchecked")
                public Iterator getPrefixes(String namespaceURI) {
                    if (nsURI.equals(namespaceURI)) {
                        ArrayList<String> list = new ArrayList<String>();
                        list.add(nsPrefix);
                        return list.iterator();
                    }
                    return null;
                }
                
            });
            
            InputSource source = new InputSource(new FileReader(osManifestPath)); 
            String packageName = xpath.evaluate("/manifest/@package", source);

            source = new InputSource(new FileReader(osManifestPath)); 
            
            // Select the "android:name" attribute of all <activity> nodes but only if they
            // contain a sub-node <intent-filter><action> with an "android:name" attribute which
            // is 'android.intent.action.MAIN' and an <intent-filter><category> with an
            // "android:name" attribute which is 'android.intent.category.LAUNCHER'  
            String expression = String.format("/manifest/application/activity" +
                    "[intent-filter/action/@%1$s:name='android.intent.action.MAIN' and " +
                    "intent-filter/category/@%1$s:name='android.intent.category.LAUNCHER']" +
                    "/@%1$s:name", nsPrefix);

            NodeList activityNames = (NodeList) xpath.evaluate(expression, source,
                    XPathConstants.NODESET);

            // If we get here, both XPath expressions were valid so we're most likely dealing
            // with an actual AndroidManifest.xml file. The nodes may not have the requested
            // attributes though, if which case we should warn.
            
            if (packageName == null || packageName.length() == 0) {
                printHelpAndExit("ERROR: missing <manifest package=\"...\"> in '%1$s'",
                        osManifestPath);
            }

            // Get the first activity that matched earlier. If there is no activity,
            // activityName is set to an empty string and the generated "combined" name
            // will be in the form "package." (with a dot at the end).
            String activityName = "";
            if (activityNames.getLength() > 0) {
                activityName = activityNames.item(0).getNodeValue();
            }

            if (!mSilent && activityNames.getLength() > 1) {
                println("WARNING: There is more than one activity defined in '%1$s'.\n" +
                        "Only the first one will be used. If this is not appropriate, you need\n" +
                        "to specify one of these values manually instead:",
                        osManifestPath);
                
                for (int i = 0; i < activityNames.getLength(); i++) {
                    String name = activityNames.item(i).getNodeValue();
                    name = combinePackageActivityNames(packageName, name);
                    println("- %1$s", name);
                }
            }
            
            if (!mSilent && activityName.length() == 0) {
                println("WARNING: missing <activity %1$s:name=\"...\"> in '%2$s'.\n" +
                        "No activity will be generated.",
                        nsPrefix, osManifestPath);
            }

            return combinePackageActivityNames(packageName, activityName);
            
        } catch (IOException e) {
            printHelpAndExit("ERROR: failed to read '%1$s', %2$s", osManifestPath, e.getMessage());
        } catch (XPathExpressionException e) {
            Throwable t = e.getCause();
            printHelpAndExit("ERROR: failed to parse '%1$s', %2$s", osManifestPath,
                    t == null ? e.getMessage() : t.getMessage());
        }
        
        return null;
    }
    
    private String combinePackageActivityNames(String packageName,
            String activityName) {
        // Activity Name can have 3 forms:
        // - ".Name" means this is a class name in the given package name.
        //    The full FQCN is thus packageName + ".Name"
        // - "Name" is an older variant of the former. Full FQCN is packageName + "." + "Name"
        // - "com.blah.Name" is a full FQCN. Ignore packageName and use activityName as-is.
        //   To be valid, the package name should have at least two components. This is checked
        //   later during the creation of the build.xml file, so we just need to detect there's
        //   a dot but not at pos==0.
        
        int pos = activityName.indexOf('.');
        if (pos == 0) {
            return packageName + activityName;
        } else if (pos > 0) {
            return activityName;
        } else {
            return packageName + "." + activityName;
        }
    }

}
