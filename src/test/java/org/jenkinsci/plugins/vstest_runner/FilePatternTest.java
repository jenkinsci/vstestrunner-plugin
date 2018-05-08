package org.jenkinsci.plugins.vstest_runner;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import org.apache.commons.lang.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FilePatternTest {

    private FilePath workspace;
    private File subfolder;

    @Before
    public void setUp() throws Exception {
        File parent = Util.createTempDir();
        workspace = new FilePath(parent);
        if (workspace.exists()) {
            workspace.deleteRecursive();
        }
        workspace.mkdirs();
        subfolder = new File(parent, "subfolder");
        if (subfolder.exists()) {
            boolean delete = subfolder.delete();
            assertThat(delete, is(true));
        }
        boolean mkdirs = subfolder.mkdirs();
        assertThat(mkdirs, is(true));
    }

    @After
    public void tearDown() throws Exception {
        workspace.deleteRecursive();
    }

    private String createFile(File folder, String child) throws Exception {
        File file = new File(folder, child);
        boolean newFile = file.createNewFile();
        assertThat(newFile, is(true));
        return file.getAbsolutePath();
    }

    @Test
    public void testGetTestFilesArgument() throws Exception {
        String absolutePath = createFile(subfolder, "testfile.trx");
        VsTestBuilder step = new VsTestBuilder();
        step.setTestFiles("**/*.trx");
        EnvVars envVars = new EnvVars();
        List<String> testFilesArguments = step.getTestFilesArguments(workspace, envVars);
        assertThat(testFilesArguments.size(), is(1));
        String trimmedPath = StringUtils.strip(testFilesArguments.get(0), "\"");
        String expected = StringUtils.strip(step.relativize(workspace, absolutePath), "\"");
        assertThat(trimmedPath, is(expected));
    }

    @Test
    public void testGetTestFilesArguments() throws Exception {
        createFile(subfolder, "testfile 1.trx");
        createFile(subfolder, "testfile2.trx");
        createFile(subfolder, "testfile3.trx");

        VsTestBuilder step = new VsTestBuilder();
        step.setTestFiles("**/*.trx");
        EnvVars envVars = new EnvVars();
        List<String> testFilesArguments = step.getTestFilesArguments(workspace, envVars);
        assertThat(testFilesArguments.size(), is(3));
    }

    @Test
    public void testGetFilesArguments_WithNewLine() throws Exception {
        createFile(subfolder, "testfile1.trx");
        createFile(subfolder, "testfile 2.trx");
        VsTestBuilder step = new VsTestBuilder();
        step.setTestFiles("**/testfile1.trx\n**/testfile 2.trx\n**/*.trx");
        EnvVars envVars = new EnvVars();
        List<String> testFilesArguments = step.getTestFilesArguments(workspace, envVars);
        assertThat(testFilesArguments.size(), is(2));
    }

    @Test
    public void testRelativePath() throws Exception {
        String absolutePath = createFile(subfolder, "testfile1.trx");
        VsTestBuilder step = new VsTestBuilder();
        String relativePath = step.relativize(workspace, absolutePath);
        step.setTestFiles(relativePath);
        EnvVars envVars = new EnvVars();
        List<String> testFilesArguments = step.getTestFilesArguments(workspace, envVars);
        assertThat(testFilesArguments.size(), is(1));
    }
}
