# Welcome to TestSw33t

## Problem Space
_Ever written web or http/REST services and then unit tested them via SOAPUI only to have to constantly tweak when switching your tests to another hosting environment?_ _Having to change messages and endpoint urls just to check out whether a service is functional and performant or not?_ _Wondering why a feature you tested 10mins earlier is now being reported as b0rked by a tester or worse your end user?_

## Solution

TestSw33t is a java-based tool that performs the following tasks:
 
*	Runs your existing SOAPUI test suites in a headless mode (using the native ```testrunner.bat``` bundled with SOAPUI)
*	Performs property expansion within SOAP messages via the convention of configuring a file named _TestSuiteName_.properties.
*	Allows common properties to be shared and specified _once_ for all Test Suites via the bundled ```runner.properties```
*	Aggregates all test suite results and generates a single html report powered by jQuery UI suitable for publishing to a wiki or CI Jenkins build.

## Progress 
Not all of the features mentioned about are implemented. Please see the [Feature Matrix](./feature-matrix.html) for an up to the minute report.

---

## Installation and Configuration
Use Mercurial to clone the project to your machine: 

```hg clone https://bitbucket.org/mesketh/testsw33t```

Now configure the following in your _src/main/resources/runner.properties_

   - ```soapui.testrunner.path``` to point to your SOAPUI installation. Note: this should include the bin directory to allow SOAPUI to be executed through it's headless batch file (required).
   -  ```soapui.report.dir``` - this is the the directory into which you would like your final html report generated (required).
   -  ```soapui.project.file``` - this is the full path to your SOAPUI project holding your test suites to run and report on (required).

## Getting Started

To begin using _TestSw33t_ it's recommended you follow these guidelines: 

   - Author your SOAPUI test cases and organise them into top level Test Suites preferably 1:1 suites per environment-under-test so for say E1 you would have a suite full of test cases evaluating E1 hosted services that your application is reliant upon and so on for higher/other environments.
   - Matching your test suite names are any _.properties_ files that your test cases use for performing environment-sensitive property expansion for example, in the _samples/_ there is a soap ui project containing two test suites each with their own associated properties files (```GWTestSuite.properties``` & ```CCTestSuite.properties```) for properties pertaining to specific test cases where these properties are expanded prior to running. 
   - Any reusable and/or default properties can be included in the default ```runner.properties```.
  
## Sample Report
  
  Here's a [sample report](./sample/report.html) generated from executing all the tests in the ```sample\``` SOAPUI project included.
  