package com.android.monkeyrunner;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.python.util.InteractiveConsole;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.lang.RuntimeException;
import java.util.Properties;


/**
 * Runs Jython based scripts.
 */
public class ScriptRunner {

  /** The "this" scope object for scripts. */
  private final Object scope;
  private final String variable;
  
  /** Private constructor. */
  private ScriptRunner(Object scope, String variable) {
    this.scope = scope;
    this.variable = variable;
  }
  
  /** Creates a new instance for the given scope object. */
  public static ScriptRunner newInstance(Object scope, String variable) {
    return new ScriptRunner(scope, variable);
  }

  /**
   * Runs the specified Jython script. First runs the initialization script to
   * preload the appropriate client library version.
   */
  public static void run(String scriptfilename) {
    try {
      initPython();
      PythonInterpreter python = new PythonInterpreter();
      
      python.execfile(scriptfilename);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  

  /** Initialize the python interpreter. */
  private static void initPython() {
    Properties props = new Properties();
    // Default is 'message' which displays sys-package-mgr bloat
    // Choose one of error,warning,message,comment,debug
    props.setProperty("python.verbose", "error");
    props.setProperty("python.path", System.getProperty("java.class.path"));
    PythonInterpreter.initialize(System.getProperties(), props, new String[] {""});
  }

  /**
   * Create and run a console using a new python interpreter for the test
   * associated with this instance.
   */
  public void console() throws IOException {
    initPython();
    InteractiveConsole python = new InteractiveConsole();
    initInterpreter(python, scope, variable);
    python.interact();
  }

  /**
   * Start an interactive python interpreter using the specified set of local
   * variables. Use this to interrupt a running test script with a prompt:
   * 
   * @param locals
   */
  public static void console(PyObject locals) {
    initPython();
    InteractiveConsole python = new InteractiveConsole(locals);
    python.interact();
  }

  /**
   * Initialize a python interpreter.
   * 
   * @param python
   * @param scope
   * @throws IOException
   */
  public static void initInterpreter(PythonInterpreter python, Object scope, String variable) 
      throws IOException {
    // Store the current test case as the this variable
    python.set(variable, scope);
  }
}
