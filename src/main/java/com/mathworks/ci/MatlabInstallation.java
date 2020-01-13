package com.mathworks.ci;

import java.io.IOException;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;

public class MatlabInstallation extends ToolInstallation implements NodeSpecific<MatlabInstallation>, EnvironmentSpecific<MatlabInstallation> {
	
	@DataBoundConstructor
	public MatlabInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
		super(name, home, properties);
	}

	private static final long serialVersionUID = 1L;
  

    public static MatlabInstallation[] list() {
        return ((MatlabInstallation.DescriptorImpl)ToolInstallation.all().get(MatlabInstallation.DescriptorImpl.class)).getInstallations();
    }
    
	@Override
	public MatlabInstallation forEnvironment(EnvVars environment) {
		return new MatlabInstallation(this.getName(), environment.expand(this.getHome()), this.getProperties().toList());
	}
	
    public MatlabInstallation forNode(final Node node, final TaskListener log) throws IOException, InterruptedException {
        return new MatlabInstallation(this.getName(), this.translateFor(node, log), this.getProperties().toList());
    }

    public MatlabInstallation forBuild(final TaskListener listener, final EnvVars environment) throws IOException, InterruptedException {
        return this.forNode(Computer.currentComputer().getNode(), listener).forEnvironment(environment);
    }
    
    /*
     * MATLAB installation descriptor
     * 
     */
    @Extension
    public static class DescriptorImpl extends ToolDescriptor<MatlabInstallation> {
    	
    	@CopyOnWrite
    	private volatile MatlabInstallation[] installations = new MatlabInstallation[0];
    	
    	@Override
    	public String getDisplayName() {
    	    return "MATLAB";
    	}
    	
    	public MatlabInstallation[] getInstallations() {
    	    return installations;
    	}
    	
    	public void setInstallations(MatlabInstallation... installations) {
    	    // Store installations
    	    this.installations = installations;
    	    // Save on disk
    	    save();
    	}
    }

    public static MatlabInstallation fromName(final String name) {
        for (final MatlabInstallation installation : list()) {
            if (name != null && name.equals(installation.getName())) {
                return installation;
            }
        }
        return null;
    }



}
