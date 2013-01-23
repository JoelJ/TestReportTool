package com.attask.jenkins.testreport;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.CaseResult;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * User: Joel Johnson
 * Date: 1/19/13
 * Time: 1:50 PM
 */
@ExportedBean
public class TestRecorder extends Recorder implements MatrixAggregatable {
	private final String resultsFilePattern;
	private final String uniquifier;
	private final String url;

	@DataBoundConstructor
	public TestRecorder(String resultsFilePattern, String uniquifier, String url) {
		this.resultsFilePattern = resultsFilePattern;
		this.uniquifier = uniquifier;
		this.url = url;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		EnvVars environment = build.getEnvironment(listener);
		String expandedResultsFilePattern = environment.expand(resultsFilePattern);
		String[] includedFiles = findResultsArtifacts(build, launcher, expandedResultsFilePattern);
		if(includedFiles == null || includedFiles.length <= 0) {
			listener.getLogger().println("No files matched " + expandedResultsFilePattern + " in the workspace.");
			return false;
		}

		FilePath workspace = build.getWorkspace();
		String expandedUniquifier = environment.expand(uniquifier);
		String expandedUrl = environment.expand(url);
		LinkedList<TestResult> results = new LinkedList<TestResult>();
		for (String includedFile : includedFiles) {
			listener.getLogger().println("Parsing: " + includedFile);
			Collection<TestResult> testResults = TestResult.parse(new FilePath(workspace, includedFile), build, expandedUniquifier, expandedUrl);
			listener.getLogger().println("\t - contained " + testResults.size() + " results.");
			results.addAll(testResults);
		}


		TestResultAction resultAction = new TestResultAction(build, results, expandedUniquifier, expandedUrl);
		build.addAction(resultAction);

		if(resultAction.getFailCount() > 0) {
			build.setResult(Result.UNSTABLE);
		}

		return true;
	}

	/**
	 * Called by Jenkins. Used to aggregate Matrix results into one result.
	 */
	@Override
	public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
		return new TestResultMatrixAggregator(build, launcher, listener);
	}

	private String[] findResultsArtifacts(AbstractBuild<?, ?> build, Launcher launcher, String resultsFilePattern) throws IOException, InterruptedException {
		return build.getWorkspace().act(new WorkspaceIteratorCallable(resultsFilePattern, launcher.isUnix()));
	}

	@Exported
	public String getResultsFilePattern() {
		return resultsFilePattern;
	}

	@Exported
	public String getUniquifier() {
		return uniquifier;
	}

	@Exported
	public String getUrl() {
		return url;
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Publish AtTask Test Results";
		}
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	private static class WorkspaceIteratorCallable implements FilePath.FileCallable<String[]> {
		private final String resultsFilePattern;
		private final boolean unix;

		public WorkspaceIteratorCallable(String resultsFilePattern, boolean unix) {
			this.resultsFilePattern = resultsFilePattern;
			this.unix = unix;
		}

		public String[] invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
			DirectoryScanner directoryScanner = new DirectoryScanner();
			directoryScanner.setIncludes(resultsFilePattern.split(","));
			directoryScanner.setBasedir(f);
			directoryScanner.setCaseSensitive(unix);
			directoryScanner.setFollowSymlinks(true);
			directoryScanner.setErrorOnMissingDir(false);
			directoryScanner.scan();
			return directoryScanner.getIncludedFiles();
		}
	}
}
