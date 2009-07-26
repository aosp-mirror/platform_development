/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.build;

import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.android.ide.eclipse.adt.internal.project.XmlErrorHandler;
import com.android.ide.eclipse.adt.internal.project.XmlErrorHandler.XmlErrorListener;
import com.android.ide.eclipse.adt.internal.sdk.LoadStatus;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Base builder for XML files. This class allows for basic XML parsing with
 * error checking and marking the files for errors/warnings.
 */
abstract class BaseBuilder extends IncrementalProjectBuilder {

    // TODO: rename the pattern to something that makes sense + javadoc comments.

    /**
     * Single line aapt warning for skipping files.<br>
     * "  (skipping hidden file '&lt;file path&gt;'"
     */
    private final static Pattern sPattern0Line1 = Pattern.compile(
            "^\\s+\\(skipping hidden file\\s'(.*)'\\)$"); //$NON-NLS-1$

    /**
     * First line of dual line aapt error.<br>
     * "ERROR at line &lt;line&gt;: &lt;error&gt;"<br>
     * " (Occurred while parsing &lt;path&gt;)"
     */
    private final static Pattern sPattern1Line1 = Pattern.compile(
            "^ERROR\\s+at\\s+line\\s+(\\d+):\\s+(.*)$"); //$NON-NLS-1$
    /**
     * Second line of dual line aapt error.<br>
     * "ERROR at line &lt;line&gt;: &lt;error&gt;"<br>
     * " (Occurred while parsing &lt;path&gt;)"<br>
     * @see #sPattern1Line1
     */
    private final static Pattern sPattern1Line2 = Pattern.compile(
            "^\\s+\\(Occurred while parsing\\s+(.*)\\)$");  //$NON-NLS-1$
    /**
     * First line of dual line aapt error.<br>
     * "ERROR: &lt;error&gt;"<br>
     * "Defined at file &lt;path&gt; line &lt;line&gt;"
     */
    private final static Pattern sPattern2Line1 = Pattern.compile(
            "^ERROR:\\s+(.+)$"); //$NON-NLS-1$
    /**
     * Second line of dual line aapt error.<br>
     * "ERROR: &lt;error&gt;"<br>
     * "Defined at file &lt;path&gt; line &lt;line&gt;"<br>
     * @see #sPattern2Line1
     */
    private final static Pattern sPattern2Line2 = Pattern.compile(
            "Defined\\s+at\\s+file\\s+(.+)\\s+line\\s+(\\d+)"); //$NON-NLS-1$
    /**
     * Single line aapt error<br>
     * "&lt;path&gt; line &lt;line&gt;: &lt;error&gt;"
     */
    private final static Pattern sPattern3Line1 = Pattern.compile(
            "^(.+)\\sline\\s(\\d+):\\s(.+)$"); //$NON-NLS-1$
    /**
     * First line of dual line aapt error.<br>
     * "ERROR parsing XML file &lt;path&gt;"<br>
     * "&lt;error&gt; at line &lt;line&gt;"
     */
    private final static Pattern sPattern4Line1 = Pattern.compile(
            "^Error\\s+parsing\\s+XML\\s+file\\s(.+)$"); //$NON-NLS-1$
    /**
     * Second line of dual line aapt error.<br>
     * "ERROR parsing XML file &lt;path&gt;"<br>
     * "&lt;error&gt; at line &lt;line&gt;"<br>
     * @see #sPattern4Line1
     */
    private final static Pattern sPattern4Line2 = Pattern.compile(
            "^(.+)\\s+at\\s+line\\s+(\\d+)$"); //$NON-NLS-1$

    /**
     * Single line aapt warning<br>
     * "&lt;path&gt;:&lt;line&gt;: &lt;error&gt;"
     */
    private final static Pattern sPattern5Line1 = Pattern.compile(
            "^(.+?):(\\d+):\\s+WARNING:(.+)$"); //$NON-NLS-1$

    /**
     * Single line aapt error<br>
     * "&lt;path&gt;:&lt;line&gt;: &lt;error&gt;"
     */
    private final static Pattern sPattern6Line1 = Pattern.compile(
            "^(.+?):(\\d+):\\s+(.+)$"); //$NON-NLS-1$

