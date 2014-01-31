play-selenide
=============

Play framework module for easy integration with Selenide (based on Selenium Web Driver) to be able to write UI tests for Play apps in plain Java instead of html files.

Add it to your dependencies.yml
-------------------------------

    require:
        - play
        - play-tests -> play-tests 2.7.6
    
    repositories:
        - play-tests:
          type: http
          artifact: http://repo.codeborne.com/play-selenide/[module]-[revision].zip
          contains:
            - play-tests -> *

Your first test
---------------

Make sure you have a test directory in your Play app.

Create a JUnit class there, eg:

  import static com.codeborne.Selenide.*;

	public class RegistrationSpec extends play.test.UITest {  
		@Before 
		public void setUp() {
			open("/"); // will start the play app in %test config as well as the browser, firefox by default
		}
	
		@Test
		public void canRegister() {
			$("#register-button").click();
			// write your regular Selenide code here
		}
	}

From there you can run these tests from your favorite IDE or other test runner.

Configuration
-------------
By default, play-selenide starts Play server in "test" mode before running tests.
If you need to disable it (for example, for running tests against remote server), you can do it by the following 
system property: `-Dselenide.play.start=false`

Running
-------------

`play tests`
 Compile and run unit-, integration- and UI tests

 `play clean-tests`
 Cleans compiled classes and test results

 `pay unit-tests`
 Runs unit-tests (all tests excluding ui/**, integration/**, itest/**)

 `play itests`
 Runs integration tests (all tests in folders itest/**)

 `pay ui-tests`
 Runs UI tests (all tests in folders ui/**)


Additional command line options
-------------
`play tests --remote_debug=true`  - runs tests with remote debug option

`play unit-tests --daemon`  - runs Gradle as daemon - should cause faster Gradle startup

`play unit-tests --gradle_opts=--debug` - uses additional Gradle options

`play tests --uitest=ui/SomeSingleTest*` - runs single UI test instead of all UI tests