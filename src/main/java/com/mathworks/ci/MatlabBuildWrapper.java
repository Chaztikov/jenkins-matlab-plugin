package com.mathworks.ci;

import java.io.IOException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
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
		return this.env == null ? getMatlabInstallationByName(getMatlabRoot()).getHome(): this.env.expand(getMatlabInstallationByName(getMatlabRoot()).getHome());
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
		int a = 0;
		//Set Environment variable
		setEnv(initialEnvironment);
		String nodeSpecificFileSep = getNodeSpecificFileSeperator(launcher);
		//initialEnvironment.put("matlabroot", getLocalMatlab() + nodeSpecificFileSep + "bin");
		launcher.launch().cmdAsSingleString("setenv PATH "+getLocalMatlab() + nodeSpecificFileSep + "bin"+":$PATH").envs(initialEnvironment).join();
		//Update PATH variable 
		initialEnvironment.override("PATH+matlabroot", initialEnvironment.get("matlabroot"));	
		System.out.println("PATH variable after update is"+ initialEnvironment.get("PATH"));
		
		if(launcher.launch().cmdAsSingleString("matlab -batch ver").envs(initialEnvironment) .stdout(listener).join()!=0) {
			System.out.println("Failed to set the System path");
		}else {
			System.out.println("Sytem Path is set ready to launch MATLAB");
			
		}
		
	}
	
	private MatlabInstallation getMatlabInstallationByName(String name) {
		return  MatlabInstallation.fromName(name);
		
	}
	
	private String getNodeSpecificFileSeperator(Launcher launcher) {
	     if (launcher.isUnix()) {
	            return "/";
	        } else {
	            return "\\";
	        }
	}
	  
	
}