    /**
     * 4 line aapt error<br>
     * "ERROR: 9-path image &lt;path&gt; malformed"<br>
     * Line 2 and 3 are taken as-is while line 4 is ignored (it repeats with<br>
     * 'ERROR: failure processing &lt;path&gt;)
     */
    private final static Pattern sPattern7Line1 = Pattern.compile(
            "^ERROR:\\s+9-patch\\s+image\\s+(.+)\\s+malformed\\.$"); //$NON-NLS-1$

    private final static Pattern sPattern8Line1 = Pattern.compile(
            "^(invalid resource directory name): (.*)$"); //$NON-NLS-1$

    /**
     * 2 line aapt error<br>
     * "ERROR: Invalid configuration: foo"<br>
     * "                              ^^^"<br>
     * There's no need to parse the 2nd line.
     */
    private final static Pattern sPattern9Line1 = Pattern.compile(
            "^Invalid configuration: (.+)$"); //$NON-NLS-1$

    /** SAX Parser factory. */
    private SAXParserFactory mParserFactory;

    /**
     * Base Resource Delta Visitor to handle XML error
     */
    protected static class BaseDeltaVisitor implements XmlErrorListener {

        /** The Xml builder used to validate XML correctness. */
        protected BaseBuilder mBuilder;

        /**
         * XML error flag. if true, we keep parsing the ResourceDelta but the
         * compilation will not happen (we're putting markers)
         */
        public boolean mXmlError = false;

        public BaseDeltaVisitor(BaseBuilder builder) {
            mBuilder = builder;
        }

        /**
         * Finds a matching Source folder for the current path. This checkds if the current path
         * leads to, or is a source folder.
         * @param sourceFolders The list of source folders
         * @param pathSegments The segments of the current path
         * @return The segments of the source folder, or null if no match was found
         */
        protected static String[] findMatchingSourceFolder(ArrayList<IPath> sourceFolders,
                String[] pathSegments) {

            for (IPath p : sourceFolders) {
                // check if we are inside one of those source class path

                // get the segments
                String[] srcSegments = p.segments();

                // compare segments. We want the path of the resource
                // we're visiting to be
                boolean valid = true;
                int segmentCount = pathSegments.length;

                for (int i = 0 ; i < segmentCount; i++) {
                    String s1 = pathSegments[i];
                    String s2 = srcSegments[i];

                    if (s1.equalsIgnoreCase(s2) == false) {
                        valid = false;
                        break;
                    }
                }

                if (valid) {
                    // this folder, or one of this children is a source
                    // folder!
                    // we return its segments
                    return srcSegments;
                }
            }

            return null;
        }

        /**
         * Sent when an XML error is detected.
         * @see XmlErrorListener
         */
        public void errorFound() {
            mXmlError = true;
        }
    }

    public BaseBuilder() {
        super();
        mParserFactory = SAXParserFactory.newInstance();

        // FIXME when the compiled XML support for namespace is in, set this to true.
        mParserFactory.setNamespaceAware(false);
    }

    /**
     * Checks an Xml file for validity. Errors/warnings will be marked on the
     * file
     * @param resource the resource to check
     * @param visitor a valid resource delta visitor
     */
    protected final void checkXML(IResource resource, BaseDeltaVisitor visitor) {

        // first make sure this is an xml file
        if (resource instanceof IFile) {
            IFile file = (IFile)resource;

            // remove previous markers
            removeMarkersFromFile(file, AndroidConstants.MARKER_XML);

            // create  the error handler
            XmlErrorHandler reporter = new XmlErrorHandler(file, visitor);
            try {
                // parse
                getParser().parse(file.getContents(), reporter);
            } catch (Exception e1) {
            }
        }
    }

    /**
     * Returns the SAXParserFactory, instantiating it first if it's not already
     * created.
     * @return the SAXParserFactory object
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    protected final SAXParser getParser() throws ParserConfigurationException,
            SAXException {
        return mParserFactory.newSAXParser();
    }

    /**
     * Adds a marker to the current project.
     *
     * @param markerId The id of the marker to add.
     * @param message the message associated with the mark
     * @param severity the severity of the marker.
     */
    protected final void markProject(String markerId, String message, int severity) {
        BaseProjectHelper.addMarker(getProject(), markerId, message, severity);
    }


