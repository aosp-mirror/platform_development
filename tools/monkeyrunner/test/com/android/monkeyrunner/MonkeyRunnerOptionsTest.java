// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.monkeyrunner;

import junit.framework.TestCase;

import java.io.File;
import java.util.Iterator;

/**
 * Unit Tests to test command line argument parsing.
 */
public class MonkeyRunnerOptionsTest extends TestCase {
  // We need to use a file that actually exists
  private static final String FILENAME = "/etc/passwd";

  public void testSimpleArgs() {
    MonkeyRunnerOptions options =
      MonkeyRunnerOptions.processOptions(new String[] { FILENAME });
    assertEquals(options.getScriptFile(), new File(FILENAME));
  }

  public void testParsingArgsBeforeScriptName() {
    MonkeyRunnerOptions options =
      MonkeyRunnerOptions.processOptions(new String[] { "-be", "stub", FILENAME});
    assertEquals("stub", options.getBackendName());
    assertEquals(options.getScriptFile(), new File(FILENAME));
  }

  public void testParsingScriptArgument() {
    MonkeyRunnerOptions options =
      MonkeyRunnerOptions.processOptions(new String[] { FILENAME, "arg1", "arg2" });
    assertEquals(options.getScriptFile(), new File(FILENAME));
    Iterator<String> i = options.getArguments().iterator();
    assertEquals("arg1", i.next());
    assertEquals("arg2", i.next());
  }

  public void testParsingScriptArgumentWithDashes() {
    MonkeyRunnerOptions options =
      MonkeyRunnerOptions.processOptions(new String[] { FILENAME, "--arg1" });
    assertEquals(options.getScriptFile(), new File(FILENAME));
    assertEquals("--arg1", options.getArguments().iterator().next());
  }

  public void testMixedArgs() {
    MonkeyRunnerOptions options =
      MonkeyRunnerOptions.processOptions(new String[] { "-be", "stub", FILENAME,
          "arg1", "--debug=True"});
    assertEquals("stub", options.getBackendName());
    assertEquals(options.getScriptFile(), new File(FILENAME));
    Iterator<String> i = options.getArguments().iterator();
    assertEquals("arg1", i.next());
    assertEquals("--debug=True", i.next());
  }
}
