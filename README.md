play-tests
=============

Play framework module for easy integration with Selenide (based on Selenium Web Driver) to be able to write UI tests for Play apps in plain Java instead of html files.

Add it to your dependencies.yml
-------------------------------

    require:
        - play
        - play-tests -> tests 3.10
    
    repositories:
        - play-tests:
          type: http
          artifact: http://repo.codeborne.com/[organization]/[module]-[revision].zip
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

`play tests --threads=3` - runs UI tests in N parallel threads


## Changelog

### 4.4

Fixed methods `assertSuccessMessage`, `assertWarningMessage`, `assertInfoMessage`, `assertErrorMessage` to support
multiple messages (not only the first one).

### 4.1

Stop Play! application if tests are running too long (after 5 seconds from last test completion).

### 4.0

UI tests must be in a separate folder "test-ui" (instead of "test").

It increases performance of UI tests, because Play! doesn't need to compile/instrument all UNIT-tests twice.

### 3.12

Support for Java 8 (removed hard-coded language level 1.7)

### 3.10

Upgraded to Selenide 2.15 and Selenium 2.44.0

### 3.7

Upgraded to Selenide 2.13 and Selenium 2.43.1

### 3.6

Run UI tests in non-system-default time zone.
By default using "Asia/Krasnoyarsk", configurable via "selenide.play.timeZone" system property. 
It's good practice to run tests in another time zone to assure that tests are not relying on your system's default time zone.

### 3.5

 - do NOT run unit-tests from modules because they can break own tests  (but run UI tests from modules because they can behave differently)
 - avoid opening browser unless it's really needed
 
### 3.4

 - Calculate action coverage
 - duplicate logs of every test process to a separate file
 - log action coverage and webdriver statistics only in prod mode (on Jenkins)
 
### 3.1

Upgraded to Selenide 2.12 and Gradle 2.0

### 3.0

Added command "play ui-tests-with-coverage" that calculates code coverage (single-threaded, non-precompiled, slow run)

### 2.14.2

Added command "play compile-check" for checking that code base is compilable

### 2.14.1

Added option to define browser for UI tests, for example: -Dbrowser=firefox

### 2.10

Added support for Java8

### 2.7.11

Now "play unit-tests" calculates code coverage.

### 2.7.10

Clear default language before every test.

### 2.7.9

* Added possibility to save/restore database state before every test.

### 2.7.8

* Added MailMock for emulating smtp mail server.
* Do not invoke each test in Play context. It' just not needed.

### 2.7.7

* Do not re-start play if it's already started. Big performance improvement!

### 2.7.6

* Runs Play in precompiled mode
* Method assertAction() now waits until the URL actually changes