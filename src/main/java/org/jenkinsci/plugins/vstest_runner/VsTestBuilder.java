package org.jenkinsci.plugins.vstest_runner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.ComboBoxModel;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @author Yasuyuki Saito
 */
public class VsTestBuilder extends Builder implements SimpleBuildStep {

    private String vsTestName;
    private String testFiles;
    private String settings;
    private String tests;
    private String testCaseFilter;
    private String platform;
    private String framework;
    private String logger = DescriptorImpl.defaultLogger;
    private String cmdLineArgs;
    @Deprecated
    transient private String otherFramework;
    @Deprecated
    transient private String otherLogger;
    @Deprecated
    transient private String otherPlatform;
    private boolean inIsolation;
    private boolean useVsixExtensions;
    private boolean useVs2017Plus;
    private boolean enablecodecoverage = DescriptorImpl.defaultEnableCodeCoverage;
    private boolean failBuild = DescriptorImpl.defaultFailBuild;

    @DataBoundConstructor
    public VsTestBuilder() {

    }

    protected Object readResolve() {
        if (StringUtils.isNotBlank((otherPlatform))) {
            this.platform = otherPlatform;
        }
        if (StringUtils.isNotBlank(otherFramework)) {
            this.framework = otherFramework;
        }
        if (StringUtils.isNotBlank(otherLogger)) {
            this.logger = otherLogger;
        }
        return this;
    }

    public String getVsTestName() {
        return vsTestName;
    }

    public String getTestFiles() {
        return testFiles;
    }

    public String getSettings() {
        return settings;
    }

    public String getTests() {
        return tests;
    }

    public boolean isEnablecodecoverage() {
        return enablecodecoverage;
    }

    public boolean isInIsolation() {
        return inIsolation;
    }

    public boolean isUseVsixExtensions() {
        return useVsixExtensions;
    }

    public boolean isUseVs2017Plus() {
        return useVs2017Plus;
    }

    public String getPlatform() {
        return platform;
    }

    public String getFramework() {
        return framework;
    }

    public String getTestCaseFilter() {
        return testCaseFilter;
    }

    public String getLogger() {
        return logger;
    }

    public String getCmdLineArgs() {
        return cmdLineArgs;
    }

    public boolean isFailBuild() {
        return failBuild;
    }

    @DataBoundSetter
    public void setVsTestName(String vsTestName) {
        this.vsTestName = Util.fixEmptyAndTrim(vsTestName);
    }

    @DataBoundSetter
    public void setTestFiles(String testFiles) {
        this.testFiles = Util.fixEmptyAndTrim(testFiles);
    }

    @DataBoundSetter
    public void setSettings(String settings) {
        this.settings = Util.fixEmptyAndTrim(settings);
    }

    @DataBoundSetter
    public void setTests(String tests) {
        this.tests = Util.fixEmptyAndTrim(tests);
    }

    @DataBoundSetter
    public void setTestCaseFilter(String testCaseFilter) {
        this.testCaseFilter = Util.fixEmptyAndTrim(testCaseFilter);
    }

    @DataBoundSetter
    public void setPlatform(String platform) {
        this.platform = Util.fixEmptyAndTrim(platform);
    }

    @DataBoundSetter
    public void setFramework(String framework) {
        this.framework = Util.fixEmptyAndTrim(framework);
    }

    @DataBoundSetter
    public void setLogger(String logger) {
        this.logger = Util.fixEmptyAndTrim(logger);
        if (this.logger == null) {
            this.logger = DescriptorImpl.defaultLogger;
        }
    }

    @DataBoundSetter
    public void setCmdLineArgs(String cmdLineArgs) {
        this.cmdLineArgs = Util.fixEmptyAndTrim(cmdLineArgs);
    }

    @DataBoundSetter
    public void setEnablecodecoverage(boolean enablecodecoverage) {
        this.enablecodecoverage = enablecodecoverage;
    }

    @DataBoundSetter
    public void setInIsolation(boolean inIsolation) {
        this.inIsolation = inIsolation;
    }

    @DataBoundSetter
    public void setUseVsixExtensions(boolean useVsixExtensions) {
        this.useVsixExtensions = useVsixExtensions;
    }

