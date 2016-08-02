play-tests
=============

Play framework module for easy integration with Selenide (based on Selenium Web Driver) to be able to write UI tests for Play apps in plain Java instead of html files.

Add it to your dependencies.yml
-------------------------------

    require:
        - play 1.3+
        - play-codeborne -> tests 6.6.3

    repositories:
        - codeborne:
            type: http
            artifact: https://repo.codeborne.com/play-[module]/[module]-[revision].zip
            contains:
                - play-codeborne -> *

Your first test
---------------

Make sure you have a test directory in your Play app.

Create a JUnit class there, eg:

  import static com.codeborne.selenide.Selenide.*;

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

### 6.6.3

* When compiling java sources and tests, compiler is executed in "fork" mode with more memory (up to 4 GB)

### 6.6.2

* exclude old org.hamcrest 1.1

### 6.6.1

* downgrade to video-recorder 1.0.6

### 6.6

* upgrade to video-recorder 1.0.7
* use hamcrest-all 1.3 instead of hamcrest-core 1.3

### 6.5

* upgrade to video-recorder 1.0.6
* upgrade to selenide 3.7
* upgrade to gradle 2.14.1

### 6.4

* upgrade to video-recorder 1.0.3
* upgrade to jacoco 0.7.7.201606060606

### 6.3

* Add video recorder. Just add `@Video` annotation to tests that you want video for (in case of failure)
* upgrade to Gradle 2.14

### 6.2

* upgrade to selenide 3.6
* upgrade to slf4j 1.7.21
* Now calculation of webdriver statistics is disabled by default
  * ... because I am afraid that it can take lot of time
  * It still can be enabled by system property "selenide.play.calculate-webdriver-statistics"
 
### 6.1

Folder "conf" from modules is not included in classpath anymore.
Only application's own "conf" folder is added to classpath.
 
### 6.0

This is a major update that makes tests independent from running Play! application.

When running tests,
 * thread context classloader is not Play classloader anymore
 * test is not executed in JPA transaction anymore
 * test classes are not instrumented with Play anymore 
  Pros: It's faster and simpler infrastructure
  Cons: 
    * JPA operations will not work in tests (aka JPAEhnancer)
    * field access will not be replaced by getters/setters (aka PropertiesEnhancer)
 * removed (unused) class play.test.PlayContextTestInvoker
 * upgrade to Selenide 3.5.1
 * upgrade to Gradle 2.13

### 5.7.1

 * remove old gson dependency (play includes a newer one) 

### 5.7

 * upgrade to Selenide 3.5 and Selenium 2.53.0

### 5.6

 * upgrade to Selenide 3.4 and Selenium 2.52.0
 * get back exit codes 101 and 102  (code 666 doesn't work because exit code can be 0..127)

### 5.5

 * replace error code 102/102 to more obvious 666

### 5.4

 * upgrade to Selenide 3.3 and Gradle 2.11

### 5.3

* upgraded to JaCoCo 0.7.5.201505241946

### 5.2

* upgraded to selenide 3.2
* upgraded to selenium-java 2.50.0

### 5.1

* upgraded to selenide 3.1
* upgraded to selenium-java 2.49.0
* upgraded to gradle 2.10

### 5.0

* upgraded to selenide 3.0

### 4.27

* upgraded to selenide 2.25

### 4.26.1

* upgrade to selenide 2.24 and selenium 2.48.2
* upgrade to gradle 2.9

### 4.26

* upgrade to selenide 2.24
* upgrade to gradle 2.8

### 4.25.1

* Enlarge text execution timeout to 2 minutes and test preparation time to 20 seconds

### 4.25

* Return the old good browser opening/closing mechanism without timeout checks

### 4.24

* Upgrade to Selenide 2.23
* Simplified methods assertSuccessMessage() etc.
* upgrade to Gradle 2.7

### 4.23.5

* add parameter `application.path` when running UI tests
 
 * example: `play tests --application.path=dist`
 * in this case Play application is run in "dist" subfolder (to use all precompiled less, js and other resources)

### 4.23

* run tests in "dist" subfolder (to use all precompiled less, js and other resources)

### 4.22

* improved assertSuccessMessage() and other checks in TwitterBootstrapUITest
* upgrade to selenide 2.22
* upgrade to Gradle 2.6

### 4.21

* upgrade to selenide 2.21
* fixed NPE in assertSuccessMessage() in case of empty collection

### 4.20

* upgrade to selenide 2.20

### 4.19

* remove old cglib 2.x dependency (coming with Selenium). Play uses cglib 3.x

### 4.18

* make thread dump periodically if Play cannot start in time
* upgrade to Selenide 2.19
* upgrade to Selenium 2.46.0

### 4.17

* upgrade to Selenide 2.18.2  (bugfix for issue https://github.com/codeborne/selenide/issues/182)

### 4.16

* upgrade to Selenide 2.18.1  (bugfix for issue https://github.com/codeborne/selenide/issues/180)

### 4.15

* Upgrade to selenide 2.18 (major improvement of "waiting" algorithm)
* Upgrade to Gradle 2.3
* Kill Play only in prod mode (in development test can run very long - e.g. paused on breakpoint)

### 4.14

Folder "test-ui" is now optional

### 4.13

Upgrade to selenide 2.17, selenium 2.45.0 - thus fixed incompatibility problems with FireFix 36

### 4.11

Does not modify play tmp folder to assure reusing of compiled classes between test runs

### 4.10

Use mockito-core instead of mockito-all to avoid including old hamcrest 1.1

### 4.9

Added command "play pitest" for running mutation tests. See http://pitest.org/ for details.

### 4.8

Upgraded to Selenide 2.16 with test reports

### 4.7

Immediately stop test execution if failed to start Play! application (otherwise multiple attempts to start play will cause OutOfMemory error).

### 4.5

Methods `assertSuccessMessage`, `assertWarningMessage`, `assertInfoMessage`, `assertErrorMessage` return found SelenideElement

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


# Thanks

Many thanks to these incredible tools that help us creating open-source software:

![Intellij IDEA](http://www.jetbrains.com/idea/docs/logo_intellij_idea.png)

![YourKit Java profiler](http://selenide.org/images/yourkit.png)

# License
play-tests is open-source project and distributed under [MIT](http://choosealicense.com/licenses/mit/) license