    /**
     * Removes markers from a file.
     * @param file The file from which to delete the markers.
     * @param markerId The id of the markers to remove. If null, all marker of
     * type <code>IMarker.PROBLEM</code> will be removed.
     */
    protected final void removeMarkersFromFile(IFile file, String markerId) {
        try {
            if (file.exists()) {
                file.deleteMarkers(markerId, true, IResource.DEPTH_ZERO);
            }
        } catch (CoreException ce) {
            String msg = String.format(Messages.Marker_Delete_Error, markerId, file.toString());
            AdtPlugin.printErrorToConsole(getProject(), msg);
        }
    }

    /**
     * Removes markers from a container and its children.
     * @param folder The container from which to delete the markers.
     * @param markerId The id of the markers to remove. If null, all marker of
     * type <code>IMarker.PROBLEM</code> will be removed.
     */
    protected final void removeMarkersFromContainer(IContainer folder, String markerId) {
        try {
            if (folder.exists()) {
                folder.deleteMarkers(markerId, true, IResource.DEPTH_INFINITE);
            }
        } catch (CoreException ce) {
            String msg = String.format(Messages.Marker_Delete_Error, markerId, folder.toString());
            AdtPlugin.printErrorToConsole(getProject(), msg);
        }
    }

    /**
     * Removes markers from a project and its children.
     * @param project The project from which to delete the markers
     * @param markerId The id of the markers to remove. If null, all marker of
     * type <code>IMarker.PROBLEM</code> will be removed.
     */
    protected final static void removeMarkersFromProject(IProject project,
            String markerId) {
        try {
            if (project.exists()) {
                project.deleteMarkers(markerId, true, IResource.DEPTH_INFINITE);
            }
        } catch (CoreException ce) {
            String msg = String.format(Messages.Marker_Delete_Error, markerId, project.getName());
            AdtPlugin.printErrorToConsole(project, msg);
        }
    }