    @DataBoundSetter
    public void setUseVs2017Plus(boolean useVs2017Plus) {
        this.useVs2017Plus = useVs2017Plus;
    }

    @DataBoundSetter
    public void setFailBuild(boolean failBuild) {
        this.failBuild = failBuild;
    }

    @NonNull
    public VsTestInstallation getVsTest(TaskListener listener) {
        if (vsTestName == null) return VsTestInstallation.getDefaultInstallation();
        VsTestInstallation.getDefaultInstallation();
        VsTestInstallation tool = Jenkins.getInstance().getDescriptorByType(VsTestInstallation.DescriptorImpl.class).getInstallation(vsTestName);
        if (tool == null) {
            listener.getLogger().println("Selected VSTest.Console installation does not exist. Using Default");
            tool = VsTestInstallation.getDefaultInstallation();
        }
        return tool;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * @author Yasuyuki Saito
     */
    @Extension
    @Symbol("vsTest")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public static final boolean defaultFailBuild = true;
        public static final boolean defaultEnableCodeCoverage = true;
        public static final String defaultLogger = VsTestLogger.TRX.toString();

        public DescriptorImpl() {
            super(VsTestBuilder.class);
            load();
        }

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.VsTestBuilder_DisplayName();
        }

        @SuppressWarnings("unused") // Used by Stapler
        public boolean showVSTestToolOptions() {
            return getVSTestToolDescriptor().getInstallations().length > 1;
        }

        private VsTestInstallation.DescriptorImpl getVSTestToolDescriptor() {
            return Jenkins.getInstance().getDescriptorByType(VsTestInstallation.DescriptorImpl.class);
        }

        public List<VsTestInstallation> getVsTestTools() {
            VsTestInstallation[] vsTestInstallations = getVSTestToolDescriptor().getInstallations();
            return Arrays.asList(vsTestInstallations);
        }

        @SuppressWarnings("unused") // Used by Stapler
        public ListBoxModel doFillVsTestNameItems() {
            ListBoxModel r = new ListBoxModel();
            for (VsTestInstallation vsTestInstallation : getVsTestTools()) {
                r.add(vsTestInstallation.getName());
            }
            return r;
        }

        @SuppressWarnings("unused") // Used by Stapler
        public ComboBoxModel doFillPlatformItems() {
            return fillComboBox(VsTestPlatform.class);
        }

        @SuppressWarnings("unused") // Used by Stapler
        public ComboBoxModel doFillFrameworkItems() {
            return fillComboBox(VsTestFramework.class);
        }

        @SuppressWarnings("unused") // Used by Stapler
        public ComboBoxModel doFillLoggerItems() {
            return fillComboBox(VsTestLogger.class);
        }

