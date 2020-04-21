# VSTestRunner Plugin

This plugin allow you to execute test using
[VsTest](http://msdn.microsoft.com/en-us/library/vstudio/ms182486.aspx)
command line tool.

## Description

This plugin allow you to execute test using
[VsTest](http://msdn.microsoft.com/en-us/library/vstudio/ms182486.aspx)
command line tool.  
VSTest are test classes for .NET framework which are integrated into
Visual Studio.

## Configuration

### System configuration

![vstestrunner plugin tool config](docs/images/vstestrunner-tool-config.png)

* Open the system configuration page "Manage Jenkins→Global Tool Configuration"
* Enter the path to the VsTest command line client, that should be used by Jenkins.
* The path location is as instance: `C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\Common7\\IDE\\CommonExtensions\\Microsoft\\TestWindow\\vstest.console.exe`.

### Job configuration

![vstestrunner plugin job config](docs/images/vstestrunner-job-config.png)

## Similar plugin

MSTestRunner Plugin  
https://wiki.jenkins-ci.org/display/JENKINS/MSTestRunner+Plugin
