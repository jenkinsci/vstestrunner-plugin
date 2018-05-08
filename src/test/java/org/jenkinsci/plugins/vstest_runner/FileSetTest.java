/*
 * The MIT License
 *
 * Copyright 2014 BELLINSALARIN.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.vstest_runner;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

/**
 * @author BELLINSALARIN
 */
public class FileSetTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    private final String loggedIOException = "java.io.IOException: ";
    private final String missingExecutableMessage = "Cannot run program \"vstest.console\"";
    
    @Test
    public void testResolveFileSet_noMatch() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        VsTestBuilder builder = new VsTestBuilder();
        builder.setVsTestName("default");
        builder.setTestFiles("**\\*.Tests");
        builder.setSettings("");
        builder.setTests("");
        builder.setTestCaseFilter("");
        builder.setEnablecodecoverage(true);
        builder.setInIsolation(true);
        builder.setUseVsixExtensions(false);
        builder.setUseVs2017Plus(false);
        builder.setPlatform("");
        builder.setFramework("");
        builder.setLogger("trx");
        builder.setCmdLineArgs("");
        builder.setFailBuild(true);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();
        
        String logOutput = new String(Files.readAllBytes(build.getLogFile().toPath()), StandardCharsets.UTF_8);
        
        if(logOutput.contains(loggedIOException + missingExecutableMessage)){
            j.assertBuildStatus(Result.SUCCESS, build);
        }
        else{
            j.assertBuildStatus(Result.FAILURE, build);
            j.assertLogContains("no files matching the pattern **\\*.Tests", build);
        }
    }

    @Test
    public void testResolveFileSet_someMatch() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        VsTestBuilder builder = new VsTestBuilder();
        builder.setVsTestName("default");
        builder.setTestFiles("**\\*.Tests.dll");
        builder.setSettings("");
        builder.setTests("");
        builder.setTestCaseFilter("Priority=1|TestCategory=Odd Nightly");
        builder.setEnablecodecoverage(true);
        builder.setInIsolation(true);
        builder.setUseVsixExtensions(false);
        builder.setUseVs2017Plus(false);
        builder.setPlatform("");
        builder.setFramework("");
        builder.setLogger("trx");
        builder.setCmdLineArgs("");
        builder.setFailBuild(true);
        project.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("aaa/aaa.Tests.dll").write("La donna è mobile, qual piuma al vento", "UTF-8");
                build.getWorkspace().child("vstest.console.exe").chmod(700);
                return true;
            }

        });
        project.getBuildersList().add(builder);
        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();
        
        String logOutput = new String(Files.readAllBytes(build.getLogFile().toPath()), StandardCharsets.UTF_8);
        
        if(logOutput.contains(loggedIOException + missingExecutableMessage)){
            j.assertBuildStatus(Result.SUCCESS, build);
        }
        else{
            j.assertBuildStatus(Result.FAILURE, build);
            j.assertLogContains("/TestCaseFilter:\"Priority=1|TestCategory=Odd Nightly\"", build);
            j.assertLogContains("aaa/aaa.Tests.dll", build);
        }
    }
}
