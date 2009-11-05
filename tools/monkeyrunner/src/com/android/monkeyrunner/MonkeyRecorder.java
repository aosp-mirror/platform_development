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

package com.android.monkeyrunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jheer.XMLWriter;

/**
 *  MonkeyRecorder is a host side class that records the output of scripts that are run. 
 *  It creates a unique directory, puts in an xml file that records each cmd and result.
 *  It stores every screenshot in this directory.
 *  When finished, it zips this all up.
 *
 *  Calling Sequence:
 *    mr = new MonkeyRecorder(scriptName);
 *    mr.startCommand();
 *    [mr.addAttribute(name, value);]
 *    ...
 *    [mr.addInput(cmd);]
 *    [mr.addResults(result, filename);]   // filename = "" if no screenshot
 *    mr.endCommand();
 *    mr.addComment(comment);
 *    mr.startCommand();
 *    ...
 *    mr.endCommand();
 *    ...
 *    mr.close();
 *
 *  With MonkeyRunner this should output an xml file, <script_name>-yyyyMMdd-HH:mm:ss.xml, into the
 *  directory out/<script_name>-yyyyMMdd-HH:mm:ss with the contents like:
 *
 *  <?xml version="1.0" encoding='UTF-8'?>
 *  <!-- Monkey Script Results -->
 *  <script_run script_name="filename" monkeyRunnerVersion="0.2">
 *    <!-- Device specific variables -->
 *    <device_var var_name="name" var_value="value" />
 *    <device_var name="build.display" value="opal-userdebug 1.6 DRC79 14207 test-keys"/>
 *    ...
 *    <!-- Script commands -->
 *    <command>
 *      dateTime="20090921-17:08:43"
 *      <input cmd="Pressing: menu"/>
 *      <response result="OK" dateTime="20090921-17:08:43"/>
 *    </command>
 *    ...
 *    <command>
 *      dateTime="20090921-17:09:44"
 *      <input cmd="grabscreen"/>
 *      <response result="OK" dateTime="20090921-17:09:45" screenshot="home_screen-20090921-17:09:45.png"/>
 *    </command>
 *    ...
 *  </script_run>
 *  
 *  And then zip it up with all the screenshots in the file: <script_name>-yyyyMMdd-HH:mm:ss.zip.
 */
 
public class MonkeyRecorder {

  // xml file to store output results in
  private static String mXmlFilename;
  private static FileWriter mXmlFile;
  private static XMLWriter mXmlWriter;
  
  // unique subdirectory to put results in (screenshots and xml file)
  private static String mDirname;
  private static List<String> mScreenShotNames = new ArrayList<String>();
  
  // where we store all the results for all the script runs
  private static final String ROOT_DIR = "out";
  
  // for getting the date and time in now()
  private static final SimpleDateFormat SIMPLE_DATE_TIME_FORMAT =
      new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
  
  /**
   * Create a new MonkeyRecorder that records commands and zips up screenshots for submittal
   * 
   * @param scriptName filepath of the monkey script we are running
   */
  public MonkeyRecorder(String scriptName, String version) throws IOException {
    // Create directory structure to store xml file, images and zips
    File scriptFile = new File(scriptName);
    scriptName = scriptFile.getName();  // Get rid of path
    mDirname = ROOT_DIR + "/" + stripType(scriptName) + "-" + now();
    new File(mDirname).mkdirs();
    
    // Initialize xml file
    mXmlFilename = stampFilename(stripType(scriptName) + ".xml");
    initXmlFile(scriptName, version);
  }

  // Get the current date and time in a simple string format (used for timestamping filenames)
  private static String now() {
    return SIMPLE_DATE_TIME_FORMAT.format(Calendar.getInstance().getTime());     
  }
  
  /**
   * Initialize the xml file writer
   * 
   * @param scriptName filename (not path) of the monkey script, stored as attribute in the xml file
   * @param version of the monkey runner test system
   */
  private static void initXmlFile(String scriptName, String version) throws IOException {
    String[] names = new String[] { "script_name", "monkeyRunnerVersion" };
    String[] values = new String[] { scriptName, version };
    mXmlFile = new FileWriter(mDirname + "/" + mXmlFilename);
    mXmlWriter = new XMLWriter(mXmlFile);
    mXmlWriter.begin();
    mXmlWriter.comment("Monkey Script Results");
    mXmlWriter.start("script_run", names, values, names.length);
  }
  
  /**
   * Add a comment to the xml file.
   * 
   * @param comment comment to add to the xml file
   */
  public static void addComment(String comment) throws IOException {
    mXmlWriter.comment(comment);
  }
    
