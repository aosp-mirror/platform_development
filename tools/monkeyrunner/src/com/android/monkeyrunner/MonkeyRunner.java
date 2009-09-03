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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;

/**
 *  MonkeyRunner is a host side application to control a monkey instance on a
 * device. MonkeyRunner provides some useful helper functions to control the
 * device as well as various other methods to help script tests. 
 */
public class MonkeyRunner {

  static String monkeyServer = "127.0.0.1";
  static int monkeyPort = 1080;
  static Socket monkeySocket = null;

  static IDevice monkeyDevice;
  
  static BufferedReader monkeyReader;
  static BufferedWriter monkeyWriter;

  static String scriptName = null;

  // delay between key events
  final static int KEY_INPUT_DELAY = 1000;

  public static void main(String[] args) {

    processOptions(args);
    
    initAdbConnection();
    openMonkeyConnection();

    start_script();
    
    ScriptRunner.run(scriptName);
   
    end_script();
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
   * This is a house cleaning type routine to run before starting a script. Puts
   * the device in a known state.
   */
  public static void start_script() {
    press("menu");
    press("menu");
    press("home");
  }

  public static void end_script() {
    String command = "END";
    sendMonkeyEvent(command);
  }

  /** This is a method for scripts to launch an activity on the device
   * 
   * @param name The name of the activity to launch 
   */
  public static void launch_activity(String name) {
    try {
      System.out.println("Launching: " + name);
      monkeyDevice.executeShellCommand("am start -a android.intent.action.MAIN -n " 
          + name, new NullOutputReceiver());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Grabs the current state of the screen stores it as a png
   * 
   * @param tag filename or tag descriptor of the screenshot
   */
  public static void grabscreen(String tag) {
    tag += ".png";

    try {
      Thread.sleep(1000);
      getDeviceImage(monkeyDevice, tag, false);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
    }
  }

  /**
   * Sleeper method for script to call
   * 
   * @param msec msecs to sleep for
   */
  public static void sleep(int msec) {
    try {
      Thread.sleep(msec);
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
  public static boolean tap(int x, int y) {
    String command = "touch down " + x + "  " + y + "\r\n" +
    "touch up " + x + " " + y + "\r\n";

    System.out.println("Tapping: " + x + ", " + y);
    return sendMonkeyEvent(command);
  }

  /** 
   * Press function for scripts to call on a particular button or key
   * 
   * @param key key to press
   */
  public static boolean press(String key) {
    String command = "key down " + key + "\r\n" +
    "key up " + key + "\r\n";

    System.out.println("Pressing: " + key);
    return sendMonkeyEvent(command);
  }

  /**
   * dpad down function
   */
  public static boolean down() {
    return press("dpad_down");
  }

  /**
   * dpad up function
   */
  public static boolean up() {
    return press("dpad_up");
  }

  /**
   * Function to type text on the device
   * 
   * @param text text to type
   */
  public static boolean type(String text) {
    System.out.println("Typing: " + text);
    for (int i=0; i<text.length(); i++) {
      String command = "key down ";
      char c = text.charAt(i);
      if(Character.isDigit(c)) {
        command += "KEYCODE_" + c + "\n" + "key up KEYCODE_" + c + "\n";
      } else {
        command = "key down " + c + "\n" + "key up " + c + "\n";
      }

      if(!sendMonkeyEvent(command)) {
        System.out.println("\nERROR: Key not set \n");
      }

      // lets delay a bit after each input to ensure accuracy
      try {
        Thread.sleep(KEY_INPUT_DELAY);
      } catch (InterruptedException e) {
      }
    }

    return true;
  }

  /**
   * This function is the communication bridge between the host and the device.
   * It sends monkey events and waits for responses over the adb tcp socket.
   * 
   * @param command the monkey command to send to the device
   */
  private static boolean sendMonkeyEvent(String command) {
    try {
      monkeyWriter = new BufferedWriter(
          new OutputStreamWriter(monkeySocket.getOutputStream()));
      monkeyWriter.write(command);
      monkeyWriter.flush();

      monkeyReader = new BufferedReader(
          new InputStreamReader(monkeySocket.getInputStream()));
      String response = monkeyReader.readLine();

      sleep(1000);
      System.out.println("MonkeyServer: " + response);
      if(response.equals("OK"))
        return true;
      if(response.equals("ERROR"))
        return false;

    } catch (IOException e) {
      e.printStackTrace();
    }

    return false;
  }


  /**
   * Process the command-line options
   *
   * @return Returns true if options were parsed with no apparent errors.
   */
  public static void processOptions(String[] args) {
    // parse command line parameters.
    int index = 0;

    do {
      String argument = args[index++];

      if ("-s".equals(argument)) {
        if(index == args.length) {
          printAndExit("Missing Server after -s", false);
        }

        monkeyServer = args[index++];

      } else if ("-p".equals(argument)) {
        // quick check on the next argument.
        if (index == args.length) {
          printAndExit("Missing Server IP after -p", false /* terminate */);
        }

        monkeyPort = Integer.parseInt(args[index++]);
      } else {
        // get the filepath of the script to run.
        scriptName = argument;

        // should not be any other device.
        //if (index < args.length) {
        //    printAndExit("Too many arguments!", false /* terminate */);
        //}
      }
    } while (index < args.length);
  }

  /*
   * Grab an image from an ADB-connected device.
   */
  private static void getDeviceImage(IDevice device, String filepath, boolean landscape)
  throws IOException {
    RawImage rawImage;

    try {
      rawImage = device.getScreenshot();
    }
    catch (IOException ioe) {
      printAndExit("Unable to get frame buffer: " + ioe.getMessage(), true /* terminate */);
      return;
    }

    // device/adb not available?
    if (rawImage == null)
      return;

    assert rawImage.bpp == 16;

    BufferedImage image;

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
      throw new IOException("Failed to find png writer");
    }
  }

  private static void printUsageAndQuit() {
    // 80 cols marker:  01234567890123456789012345678901234567890123456789012345678901234567890123456789
    System.out.println("Usage: monkeyrunner [options] SCRIPT_FILE");
    System.out.println("");
    System.out.println("    -s      MonkeyServer IP Address.");
    System.out.println("    -p      MonkeyServer TCP Port.");
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