        private <E extends Enum<E>> ComboBoxModel fillComboBox(Class<E> clazz) {
            ComboBoxModel r = new ComboBoxModel();
            for (Enum<E> enumVal : clazz.getEnumConstants()) {
                r.add(enumVal.toString());
            }
            return r;
        }
    }

    /**
     *
     */
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        ArrayList<String> args = new ArrayList<String>();

        EnvVars env = run.getEnvironment(listener);

        // VsTest.console.exe path.
        String pathToVsTest = getVsTestPath(workspaceToNode(workspace), listener, env);
        args.add(pathToVsTest);

        // Target dll path
        if (!StringUtils.isBlank(testFiles)) {
            List<String> targets = getTestFilesArguments(workspace, env);
            if (targets.size() == 0) {
                listener.getLogger().println("no files matching the pattern " + this.testFiles);
                if (this.failBuild) {
                    run.setResult(Result.FAILURE);
                    throw new AbortException("no files matching the pattern " + this.testFiles);
                }
            }
            args.addAll(targets);
        }

        // Run tests with additional settings such as data collectors.
        if (!StringUtils.isBlank(settings)) {
            args.add(convertArgumentWithQuote("Settings", replaceMacro(settings, env)));
        }

        // Run tests with names that match the provided values.
        if (!StringUtils.isBlank(tests)) {
            args.add(convertArgument("Tests", replaceMacro(tests, env)));
        }

        // Run tests that match the given expression.
        if (!StringUtils.isBlank(testCaseFilter)) {
            args.add(convertArgumentWithQuote("TestCaseFilter", replaceMacro(testCaseFilter, env)));
        }

        // Enables data diagnostic adapter CodeCoverage in the test run.
        if (enablecodecoverage) {
            args.add("/Enablecodecoverage");
        }

        // Runs the tests in an isolated process.
        if (inIsolation) {
            args.add("/InIsolation");
        }

        // This makes vstest.console.exe process use or skip the VSIX extensions installed (if any) in the test run.
        if (!useVs2017Plus) {
            if (useVsixExtensions) {
                args.add("/UseVsixExtensions:true");
            } else {
                args.add("/UseVsixExtensions:false");
            }
        }

        // Target platform architecture to be used for test execution.
        String platformArg = getPlatformArgument(env);
        if (!StringUtils.isBlank(platformArg)) {
            args.add(convertArgument("Platform", platformArg));
        }

        // Target .NET Framework version to be used for test execution.
        String frameworkArg = getFrameworkArgument(env);
        if (!StringUtils.isBlank(frameworkArg)) {
            args.add(convertArgument("Framework", frameworkArg));
        }

        // Specify a logger for test results.
        String loggerArg = getLoggerArgument(env);
        if (!StringUtils.isBlank(loggerArg)) {
            args.add(convertArgument("Logger", loggerArg));
        }

        // Manual Command Line String
        if (!StringUtils.isBlank(cmdLineArgs)) {
            args.add(replaceMacro(cmdLineArgs, env));
        }

        // VSTest run.
        execVsTest(args, run, workspace, launcher, listener, env);
    }

    /**
     * @param value
     * @param env
     * @return
     */
    private String replaceMacro(String value, EnvVars env) {
        String result = Util.replaceMacro(value, env);
        return result;
    }

    /**
     * @param builtOn
     * @param listener
     * @param env
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    @NonNull
    private String getVsTestPath(Node builtOn, TaskListener listener, EnvVars env) {
        VsTestInstallation installation = getVsTest(listener);
        if (builtOn != null) {
            try {
                installation = installation.forNode(builtOn, listener);
            } catch (IOException | InterruptedException e) {
                listener.getLogger().println("Failed to get VSTest.Console executable");
            }
        }
        if (env != null) {
            installation = installation.forEnvironment(env);
        }

        String vsTestExe = installation.getVsTestExe();

        listener.getLogger().println("Path To VSTest.console.exe: " + vsTestExe);

        return vsTestExe;
    }

    /**
     * @param workspace
     * @param env
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    /* package */ List<String> getTestFilesArguments(FilePath workspace, EnvVars env) throws InterruptedException {
        Set<String> files = new HashSet<>();

        StringTokenizer testFilesTokenizer = new StringTokenizer(testFiles, " \t\r\n");

        while (testFilesTokenizer.hasMoreTokens()) {
            String testFile = testFilesTokenizer.nextToken();
            testFile = replaceMacro(testFile, env);

            if (!StringUtils.isBlank(testFile)) {
                try {
                    for (FilePath filePath : workspace.list(testFile)) {
                        files.add(appendQuote(relativize(workspace, filePath)));
                    }
                } catch (IOException ignored) {
                }
            }
        }

        return new ArrayList<>(files);
    }

    /**
     * @param env
     * @return
     */
    private String getPlatformArgument(EnvVars env) {
        return replaceMacro(platform, env);
    }

    /**
     * @param env
     * @return
     */
    private String getFrameworkArgument(EnvVars env) {
        String expanded = replaceMacro(framework, env);
        return useVs2017Plus ? StringUtils.capitalize(expanded) : expanded;
    }

    /**
     * @param env
     * @return
     */
    private String getLoggerArgument(EnvVars env) {
        return replaceMacro(logger, env);
    }

    /**
     * @param base
     * @param path
     * @return the relative path of 'path'
     * @throws InterruptedException
     * @throws IOException
     */
    /* package */ String relativize(FilePath base, FilePath path) throws InterruptedException, IOException {
        return base.toURI().relativize(path.toURI()).getPath();
    }

    /**
     * @param args
     * @param run
     * @param workspace
     * @param launcher
     * @param listener
     * @param env
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private void execVsTest(List<String> args, Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars env) throws InterruptedException, IOException {
        ArgumentListBuilder cmdExecArgs = new ArgumentListBuilder();
        FilePath tmpDir = null;

        if (!launcher.isUnix()) {
            tmpDir = workspace.createTextTempFile("vstest", ".bat", concatString(args), false);
            cmdExecArgs.add("cmd.exe", "/C", tmpDir.getRemote(), "&&", "exit", "%ERRORLEVEL%");
        } else {
            for (String arg : args) {
                cmdExecArgs.add(arg);
            }
        }

        listener.getLogger().println("Executing VSTest: " + cmdExecArgs.toStringWithQuote());

        try {
            VsTestListenerDecorator parserListener = new VsTestListenerDecorator(listener);
            int r = launcher.launch().cmds(cmdExecArgs).envs(env).stdout(parserListener).pwd(workspace).join();

            String trxFullPath = parserListener.getTrxFile();
            String trxPathRelativeToWorkspace = null;
            String coverageFullPath = parserListener.getCoverageFile();
            String coveragePathRelativeToWorkspace = null;

            if (trxFullPath != null) {
                trxPathRelativeToWorkspace = relativize(workspace, workspace.child(trxFullPath));
            }
            if (coverageFullPath != null) {
                coveragePathRelativeToWorkspace = relativize(workspace, workspace.child(parserListener.getCoverageFile()));
            }

            run.addAction(new AddVsTestEnvVarsAction(trxPathRelativeToWorkspace, coveragePathRelativeToWorkspace));

            if (r != 0) {
                if (failBuild) {
                    run.setResult(Result.FAILURE);
                    throw new AbortException("VsTest.Console exited with " + r);
                } else {
                    run.setResult(Result.UNSTABLE);
                }
            }
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("VSTest command execution failed"));
        } finally {
            try {
                if (tmpDir != null) {
                    tmpDir.delete();
                }
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError("temporary file delete failed"));
            }
        }
    }

    /**
     * @param option
     * @param param
     * @return
     */
    private String convertArgument(String option, String param) {
        return String.format("/%s:%s", option, param);
    }

    /**
     * @param option
     * @param param
     * @return
     */
    private String convertArgumentWithQuote(String option, String param) {
        return String.format("/%s:\"%s\"", option, param);
    }

    /**
     * @param value
     * @return
     */
    private String appendQuote(String value) {
        return String.format("\"%s\"", value);
    }

    /**
     * @param args
     * @return
     */
    private String concatString(List<String> args) {
        StringBuilder buf = new StringBuilder();
        for (String arg : args) {
            if (buf.length() > 0) {
                buf.append(' ');
            }
            buf.append(arg);
        }
        return buf.toString();
    }

    private static Node workspaceToNode(FilePath workspace) {
        Computer computer = workspace.toComputer();
        Node node = null;
        if (computer != null) node = computer.getNode();
        return node != null ? node : Jenkins.getInstance();
    }

    private static class AddVsTestEnvVarsAction implements EnvironmentContributingAction {

        private final static String TRX_ENV = "VSTEST_RESULT_TRX";
        private final static String COVERAGE_ENV = "VSTEST_RESULT_COVERAGE";

        private final String trxEnv;
        private final String coverageEnv;

        public AddVsTestEnvVarsAction(String trxEnv, String coverageEnv) {
            this.trxEnv = trxEnv;
            this.coverageEnv = coverageEnv;
        }

        public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
            if (trxEnv != null) {
                env.put(TRX_ENV, trxEnv);
            }

            if (coverageEnv != null) {
                env.put(COVERAGE_ENV, coverageEnv);
            }
        }

        public String getDisplayName() {
            return "Add VSTestRunner Environment Variables to Build Environment";
        }

        public String getIconFileName() {
            return null;
        }

        public String getUrlName() {
            return null;
        }
    }
}
