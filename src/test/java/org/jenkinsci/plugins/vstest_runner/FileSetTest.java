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

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

/**
 *
 * @author BELLINSALARIN
 */
public class FileSetTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testResolveFileSet_noMatch() throws InterruptedException, IOException, Exception {

        FreeStyleProject project = j.createFreeStyleProject();
        VsTestBuilder builder = new VsTestBuilder("default", "**\\*.Tests", "", "", "", true, true, false, "", "", "", "", "trx", "", "", true);
        project.getBuildersList().add(builder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        //build.getBuildStatusSummary().message;
        assertTrue(build.getResult() == Result.FAILURE);
        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("no file matches the pattern **\\*.Tests"));
        //String content = build.getWorkspace().child("AssemblyVersion.cs").readToString();
    }

    @Test
    public void testResolveFileSet_someMatch() throws InterruptedException, IOException, Exception {

        FreeStyleProject project = j.createFreeStyleProject();
        VsTestBuilder builder = new VsTestBuilder("default", "**\\*.Tests.dll", "", "", "", true, true, false, "", "", "", "", "trx", "", "", true);
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("aaa\\aaa.Tests.dll").write("La donna Ã¨ mobile, qual piÃ¹ mal vento", "UTF-8");
                build.getWorkspace().child("vstest.console.exe").chmod(700);
                return true;
            }
        });
        project.getBuildersList().add(builder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        //build.getBuildStatusSummary().message;
        assertTrue(build.getResult() == Result.FAILURE);
        String s = FileUtils.readFileToString(build.getLogFile());
        //assertTrue(s.contains("no file matches the pattern **\\*.Tests.dll"));
        //String content = build.getWorkspace().child("AssemblyVersion.cs").readToString();
        assertTrue(s.contains("aaa" + File.separator + "aaa.Tests.dll"));
    }
}
