package com.mathworks.ci;

import java.io.IOException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildWrapper;
import com.mathworks.ci.MatlabInstallation.DescriptorImpl;

public class MatlabBuildWrapper extends SimpleBuildWrapper  {
	
	private String matlabRoot;
	private EnvVars env;
	
	@DataBoundConstructor
	 public MatlabBuildWrapper() {

	    }
	
	public String getMatlabRoot() {
		return matlabRoot;
	}

	@DataBoundSetter
	public void setMatlabRoot(String matlabRoot) {
		this.matlabRoot = matlabRoot;
	}
	
	private String getLocalMatlab() {
		return this.env == null ? getMatlabRoot(): this.env.expand(getMatlabRoot());
	}
	
	private void setEnv(EnvVars env) {
	   this.env = env;
	}

	@Symbol("matlab")
	@Extension
	public static final class MatabBuildWrapperDescriptor extends BuildWrapperDescriptor{

		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			
			return true;
		}
		
		@Override
        public String getDisplayName() {
            return "With MATLAB";
        }
		
		public MatlabInstallation[] getInstallations() {
			DescriptorImpl descriptor = Jenkins.getInstanceOrNull().getDescriptorByType(MatlabInstallation.DescriptorImpl.class);
	        if (descriptor == null) {
	            throw new IllegalStateException("Could not find MATLAB Installations");
	        }
	        return descriptor.getInstallations();
        }
		
		
	}

	@Override
	public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener,
			EnvVars initialEnvironment) throws IOException, InterruptedException {
		//Set Environment variable
		setEnv(initialEnvironment);
		
	}
	 
	  
	
}

