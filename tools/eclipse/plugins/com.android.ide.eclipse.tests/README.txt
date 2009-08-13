This project contains the tests for the Android Eclipse Plugins.

You can do three things:
1- Run the unit tests as a full "eclipse plugin" suite
2- Run the unit tests as independent JUnit tests (not as plugin)
3. Run the functional tests as a full "eclipse plugin" suite (require a real SDK)

The unit tests are isolated tests that do not require external dependencies such as an SDK. 
The functional tests are higher level tests that may require a real SDK.

------------------------------------------
1- Running the unit tests as a full "eclipse plugin" suite
------------------------------------------

Steps to run the test suite:

A- In Eclipse, import following projects from development/tools/eclipse/plugins:
	- adt-tests
	- adt
	- ddms

B- Create a new "JUnit Plug-in Test" run configuration via the "Run > Open Run Dialog..." menu
Set the launch configuration's data as follows:
i. "Test" tab: 
  Select "Run a single test"
  Project: adt-tests 
  Test class: com.android.ide.eclipse.tests.UnitTests
  Test runner: JUnit 3
ii. "Arguments" tab:
 Set "VM Arguments" to 
"-Dtest_data=<adt>/plugins/com.android.ide.eclipse.tests/unittests/data/"
replacing "<adt>" with absolute filesystem path to the android plugin source location

All other fields can be left with their default values

C. Run the newly created launch configuration

Running the tests will run a secondary instance of Eclipse. 

Please note the following constraints to be aware of when writing tests to run within a plugin environment:

a. Access restrictions: cannot access package or protected members in a different
plugin, even if they are in the same declared package 
b. Using classloader.getResource or getResourceAsStream to access test data will 
likely fail in the plugin environment. Instead, use AdtTestData to access test files
in conjunction with the "test_data" environment variable mentioned above


-------------------------------------------
2- Run the unit tests as independent JUnit tests (not plugin)
-------------------------------------------

A- In Eclipse, import following projects from development/tools/eclipse/plugins:
	- adt-tests
	- adt
	- ddms

B- Select the "unittests" source folder, right-click and select
	"Run As > JUnit Test" (i.e. not the plugin tests)

This creates a debug configuration of type "JUnit Test" running all tests
in the source folder "unittests". The runtime must be JUnit 3.

Note: this method runs the tests within a regular JVM environment (ie not within
an Eclipse instance). This method has the advantage of being quicker than running
as a JUnit plugin test, and requires less potential set-up, but has the 
disadvantage of not properly replicating how the tests will be run in the 
continuous test environment. Tests that pass when run as "JUnit Tests" can
fail when run as "JUnit Plugin Tests", due to the extra constraints imposed by
running within an Eclipse plug-in noted in section 1.

------------------------------------------
3- Running the functional tests as a full "eclipse plugin" suite
------------------------------------------

Steps to run the test suite:

A- In Eclipse, import following projects from development/tools/eclipse/plugins:
	- adt-tests
	- adt
	- ddms

B - Setup an SDK on host machine, that is compatible with the Eclipse ADT plugins under test

C- Create a new "JUnit Plug-in Test" run configuration via the "Run > Open Run Dialog..." menu
Set the launch configuration's data as follows:
i. "Test" tab: 
  Select "Run a single test"
  Project: adt-tests 
  Test class: com.android.ide.eclipse.tests.FuncTests
  Test runner: JUnit 3
ii. "Environment" tab:
 Add a "sdk_home" environment variable, setting its path to the SDK from step B

All other fields can be left with their default values

D. Run the newly created launch configuration

Running the tests will run a secondary instance of Eclipse. 


