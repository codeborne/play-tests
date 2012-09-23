play-selenide
=============

Play framework module for easy integration with Selenide (based on Selenium Web Driver) to be able to write UI tests for Play apps in plain Java instead of html files.

Add it to your dependencies.yml
-------------------------------

    require:
        - play
        - play-selenide -> selenide 0.1
    
    repositories:
        - play-selenide:
          type: http
          artifact: http://cloud.github.com/downloads/codeborne/play-selenide/[module]-[revision].zip
          contains:
            - play-selenide -> *

Your first test
---------------

Make sure you have a test directory in your Play app.

Create a JUnit class there, eg:

	public class RegistrationSpec extends play.test.UITest {  
		@Before 
		public void setUp() throws Exception {    
			open("/"); // will start the play app in %test config as well as the browser, firefox by default
		}
	
		@Test
		public void canRegister() throws Exception {
			click(By.id("register-button"));
			// write your regular Selenide code here
		}
	}

From there you can run these tests from your favorite IDE or other test runner.
