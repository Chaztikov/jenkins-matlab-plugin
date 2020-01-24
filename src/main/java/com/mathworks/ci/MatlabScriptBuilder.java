package com.mathworks.ci;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import com.mathworks.ci.MatlabBuilder.TestRunTypeList;

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
	private String matlabRoot;
	private EnvVars env;
	private MatlabReleaseInfo matlabRel;
	private String nodeSpecificfileSeparator;
	private String customMatlabCommand;

	@DataBoundConstructor
	public MatlabScriptBuilder() {
	}

	// Getter and Setters to access local members

	@DataBoundSetter
	public void setMatlabRoot(String matlabRoot) {
		this.matlabRoot = matlabRoot;
	}
	
	@DataBoundSetter
	public void setCustomMatlabCommand(String customMatlabCommand) {
		this.customMatlabCommand = customMatlabCommand;
	}

	public String getMatlabRoot() {

		return this.matlabRoot;
	}


	private String getLocalMatlab() {
		return this.env == null ? getMatlabRoot() : this.env.expand(getMatlabRoot());
	}

	private String getCustomMatlabCommand() {
		return this.env == null ? this.customMatlabCommand :  this.env.expand(this.customMatlabCommand);
	}

	private void setEnv(EnvVars env) {
		this.env = env;
	}

	@Symbol("runmatlab")
	@Extension
	public static class MatlabScriptDescriptor extends BuildStepDescriptor<Builder> {

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Message.getValue("Builder.matlab.script.builder.display.name");
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
		FilePath nodeSpecificMatlabRoot = new FilePath(launcher.getChannel(), getLocalMatlab());
		matlabRel = new MatlabReleaseInfo(nodeSpecificMatlabRoot);
		nodeSpecificfileSeparator = getNodeSpecificFileSeperator(launcher);

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
				matlabLauncher = matlabLauncher.cmds(constructMatlabCommandWithBatch()).stdout(listener);
			}
		} catch (Exception e) {
			listener.getLogger().println(e.getMessage());
			return 1;
		}
		return matlabLauncher.join();
	}

	public List<String> constructMatlabCommandWithBatch() {
		final String runCommand;
		final List<String> matlabDefaultArgs;
		
		runCommand = getCustomMatlabCommand();

		matlabDefaultArgs = Arrays.asList(
				getLocalMatlab() + nodeSpecificfileSeparator + "bin" + nodeSpecificfileSeparator + "matlab", "-batch",
				runCommand);

		return matlabDefaultArgs;
	}

	public List<String> constructDefaultMatlabCommand(boolean isLinuxLauncher) throws MatlabVersionNotFoundException {
		final List<String> matlabDefaultArgs = new ArrayList<String>();
		Collections.addAll(matlabDefaultArgs, getPreRunnerSwitches());
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

	private String[] getPreRunnerSwitches() throws MatlabVersionNotFoundException {
		String[] preRunnerSwitches = {
				getLocalMatlab() + nodeSpecificfileSeparator + "bin" + nodeSpecificfileSeparator + "matlab",
				"-nosplash", "-nodesktop" };
		if (!matlabRel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_NO_APP_ICON_SUPPORT)) {
			preRunnerSwitches = (String[]) ArrayUtils.add(preRunnerSwitches, "-noAppIcon");
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

	private String getNodeSpecificFileSeperator(Launcher launcher) {
		if (launcher.isUnix()) {
			return "/";
		} else {
			return "\\";
		}
	}

}
