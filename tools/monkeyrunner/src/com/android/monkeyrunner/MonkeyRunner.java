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

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.NullOutputReceiver;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.Log.ILogOutput;
import com.android.ddmlib.Log.LogLevel;

import java.awt.image.BufferedImage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 *  MonkeyRunner is a host side application to control a monkey instance on a
 *  device. MonkeyRunner provides some useful helper functions to control the
 *  device as well as various other methods to help script tests. 
 */
public class MonkeyRunner {

  static String monkeyServer = "127.0.0.1";
  static int monkeyPort = 1080;
  static Socket monkeySocket = null;

  static IDevice monkeyDevice;

  static BufferedReader monkeyReader;
  static BufferedWriter monkeyWriter;
  static String monkeyResponse;

  static MonkeyRecorder monkeyRecorder;

  static String scriptName = null;
  
  // Obtain a suitable logger.
  private static Logger logger = Logger.getLogger("com.android.monkeyrunner");

  // delay between key events
  final static int KEY_INPUT_DELAY = 1000;
  
  // version of monkey runner
  final static String monkeyRunnerVersion = "0.4";

  // TODO: interface cmd; class xml tags; fix logger; test class/script

  public static void main(String[] args) throws IOException {

    // haven't figure out how to get below INFO...bad parent.  Pass -v INFO to turn on logging 
    logger.setLevel(Level.parse("WARNING"));  
    processOptions(args);
    
    logger.info("initAdb");
    initAdbConnection();
    logger.info("openMonkeyConnection");
    openMonkeyConnection();

    logger.info("start_script");
    start_script();
    
    logger.info("ScriptRunner.run");
    ScriptRunner.run(scriptName);
   
    logger.info("end_script");
    end_script();
    logger.info("closeMonkeyConnection");
    closeMonkeyConnection();  
  }