    /**
     * Get the stderr output of a process and return when the process is done.
     * @param process The process to get the ouput from
     * @param results The array to store the stderr output
     * @return the process return code.
     * @throws InterruptedException
     */
    protected final int grabProcessOutput(final Process process,
            final ArrayList<String> results)
            throws InterruptedException {
        // Due to the limited buffer size on windows for the standard io (stderr, stdout), we
        // *need* to read both stdout and stderr all the time. If we don't and a process output
        // a large amount, this could deadlock the process.

        // read the lines as they come. if null is returned, it's
        // because the process finished
        new Thread("") { //$NON-NLS-1$
            @Override
            public void run() {
                // create a buffer to read the stderr output
                InputStreamReader is = new InputStreamReader(process.getErrorStream());
                BufferedReader errReader = new BufferedReader(is);

                try {
                    while (true) {
                        String line = errReader.readLine();
                        if (line != null) {
                            results.add(line);
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }.start();

        new Thread("") { //$NON-NLS-1$
            @Override
            public void run() {
                InputStreamReader is = new InputStreamReader(process.getInputStream());
                BufferedReader outReader = new BufferedReader(is);

                IProject project = getProject();

                try {
                    while (true) {
                        String line = outReader.readLine();
                        if (line != null) {
                            AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE,
                                    project, line);
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    // do nothing.
                }
            }

        }.start();

        // get the return code from the process
        return process.waitFor();
    }

    /**
     * Parse the output of aapt and mark the incorrect file with error markers
     *
     * @param results the output of aapt
     * @param project the project containing the file to mark
     * @return true if the parsing failed, false if success.
     */
    protected final boolean parseAaptOutput(ArrayList<String> results,
            IProject project) {
        // nothing to parse? just return false;
        if (results.size() == 0) {
            return false;
        }

        // get the root of the project so that we can make IFile from full
        // file path
        String osRoot = project.getLocation().toOSString();

        Matcher m;

        for (int i = 0; i < results.size(); i++) {
            String p = results.get(i);

            m = sPattern0Line1.matcher(p);
            if (m.matches()) {
                // we ignore those (as this is an ignore message from aapt)
                continue;
            }

            m = sPattern1Line1.matcher(p);
            if (m.matches()) {
                String lineStr = m.group(1);
                String msg = m.group(2);

                // get the matcher for the next line.
                m = getNextLineMatcher(results, ++i, sPattern1Line2);
                if (m == null) {
                    return true;
                }

                String location = m.group(1);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AndroidConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }
                continue;
            }

            // this needs to be tested before Pattern2 since they both start with 'ERROR:'
            m = sPattern7Line1.matcher(p);
            if (m.matches()) {
                String location = m.group(1);
                String msg = p; // default msg is the line in case we don't find anything else

                if (++i < results.size()) {
                    msg = results.get(i).trim();
                    if (++i < results.size()) {
                        msg = msg + " - " + results.get(i).trim(); //$NON-NLS-1$

                        // skip the next line
                        i++;
                    }
                }

                // display the error
                if (checkAndMark(location, null, msg, osRoot, project,
                        AndroidConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m =  sPattern2Line1.matcher(p);
            if (m.matches()) {
                // get the msg
                String msg = m.group(1);

                // get the matcher for the next line.
                m = getNextLineMatcher(results, ++i, sPattern2Line2);
                if (m == null) {
                    return true;
                }

                String location = m.group(1);
                String lineStr = m.group(2);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AndroidConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }
                continue;
            }

            m = sPattern3Line1.matcher(p);
            if (m.matches()) {
                String location = m.group(1);
                String lineStr = m.group(2);
                String msg = m.group(3);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AndroidConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sPattern4Line1.matcher(p);
            if (m.matches()) {
                // get the filename.
                String location = m.group(1);

                // get the matcher for the next line.
                m = getNextLineMatcher(results, ++i, sPattern4Line2);
                if (m == null) {
                    return true;
                }

                String msg = m.group(1);
                String lineStr = m.group(2);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AndroidConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sPattern5Line1.matcher(p);
            if (m.matches()) {
                String location = m.group(1);
                String lineStr = m.group(2);
                String msg = m.group(3);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AndroidConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_WARNING) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sPattern6Line1.matcher(p);
            if (m.matches()) {
                String location = m.group(1);
                String lineStr = m.group(2);
                String msg = m.group(3);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, lineStr, msg, osRoot, project,
                        AndroidConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sPattern8Line1.matcher(p);
            if (m.matches()) {
                String location = m.group(2);
                String msg = m.group(1);

                // check the values and attempt to mark the file.
                if (checkAndMark(location, null, msg, osRoot, project,
                        AndroidConstants.MARKER_AAPT_COMPILE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            m = sPattern9Line1.matcher(p);
            if (m.matches()) {
                String badConfig = m.group(1);
                String msg = String.format("APK Configuration filter '%1$s' is invalid", badConfig);

                // skip the next line
                i++;

                // check the values and attempt to mark the file.
                if (checkAndMark(null /*location*/, null, msg, osRoot, project,
                        AndroidConstants.MARKER_AAPT_PACKAGE, IMarker.SEVERITY_ERROR) == false) {
                    return true;
                }

                // success, go to the next line
                continue;
            }

            // invalid line format, flag as error, and bail
            return true;
        }

        return false;
    }



    /**
     * Saves a String property into the persistent storage of the project.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @param value the value to save
     * @return true if the save succeeded.
     */
    protected boolean saveProjectStringProperty(String propertyName, String value) {
        IProject project = getProject();
        return ProjectHelper.saveStringProperty(project, propertyName, value);
    }


    /**
     * Loads a String property from the persistent storage of the project.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @return the property value or null if it was not found.
     */
    protected String loadProjectStringProperty(String propertyName) {
        IProject project = getProject();
        return ProjectHelper.loadStringProperty(project, propertyName);
    }

    /**
     * Saves a property into the persistent storage of the project.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @param value the value to save
     * @return true if the save succeeded.
     */
    protected boolean saveProjectBooleanProperty(String propertyName, boolean value) {
        IProject project = getProject();
        return ProjectHelper.saveStringProperty(project, propertyName, Boolean.toString(value));
    }

    /**
     * Loads a boolean property from the persistent storage of the project.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @param defaultValue The default value to return if the property was not found.
     * @return the property value or the default value if the property was not found.
     */
    protected boolean loadProjectBooleanProperty(String propertyName, boolean defaultValue) {
        IProject project = getProject();
        return ProjectHelper.loadBooleanProperty(project, propertyName, defaultValue);
    }

    /**
     * Saves the path of a resource into the persistent storate of the project.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @param resource the resource which path is saved.
     * @return true if the save succeeded
     */
    protected boolean saveProjectResourceProperty(String propertyName, IResource resource) {
        return ProjectHelper.saveResourceProperty(getProject(), propertyName, resource);
    }

    /**
     * Loads the path of a resource from the persistent storage of the project, and returns the
     * corresponding IResource object.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @return The corresponding IResource object (or children interface) or null
     */
    protected IResource loadProjectResourceProperty(String propertyName) {
        IProject project = getProject();
        return ProjectHelper.loadResourceProperty(project, propertyName);
    }

    /**
     * Check if the parameters gotten from the error output are valid, and mark
     * the file with an AAPT marker.
     * @param location the full OS path of the error file. If null, the project is marked
     * @param lineStr
     * @param message
     * @param root The root directory of the project, in OS specific format.
     * @param project
     * @param markerId The marker id to put.
     * @param severity The severity of the marker to put (IMarker.SEVERITY_*)
     * @return true if the parameters were valid and the file was marked successfully.
     *
     * @see IMarker
     */
    private final  boolean checkAndMark(String location, String lineStr,
            String message, String root, IProject project, String markerId, int severity) {
        // check this is in fact a file
        if (location != null) {
            File f = new File(location);
            if (f.exists() == false) {
                return false;
            }
        }

        // get the line number
        int line = -1; // default value for error with no line.

        if (lineStr != null) {
            try {
                line = Integer.parseInt(lineStr);
            } catch (NumberFormatException e) {
                // looks like the string we extracted wasn't a valid
                // file number. Parsing failed and we return true
                return false;
            }
        }

        // add the marker
        IResource f2 = project;
        if (location != null) {
            f2 = getResourceFromFullPath(location, root, project);
            if (f2 == null) {
                return false;
            }
        }

        // check if there's a similar marker already, since aapt is launched twice
        boolean markerAlreadyExists = false;
        try {
            IMarker[] markers = f2.findMarkers(markerId, true, IResource.DEPTH_ZERO);

            for (IMarker marker : markers) {
                int tmpLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                if (tmpLine != line) {
                    break;
                }

                int tmpSeverity = marker.getAttribute(IMarker.SEVERITY, -1);
                if (tmpSeverity != severity) {
                    break;
                }

                String tmpMsg = marker.getAttribute(IMarker.MESSAGE, null);
                if (tmpMsg == null || tmpMsg.equals(message) == false) {
                    break;
                }

                // if we're here, all the marker attributes are equals, we found it
                // and exit
                markerAlreadyExists = true;
                break;
            }

        } catch (CoreException e) {
            // if we couldn't get the markers, then we just mark the file again
            // (since markerAlreadyExists is initialized to false, we do nothing)
        }

        if (markerAlreadyExists == false) {
            if (line != -1) {
                BaseProjectHelper.addMarker(f2, markerId, message, line,
                        severity);
            } else {
                BaseProjectHelper.addMarker(f2, markerId, message, severity);
            }
        }

        return true;
    }

    /**
     * Returns a matching matcher for the next line
     * @param lines The array of lines
     * @param nextIndex The index of the next line
     * @param pattern The pattern to match
     * @return null if error or no match, the matcher otherwise.
     */
    private final Matcher getNextLineMatcher(ArrayList<String> lines,
            int nextIndex, Pattern pattern) {
        // unless we can't, because we reached the last line
        if (nextIndex == lines.size()) {
            // we expected a 2nd line, so we flag as error
            // and we bail
            return null;
        }

        Matcher m = pattern.matcher(lines.get(nextIndex));
        if (m.matches()) {
           return m;
        }

        return null;
    }

    private IResource getResourceFromFullPath(String filename, String root,
            IProject project) {
        if (filename.startsWith(root)) {
            String file = filename.substring(root.length());

            // get the resource
            IResource r = project.findMember(file);

            // if the resource is valid, we add the marker
            if (r.exists()) {
                return r;
            }
        }

        return null;
    }

    /**
     * Returns an array of external jar files used by the project.
     * @return an array of OS-specific absolute file paths
     */
    protected final String[] getExternalJars() {
        // get the current project
        IProject project = getProject();

        // get a java project from it
        IJavaProject javaProject = JavaCore.create(project);

        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();

        ArrayList<String> oslibraryList = new ArrayList<String>();
        IClasspathEntry[] classpaths = javaProject.readRawClasspath();
        if (classpaths != null) {
            for (IClasspathEntry e : classpaths) {
                if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY ||
                        e.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
                    // if this is a classpath variable reference, we resolve it.
                    if (e.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
                        e = JavaCore.getResolvedClasspathEntry(e);
                    }

                    // get the IPath
                    IPath path = e.getPath();

                    // check the name ends with .jar
                    if (AndroidConstants.EXT_JAR.equalsIgnoreCase(path.getFileExtension())) {
                        boolean local = false;
                        IResource resource = wsRoot.findMember(path);
                        if (resource != null && resource.exists() &&
                                resource.getType() == IResource.FILE) {
                            local = true;
                            oslibraryList.add(resource.getLocation().toOSString());
                        }

                        if (local == false) {
                            // if the jar path doesn't match a workspace resource,
                            // then we get an OSString and check if this links to a valid file.
                            String osFullPath = path.toOSString();

                            File f = new File(osFullPath);
                            if (f.exists()) {
                                oslibraryList.add(osFullPath);
                            } else {
                                String message = String.format( Messages.Couldnt_Locate_s_Error,
                                        path);
                                AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE,
                                        project, message);

                                // Also put a warning marker on the project
                                markProject(AdtConstants.MARKER_ADT, message,
                                        IMarker.SEVERITY_WARNING);
                            }
                        }
                    }
                }
            }
        }

        return oslibraryList.toArray(new String[oslibraryList.size()]);
    }

    /**
     * Aborts the build if the SDK/project setups are broken. This does not
     * display any errors.
     *
     * @param project The {@link IJavaProject} being compiled.
     * @throws CoreException
     */
    protected void abortOnBadSetup(IProject project) throws CoreException {
        // check if we have finished loading the SDK.
        if (AdtPlugin.getDefault().getSdkLoadStatus() != LoadStatus.LOADED) {
            // we exit silently
            stopBuild("SDK is not loaded yet");
        }

        // abort if there are TARGET or ADT type markers
        IMarker[] markers = project.findMarkers(AdtConstants.MARKER_TARGET,
                false /*includeSubtypes*/, IResource.DEPTH_ZERO);

        if (markers.length > 0) {
            stopBuild("");
        }

        markers = project.findMarkers(AdtConstants.MARKER_ADT, false /*includeSubtypes*/,
                IResource.DEPTH_ZERO);

        if (markers.length > 0) {
            stopBuild("");
        }
    }

    /**
     * Throws an exception to cancel the build.
     *
     * @param error the error message
     * @param args the printf-style arguments to the error message.
     * @throws CoreException
     */
    protected final void stopBuild(String error, Object... args) throws CoreException {
        throw new CoreException(new Status(IStatus.CANCEL, AdtPlugin.PLUGIN_ID,
                String.format(error, args)));
    }

    /**
     * Recursively delete all the derived resources.
     */
    protected void removeDerivedResources(IResource resource, IProgressMonitor monitor)
            throws CoreException {
        if (resource.exists()) {
            if (resource.isDerived()) {
                resource.delete(true, new SubProgressMonitor(monitor, 10));
            } else if (resource.getType() == IResource.FOLDER) {
                IFolder folder = (IFolder)resource;
                IResource[] members = folder.members();
                for (IResource member : members) {
                    removeDerivedResources(member, monitor);
                }
            }
        }
    }
}
