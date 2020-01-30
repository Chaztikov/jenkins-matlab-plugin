package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.FormValidation.Kind;
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

	
	@Extension
	public static final class MatabBuildWrapperDescriptor extends BuildWrapperDescriptor {
		
		MatlabReleaseInfo rel;
        String matlabRoot;

        public String getMatlabRoot() {
            return matlabRoot;
        }

        public void setMatlabRoot(String matlabRoot) {
            this.matlabRoot = matlabRoot;
        }

		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {		
			return true;
		}
		
		@Override
        public String getDisplayName() {
            return "With MATLAB";
        }
		
		public String getSelected() {
			return this.matlabRoot;
		}
		
		 /*
         * Below methods with 'doCheck' prefix gets called by jenkins when this builder is loaded.
         * these methods are used to perform basic validation on UI elements associated with this
         * descriptor class.
         */


        public FormValidation doCheckMatlabRoot(@QueryParameter String matlabRoot) {
            setMatlabRoot(matlabRoot);
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            listOfCheckMethods.add(chkMatlabEmpty);
            listOfCheckMethods.add(chkMatlabSupportsRunTests);

            return getFirstErrorOrWarning(listOfCheckMethods);
        }

        public FormValidation getFirstErrorOrWarning(
                List<Function<String, FormValidation>> validations) {
            if (validations == null || validations.isEmpty())
                return FormValidation.ok();
            try {
            	final String matlabRoot = MatlabInstallation.fromName(getMatlabRoot()).getHome();
            	for (Function<String, FormValidation> val : validations) {
                    FormValidation validationResult = val.apply(matlabRoot);
                    if (validationResult.kind.compareTo(Kind.ERROR) == 0
                            || validationResult.kind.compareTo(Kind.WARNING) == 0) {
                        return validationResult;
                    }
                }
            }catch (Exception e) {
            	FormValidation.warning("");
            }
            
            return FormValidation.ok();
        }

        Function<String, FormValidation> chkMatlabEmpty = (String matlabRoot) -> {
            if (matlabRoot.isEmpty()) {
                return FormValidation.error(Message.getValue("Builder.matlab.root.empty.error"));
            }
            return FormValidation.ok();
        };
        
        Function<String, FormValidation> chkMatlabSupportsRunTests = (String matlabRoot) -> {
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if (!resolver.hasVariablePattern()) {
                try {
                    FilePath matlabRootPath = new FilePath(new File(matlabRoot));
                    rel = new MatlabReleaseInfo(matlabRootPath);
                    if (rel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_RUNTESTS_SUPPORT)) {
                        return FormValidation
                                .error(Message.getValue("Builder.matlab.test.support.error"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation
                            .warning(Message.getValue("Builder.invalid.matlab.root.warning"));
                }
            }
            return FormValidation.ok();
        };
		
		public MatlabInstallation[] getInstallations() {
			DescriptorImpl descriptor = Jenkins.getInstanceOrNull().getDescriptorByType(MatlabInstallation.DescriptorImpl.class);
	        if (descriptor == null) {
	            throw new IllegalStateException("Could not find MATLAB Installations");
	        }
	        return descriptor.getInstallations();
        }
		
		public ListBoxModel doFillMatlabRootItems() {
		    ListBoxModel items = new ListBoxModel();
		    for (MatlabInstallation inst : getInstallations()) {
		        items.add(inst.getName(),inst.getName());
		    }
		    items.add("Custom","Custom");
		    
		    return items;
		}
		
		
	}

	@Override
	public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener,
			EnvVars initialEnvironment) throws IOException, InterruptedException {
		//Set Environment variable
		
		setEnv(initialEnvironment);
		String nodeSpecificFileSep = getNodeSpecificFileSeperator(launcher);
		context.env("matlabroot", getLocalMatlab());
		context.env("PATH+matlabroot", getLocalMatlab() + nodeSpecificFileSep + "bin");
		System.out.println("This is in Build Wrapper Env variable "+ initialEnvironment.get("matlabroot"));
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