  /**
   *  Initialize an adb session with a device connected to the host
   * 
   */
  public static void initAdbConnection() {
    String adbLocation = "adb";
    boolean device = false;
    boolean emulator = false;
    String serial = null;

    AndroidDebugBridge.init(false /* debugger support */);

    try {
      AndroidDebugBridge bridge = AndroidDebugBridge.createBridge(
          adbLocation, true /* forceNewBridge */);

      // we can't just ask for the device list right away, as the internal thread getting
      // them from ADB may not be done getting the first list.
      // Since we don't really want getDevices() to be blocking, we wait here manually.
      int count = 0;
      while (bridge.hasInitialDeviceList() == false) {
        try {
          Thread.sleep(100);
          count++;
        } catch (InterruptedException e) {
          // pass
        }

        // let's not wait > 10 sec.
        if (count > 100) {
          System.err.println("Timeout getting device list!");
          return;
        }
      }

      // now get the devices
      IDevice[] devices = bridge.getDevices();

      if (devices.length == 0) {
        printAndExit("No devices found!", true /* terminate */);
      }

      monkeyDevice = null;

      if (emulator || device) {
        for (IDevice d : devices) {
          // this test works because emulator and device can't both be true at the same
          // time.
          if (d.isEmulator() == emulator) {
            // if we already found a valid target, we print an error and return.
            if (monkeyDevice != null) {
              if (emulator) {
                printAndExit("Error: more than one emulator launched!",
                    true /* terminate */);
              } else {
                printAndExit("Error: more than one device connected!",true /* terminate */);
              }
            }
            monkeyDevice = d;
          }
        }
      } else if (serial != null) {
        for (IDevice d : devices) {
          if (serial.equals(d.getSerialNumber())) {
            monkeyDevice = d;
            break;
          }
        }
      } else {
        if (devices.length > 1) {
          printAndExit("Error: more than one emulator or device available!",
              true /* terminate */);
        }
        monkeyDevice = devices[0];
      }

      monkeyDevice.createForward(monkeyPort, monkeyPort);
      String command = "monkey --port " + monkeyPort;
      monkeyDevice.executeShellCommand(command, new NullOutputReceiver());

    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Open a tcp session over adb with the device to communicate monkey commands
   */
  public static void openMonkeyConnection() {
    try {
      InetAddress addr = InetAddress.getByName(monkeyServer);
      monkeySocket = new Socket(addr, monkeyPort);
      monkeyWriter = new BufferedWriter(new OutputStreamWriter(monkeySocket.getOutputStream()));
      monkeyReader = new BufferedReader(new InputStreamReader(monkeySocket.getInputStream()));
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
  
  /** 
   * Close tcp session with the monkey on the device
   * 
   */
  public static void closeMonkeyConnection() {
    try {
      monkeyReader.close();
      monkeyWriter.close();
      monkeySocket.close();
      AndroidDebugBridge.terminate();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  /** 
   * This is a house cleaning routine to run before starting a script. Puts
   * the device in a known state and starts recording interesting info.
   */
  public static void start_script() throws IOException {
    press("menu", false);
    press("menu", false);
    press("home", false);
    
    // Start recording the script output, might want md5 signature of file for completeness
    monkeyRecorder = new MonkeyRecorder(scriptName, monkeyRunnerVersion);

    // Record what device we are running on
    addDeviceVars();
    monkeyRecorder.addComment("Script commands");
  }

  /** 
   * This is a house cleaning routine to run after finishing a script.
   * Puts the monkey server in a known state and closes the recording.
   */
  public static void end_script() throws IOException {
    String command = "done";
    sendMonkeyEvent(command, false, false);
    
    // Stop the recording and zip up the results
    monkeyRecorder.close();
  }

  /** This is a method for scripts to launch an activity on the device
   * 
   * @param name The name of the activity to launch 
   */
  public static void launch_activity(String name) throws IOException {
    System.out.println("Launching: " + name);
    recordCommand("Launching: " + name);
    monkeyDevice.executeShellCommand("am start -a android.intent.action.MAIN -n " 
        + name, new NullOutputReceiver());
    // void return, so no response given, just close the command element in the xml file.
    monkeyRecorder.endCommand();
   }

  /**
   * Grabs the current state of the screen stores it as a png
   * 
   * @param tag filename or tag descriptor of the screenshot
   */
  public static void grabscreen(String tag) throws IOException {
    tag += ".png";

    try {
      Thread.sleep(1000);
      getDeviceImage(monkeyDevice, tag, false);
    } catch (InterruptedException e) {
    }
  }

  /**
   * Sleeper method for script to call
   * 
   * @param msec msecs to sleep for
   */
  public static void sleep(int msec) throws IOException {
    try {
      recordCommand("sleep: " + msec);
      Thread.sleep(msec);
      recordResponse("OK");
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Tap function for scripts to call at a particular x and y location
   * 
   * @param x x-coordinate
   * @param y y-coordinate
   */
  public static boolean tap(int x, int y) throws IOException {
    String command = "tap " + x + " " + y;
    boolean result = sendMonkeyEvent(command);
    return result;
  }

  /** 
   * Press function for scripts to call on a particular button or key
   * 
   * @param key key to press
   */
  public static boolean press(String key) throws IOException {
    return press(key, true);
  }

  /** 
   * Press function for scripts to call on a particular button or key
   * 
   * @param key key to press
   * @param print whether to send output to user
   */
  private static boolean press(String key, boolean print) throws IOException {
    String command = "press " + key;
    boolean result = sendMonkeyEvent(command, print, true);
    return result;
  }

  /**
   * dpad down function
   */
  public static boolean down() throws IOException {
    return press("dpad_down");
  }

  /**
   * dpad up function
   */
  public static boolean up() throws IOException {
    return press("dpad_up");
  }

  /**
   * Function to type text on the device
   * 
   * @param text text to type
   */
  public static boolean type(String text) throws IOException {
    boolean result = false;
    // text might have line ends, which signal new monkey command, so we have to eat and reissue
    String[] lines = text.split("[\\r\\n]+");
    for (String line: lines) {
      result = sendMonkeyEvent("type " + line + "\n");
    }
    // return last result.  Should never fail..?
    return result;
  }
  
  /**
   * Function to get a static variable from the device
   * 
   * @param name name of static variable to get
   */
  public static boolean getvar(String name) throws IOException {
    return sendMonkeyEvent("getvar " + name + "\n");
  }

  /**
   * Function to get the list of static variables from the device
   */
  public static boolean listvar() throws IOException {
    return sendMonkeyEvent("listvar \n");
  }

  /**
   * This function is the communication bridge between the host and the device.
   * It sends monkey events and waits for responses over the adb tcp socket.
   * This version if for all scripted events so that they get recorded and reported to user.
   * 
   * @param command the monkey command to send to the device
   */
  private static boolean sendMonkeyEvent(String command) throws IOException {
    return sendMonkeyEvent(command, true, true);
  }

  /**
   * This function allows the communication bridge between the host and the device
   * to be invisible to the script for internal needs.
   * It splits a command into monkey events and waits for responses for each over an adb tcp socket.
   * Returns on an error, else continues and sets up last response.
   * 
   * @param command the monkey command to send to the device
   * @param print whether to print out the responses to the user
   * @param record whether to put the command in the xml file that stores test outputs
   */
  private static boolean sendMonkeyEvent(String command, Boolean print, Boolean record) throws IOException {
    command = command.trim();
    if (print)
      System.out.println("MonkeyCommand: " + command);
    if (record)
      recordCommand(command);
    logger.info("Monkey Command: " + command + ".");
      
    // send a single command and get the response
    monkeyWriter.write(command + "\n");
    monkeyWriter.flush();
    monkeyResponse = monkeyReader.readLine();

    if(monkeyResponse != null) {
      // if a command returns with a response
      if (print)
        System.out.println("MonkeyServer: " + monkeyResponse);
      if (record)
        recordResponse(monkeyResponse);
      logger.info("Monkey Response: " + monkeyResponse + ".");

      // return on error
      if (monkeyResponse.startsWith("ERROR"))
        return false;

      // return on ok
      if(monkeyResponse.startsWith("OK"))
        return true;

      // return on something else?
      return false;
    }
    // didn't get a response...
    if (print)
      System.out.println("MonkeyServer: ??no response");
    if (record)
      recordResponse("??no response");
    logger.info("Monkey Response: ??no response.");

    //return on no response
    return false;
  }

  /**
   * Record the command in the xml file
   *
   * @param command the command sent to the monkey server
   */
  private static void recordCommand(String command) throws IOException {
    if (monkeyRecorder != null) {                       // don't record setup junk
      monkeyRecorder.startCommand();
      monkeyRecorder.addInput(command);
    }
  }
  
  /**
   * Record the response in the xml file
   *
   * @param response the response sent by the monkey server
   */
  private static void recordResponse(String response) throws IOException {
    recordResponse(response, "");
  } 
  
  /**
   * Record the response and the filename in the xml file, store the file (to be zipped up later)
   *
   * @param response the response sent by the monkey server
   * @param filename the filename of a file to be time stamped, recorded in the xml file and stored
   */
  private static void recordResponse(String response, String filename) throws IOException {
    if (monkeyRecorder != null) {                    // don't record setup junk
      monkeyRecorder.addResult(response, filename);  // ignores file if filename empty
      monkeyRecorder.endCommand();
    }
  }
    
  /**
   * Add the device variables to the xml file in monkeyRecorder.
   * The results get added as device_var tags in the script_run tag
   */
  private static void addDeviceVars() throws IOException {
    monkeyRecorder.addComment("Device specific variables");
    sendMonkeyEvent("listvar \n", false, false);
    if (monkeyResponse.startsWith("OK:")) {
      // peel off "OK:" string and get the individual var names  
      String[] varNames = monkeyResponse.substring(3).split("\\s+");
      // grab all the individual var values
      for (String name: varNames) {
        sendMonkeyEvent("getvar " + name, false, false);
        if(monkeyResponse != null) {
          if (monkeyResponse.startsWith("OK") ) {
            if (monkeyResponse.length() > 2) {
              monkeyRecorder.addDeviceVar(name, monkeyResponse.substring(3));
            } else { 
              // only got OK - good variable but no value
              monkeyRecorder.addDeviceVar(name, "null");
            }
          } else { 
            // error returned - couldn't get var value for name... include error return
            monkeyRecorder.addDeviceVar(name, monkeyResponse);
          }
        } else { 
          // no monkeyResponse - bad variable with no value
          monkeyRecorder.addDeviceVar(name, "null");
        }
      }
    } else {
      // it's an error, can't find variable names...
      monkeyRecorder.addAttribute("listvar", monkeyResponse);
    }
  }
  
  /**
   * Process the command-line options
   *
   * @return Returns true if options were parsed with no apparent errors.
   */
  private static void processOptions(String[] args) {
    // parse command line parameters.
    int index = 0;

    do {
      String argument = args[index++];

      if ("-s".equals(argument)) {
        if(index == args.length) {
          printUsageAndQuit("Missing Server after -s");
        }

        monkeyServer = args[index++];

      } else if ("-p".equals(argument)) {
        // quick check on the next argument.
        if (index == args.length) {
          printUsageAndQuit("Missing Server port after -p");
        }

        monkeyPort = Integer.parseInt(args[index++]);

      } else if ("-v".equals(argument)) {
        // quick check on the next argument.
        if (index == args.length) {
          printUsageAndQuit("Missing Log Level after -v");
        }

        Level level = Level.parse(args[index++]);
        logger.setLevel(level);
        level = logger.getLevel();
        System.out.println("Log level set to: " + level + "(" + level.intValue() + ").");
        System.out.println("Warning: Log levels below INFO(800) not working currently... parent issues");
        
      } else if (argument.startsWith("-")) {
        // we have an unrecognized argument.
        printUsageAndQuit("Unrecognized argument: " + argument + ".");

        monkeyPort = Integer.parseInt(args[index++]);

      } else {
        // get the filepath of the script to run.  This will be the last undashed argument.
        scriptName = argument;
      }
    } while (index < args.length);
  }

  /*
   * Grab an image from an ADB-connected device.
   */
  private static void getDeviceImage(IDevice device, String filepath, boolean landscape)
  throws IOException {
    RawImage rawImage;
    recordCommand("grabscreen");
    System.out.println("Grabbing Screeshot: " + filepath + ".");

    try {
      rawImage = device.getScreenshot();
    }
    catch (IOException ioe) {
      recordResponse("No frame buffer", "");
      printAndExit("Unable to get frame buffer: " + ioe.getMessage(), true /* terminate */);
      return;
    }

    // device/adb not available?
    if (rawImage == null) {
      recordResponse("No image", "");
      return;
    }
    
    assert rawImage.bpp == 16;

    BufferedImage image;
    
    logger.info("Raw Image - height: " + rawImage.height + ", width: " + rawImage.width);

    if (landscape) {
      // convert raw data to an Image
      image = new BufferedImage(rawImage.height, rawImage.width,
          BufferedImage.TYPE_INT_ARGB);

      byte[] buffer = rawImage.data;
      int index = 0;
      for (int y = 0 ; y < rawImage.height ; y++) {
        for (int x = 0 ; x < rawImage.width ; x++) {

          int value = buffer[index++] & 0x00FF;
          value |= (buffer[index++] << 8) & 0x0FF00;

          int r = ((value >> 11) & 0x01F) << 3;
          int g = ((value >> 5) & 0x03F) << 2;
          int b = ((value >> 0) & 0x01F) << 3;

          value = 0xFF << 24 | r << 16 | g << 8 | b;

          image.setRGB(y, rawImage.width - x - 1, value);
        }
      }
    } else {
      // convert raw data to an Image
      image = new BufferedImage(rawImage.width, rawImage.height,
          BufferedImage.TYPE_INT_ARGB);

      byte[] buffer = rawImage.data;
      int index = 0;
      for (int y = 0 ; y < rawImage.height ; y++) {
        for (int x = 0 ; x < rawImage.width ; x++) {

          int value = buffer[index++] & 0x00FF;
          value |= (buffer[index++] << 8) & 0x0FF00;

          int r = ((value >> 11) & 0x01F) << 3;
          int g = ((value >> 5) & 0x03F) << 2;
          int b = ((value >> 0) & 0x01F) << 3;

          value = 0xFF << 24 | r << 16 | g << 8 | b;

          image.setRGB(x, y, value);
        }
      }
    }

    if (!ImageIO.write(image, "png", new File(filepath))) {
      recordResponse("No png writer", "");
      throw new IOException("Failed to find png writer");
    }
    recordResponse("OK", filepath);
  }

  private static void printUsageAndQuit(String message) {
    // 80 cols marker:  01234567890123456789012345678901234567890123456789012345678901234567890123456789
    System.out.println(message);
    System.out.println("Usage: monkeyrunner [options] SCRIPT_FILE");
    System.out.println("");
    System.out.println("    -s      MonkeyServer IP Address.");
    System.out.println("    -p      MonkeyServer TCP Port.");
    System.out.println("    -v      MonkeyServer Logging level (ALL, FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE, OFF)");
    System.out.println("");
    System.out.println("");

    System.exit(1);
  }

  private static void printAndExit(String message, boolean terminate) {
    System.out.println(message);
    if (terminate) {
      AndroidDebugBridge.terminate();
    }
    System.exit(1);
  }
}