  /**
   * Begin writing a command xml element
   */
  public static void startCommand() throws IOException {
    mXmlWriter.start("command", "dateTime", now());
  }
  
  /**
   * Write a command name attribute in a command xml element.  
   * It's add as a sinlge script command could be multiple monkey commands.
   * 
   * @param cmd command sent to the monkey
   */
  public static void addInput(String cmd)  throws IOException {
    String name = "cmd";
    String value = cmd;
    mXmlWriter.tag("input", name, value);
  }
  
  /**
   * Write a response xml element in a command.  
   * Attributes include the monkey result, datetime, and possibly screenshot filename
   * 
   * @param result response of the monkey to the command
   * @param filename filename of the screen shot (or other file to be included)
   */
  public static void addResult(String result, String filename) throws IOException {
    int num_args = 2;
    String[] names = new String[3];
    String[] values = new String[3];
    names[0] = "result";
    values[0] = result;
    names[1] = "dateTime";
    values[1] = now();
    if (filename.length() != 0) {
      names[2] = "screenshot";
      values[2] = stampFilename(filename); 
      addScreenShot(filename);
      num_args = 3;
    }
    mXmlWriter.tag("response", names, values, num_args); 
  }
  
  /**
   * Add an attribut to an open xml element. name="escaped_value"
   * 
   * @param name name of the attribute
   * @param value value of the attribute
   */
  public static void addAttribute(String name, String value) throws IOException {
    mXmlWriter.addAttribute(name, value);
  }

   /**
   * Add an xml device variable element. name="escaped_value"
   * 
   * @param name name of the variable
   * @param value value of the variable
   */
  public static void addDeviceVar(String name, String value) throws IOException {
    String[] names = {"name", "value"};
    String[] values = {name, value};
    mXmlWriter.tag("device_var", names, values, names.length);
  }
 
  /**
   * Move the screenshot to storage and remember you did it so it can be zipped up later.
   * 
   * @param filename file name of the screenshot to be stored (Not path name)
   */
  private static void addScreenShot(String filename) {
    File file = new File(filename);
    String screenShotName = stampFilename(filename);
    file.renameTo(new File(mDirname, screenShotName));
    mScreenShotNames.add(screenShotName);
  }

  /**
   * Finish writing a command xml element
   */
  public static void endCommand() throws IOException {
    mXmlWriter.end();
  }
  
  /**
   * Add datetime in front of filetype (the stuff after and including the last infamous '.')
   *
   * @param filename path of file to be stamped
   */
  private static String stampFilename(String filename) {
    // 
    int typeIndex = filename.lastIndexOf('.');
    if (typeIndex == -1) {
      return filename + "-" + now();
    }  
    return filename.substring(0, typeIndex) + "-" + now() + filename.substring(typeIndex);
  }
  
  /**
   * Strip out the file type (the stuff after and including the last infamous '.')
   *
   * @param filename path of file to be stripped of type information
   */
   private static String stripType(String filename) {
    // 
    int typeIndex = filename.lastIndexOf('.');
    if (typeIndex == -1)
      return filename;
    return filename.substring(0, typeIndex);
  }

  /**
   * Close the monkeyRecorder by closing the xml file and zipping it up with the screenshots.
   *
   * @param filename path of file to be stripped of type information
   */ 
  public static void close() throws IOException {
    // zip up xml file and screenshots into ROOT_DIR.
    byte[] buf = new byte[1024];
    String zipFileName = mXmlFilename + ".zip";
    endCommand();
    mXmlFile.close();
    FileOutputStream zipFile = new FileOutputStream(ROOT_DIR + "/" + zipFileName);
    ZipOutputStream out = new ZipOutputStream(zipFile);

    // add the xml file
    addFileToZip(out, mDirname + "/" + mXmlFilename, buf);
    
    // Add the screenshots
    for (String filename : mScreenShotNames) {
      addFileToZip(out, mDirname + "/" + filename, buf);
    }
    out.close();
  }
  
  /**
   * Helper function to zip up a file into an open zip archive.
   *
   * @param zip the stream of the zip archive
   * @param filepath the filepath of the file to be added to the zip archive
   * @param buf storage place to stage reads of file before zipping
   */ 
  private static void addFileToZip(ZipOutputStream zip, String filepath, byte[] buf) throws IOException {
    FileInputStream in = new FileInputStream(filepath);
    zip.putNextEntry(new ZipEntry(filepath));
    int len;
    while ((len = in.read(buf)) > 0) {
      zip.write(buf, 0, len);
    }
    zip.closeEntry();
    in.close();
  }
}
