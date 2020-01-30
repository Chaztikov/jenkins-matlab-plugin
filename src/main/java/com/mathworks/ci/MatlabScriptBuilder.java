package com.mathworks.ci;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;


import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class MatlabScriptBuilder extends Builder implements SimpleBuildStep {
	private int buildResult;
	private EnvVars env;
	private MatlabReleaseInfo matlabRel;
	private String customMatlabCommand;

	@DataBoundConstructor
	public MatlabScriptBuilder() {
	}

	// Getter and Setters to access local members

	@DataBoundSetter
	public void setCustomMatlabCommand(String customMatlabCommand) {
		this.customMatlabCommand = customMatlabCommand;
	}

	public String getCustomMatlabCommand() {
		return this.env == null ? this.customMatlabCommand :  this.env.expand(this.customMatlabCommand);
	}

	private void setEnv(EnvVars env) {
		this.env = env;
	}

	@Symbol("runmatlab")
	@Extension
	public static class MatlabScriptDescriptor extends BuildStepDescriptor<Builder> {

		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "";//Message.getValue("Builder.matlab.script.builder.display.name");
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}

	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		// Set the environment variable specific to the this build
		setEnv(run.getEnvironment(listener));
		// Get node specific matlabroot to get matlab version information
		FilePath nodeSpecificMatlabRoot = new FilePath(launcher.getChannel(), this.env.get("matlabroot"));
		matlabRel = new MatlabReleaseInfo(nodeSpecificMatlabRoot);

		// Invoke MATLAB command and transfer output to standard
		// Output Console

		buildResult = execMatlabCommand(workspace, launcher, listener);

		if (buildResult != 0) {
			run.setResult(Result.FAILURE);
		}

	}

	private synchronized int execMatlabCommand(FilePath workspace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {
		ProcStarter matlabLauncher;
		try {
			matlabLauncher = launcher.launch().pwd(workspace).envs(this.env);
			if (matlabRel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_BATCH_SUPPORT)) {
				ListenerLogDecorator outStream = new ListenerLogDecorator(listener);
				matlabLauncher = matlabLauncher.cmds(constructDefaultMatlabCommand(launcher.isUnix()))
						.stderr(outStream);
			} else {
				matlabLauncher = matlabLauncher.cmds(constructMatlabCommandWithBatch(launcher.isUnix())).stdout(listener);
			}
		} catch (Exception e) {
			listener.getLogger().println(e.getMessage());
			return 1;
		}
		return matlabLauncher.join();
	}

	public List<String> constructMatlabCommandWithBatch(boolean isLinuxLauncher) {
		final List<String> matlabDefaultArgs;
		if(isLinuxLauncher) {
			matlabDefaultArgs = Arrays.asList("/bin/bash", "-c", "matlab -batch "+getCustomMatlabCommand());
		} else {
			matlabDefaultArgs = Arrays.asList("cmd.exe","/C", "matlab", "-batch",getCustomMatlabCommand());
		}
		return matlabDefaultArgs;
	}

	public List<String> constructDefaultMatlabCommand(boolean isLinuxLauncher) throws MatlabVersionNotFoundException {
		final List<String> matlabDefaultArgs = new ArrayList<String>();
		Collections.addAll(matlabDefaultArgs, getPreRunnerSwitches(isLinuxLauncher));
		if (!isLinuxLauncher) {
			matlabDefaultArgs.add("-noDisplayDesktop");
		}
		Collections.addAll(matlabDefaultArgs, getRunnerSwitch());
		if (!isLinuxLauncher) {
			matlabDefaultArgs.add("-wait");
		}
		Collections.addAll(matlabDefaultArgs, getPostRunnerSwitches());
		return matlabDefaultArgs;
	}

	private String[] getPreRunnerSwitches(boolean isLinuxLauncher) throws MatlabVersionNotFoundException {
    	String[] defaultRunnerSwitches = {"matlab", "-nosplash","-nodesktop"};
    	String[] preRunnerSwitches;
    	if(isLinuxLauncher) {
    		String[] commandRunner = {"/bin/bash","-c"};
    		preRunnerSwitches =  (String[]) ArrayUtils.add(commandRunner, defaultRunnerSwitches);
    	}else {
    		String[] commandRunner = {"cmd.exe","/C","matlab"};
        	preRunnerSwitches =  (String[]) ArrayUtils.add(commandRunner, defaultRunnerSwitches);
    	}
    	
        if(!matlabRel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_NO_APP_ICON_SUPPORT)) {
        	preRunnerSwitches =  (String[]) ArrayUtils.add(preRunnerSwitches, "-noAppIcon");
        } 
        return preRunnerSwitches;
    }

	private String[] getPostRunnerSwitches() {
		String[] postRunnerSwitch = { "-log" };
		return postRunnerSwitch;
	}

	private String[] getRunnerSwitch() {
		final String runCommand;
		runCommand = "try,eval('" + getCustomMatlabCommand().replaceAll("'", "''")
				+ "'),catch e,disp(getReport(e,'extended')),exit(1),end,exit";

		final String[] runnerSwitch = { "-r", runCommand };
		return runnerSwitch;
	}

}
