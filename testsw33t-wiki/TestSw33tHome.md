
# Welcome to TestSw33t

## Problem Space
_Ever written web or http/REST services and then unit tested them via SOAPUI only to have to constantly tweak when switching your tests to another hosting environment?_ _Having to change messages and endpoint urls just to check out whether a service is functional and performant or not?_ _Wondering why a feature you tested 10mins earlier is now being reported as b0rked by a tester or worse your end user?_

## Solution Space

TestSw33t is a java-based tool that performs the following tasks:
 
*	Runs your existing SOAPUI test suites in a headless mode (using the native _testrunner.bat_ bundled with SOAPUI)
*	Performs property expansion within SOAP messages via the convention of configuring a file named _TestSuiteName_.properties.
*	Allows common properties to be shared and specified _once_ for all Test Suites via the bundled _runner.properties_
*	Aggregates all test suite results and generates a single html report powered by jQuery UI suitable for publishing to a wiki or CI Jenkins build.

## Progress 
Not all of the features mentioned about are implemented. Please see the [Feature Matrix](http://bitbucket.org/mesketh/testsw33t/wiki/feature-matrix.html) for an up to the minute report.