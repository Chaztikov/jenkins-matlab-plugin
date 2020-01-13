package com.mathworks.ci;

import java.io.IOException;

import org.jenkinsci.Symbol;

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
		
		
	}
	 
	  
	
}

