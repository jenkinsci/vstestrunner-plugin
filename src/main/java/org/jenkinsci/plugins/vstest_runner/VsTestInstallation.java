package org.jenkinsci.plugins.vstest_runner;

import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.EnvVars;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;

/**
 * @author Yasuyuki Saito
 */
public class VsTestInstallation extends ToolInstallation implements NodeSpecific<VsTestInstallation>, EnvironmentSpecific<VsTestInstallation> {


    public static transient final String DEFAULT = "Default";

    private static final long serialVersionUID = 1;

    /** */
    @Deprecated
    private transient String pathToVsTest;

    /**
     * @param name
     * @param home
     */
    @DataBoundConstructor
    public VsTestInstallation(String name, String home) {
        super(name, home, null);
    }

    /**
     *
     */
    public VsTestInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new VsTestInstallation(getName(), translateFor(node, log));
    }

    /**
     * @param environment
     * @return
     */
    public VsTestInstallation forEnvironment(EnvVars environment) {
        return new VsTestInstallation(getName(), environment.expand(getHome()));
    }

    /**
     * Used for backward compatibility
     *
     * @return the new object, an instance of MsBuildInstallation
     */
    protected Object readResolve() {
        if (this.pathToVsTest != null) {
            return new VsTestInstallation(this.getName(), this.pathToVsTest);
        }
        return this;
    }

    public String getVsTestExe() {
        return getHome();
    }

    public static VsTestInstallation getDefaultInstallation() {
        DescriptorImpl vstestsTools = Jenkins.getInstance().getDescriptorByType(VsTestInstallation.DescriptorImpl.class);
        VsTestInstallation tool = vstestsTools.getInstallation(VsTestInstallation.DEFAULT);
        if (tool != null) {
            return tool;
        } else {
            VsTestInstallation[] installations = vstestsTools.getInstallations();
            if (installations.length > 0) {
                return installations[0];
            } else {
                onLoaded();
                return vstestsTools.getInstallations()[0];
            }
        }
    }

    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public static void onLoaded() {
        DescriptorImpl descriptor = (VsTestInstallation.DescriptorImpl) Jenkins.getInstance().getDescriptor(VsTestInstallation.class);
        assert descriptor != null;
        VsTestInstallation[] installations = descriptor.getInstallations();
        if (installations != null && installations.length > 0) {
            return;
        }
        String defaultVSTestExe = isWindows() ? "vstest.console.exe" : "vstest.console";
        VsTestInstallation tool = new VsTestInstallation(DEFAULT, defaultVSTestExe);
        descriptor.setInstallations(tool);
        descriptor.save();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    private static boolean isWindows() {
        return File.pathSeparatorChar == ';';
    }

    /**
     * @author Yasuyuki Saito
     */
    @Extension
    public static class DescriptorImpl extends ToolDescriptor<VsTestInstallation> {

        public DescriptorImpl() {
            super();
            load();
        }

        public String getDisplayName() {
            return Messages.VsTestInstallation_DisplayName();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) {
            setInstallations(req.bindJSONToList(VsTestInstallation.class, json.get("tool"))
                    .toArray((VsTestInstallation[]) Array.newInstance(VsTestInstallation.class, 0)));
            save();
            return true;
        }

        @Nullable
        public VsTestInstallation getInstallation(String name) {
            for (VsTestInstallation i : getInstallations()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

    }
}
