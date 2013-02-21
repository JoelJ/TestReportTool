package com.attask.jenkins.testreport;

import com.attask.jenkins.testreport.dynamicresults.DynamicTestResultsAction;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * User: Joel Johnson
 * Date: 1/19/13
 * Time: 1:50 PM
 */
@ExportedBean
public class TestRecorder extends Recorder implements MatrixAggregatable {
	private static final Logger log = Logger.getLogger(TestRecorder.class.getCanonicalName());
	private final String resultsFilePattern;
	private final String uniquifier;
	private transient final String url = "testReport";
	private final DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers;

	@DataBoundConstructor
	public TestRecorder(String resultsFilePattern, String uniquifier, DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers) {
		//This constructor isn't automatically bound. It's manually bound in the DescriptorImpl class
		this.resultsFilePattern = resultsFilePattern;
		this.uniquifier = uniquifier;
		this.testDataPublishers = testDataPublishers;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		EnvVars environment = build.getEnvironment(listener);
		String expandedResultsFilePattern = environment.expand(resultsFilePattern);
		String[] includedFiles = findResultsArtifacts(build, launcher, expandedResultsFilePattern, listener);
		if(includedFiles == null || includedFiles.length <= 0) {
			listener.getLogger().println("No files matched " + expandedResultsFilePattern + " in the workspace.");
			return false;
		}

		FilePath workspace = build.getWorkspace();
		String expandedUniquifier = environment.expand(uniquifier);
		LinkedList<TestResult> results = new LinkedList<TestResult>();
		for (String includedFile : includedFiles) {
			listener.getLogger().println("Parsing: " + includedFile);
			Collection<TestResult> testResults = TestResult.parse(new FilePath(workspace, includedFile), build, expandedUniquifier, url);
			listener.getLogger().println("\t - contained " + testResults.size() + " results.");
			results.addAll(testResults);
		}

		List<TestDataPublisher> testDataPublisherList = new ArrayList<TestDataPublisher>(testDataPublishers.size());
		for (TestDataPublisher testDataPublisher : testDataPublishers) {
			testDataPublisherList.add(testDataPublisher);
		}

		TestResultAction resultAction = new TestResultAction(build, results, expandedUniquifier, url, testDataPublisherList);

		if(resultAction.getFailCount() > 0) {
			build.setResult(Result.UNSTABLE);
		}

		List<DynamicTestResultsAction> oldTestResultActions = build.getActions(DynamicTestResultsAction.class);
		if(oldTestResultActions != null && !oldTestResultActions.isEmpty()) {
			build.getActions().removeAll(oldTestResultActions);
		}
		build.addAction(resultAction);
		return true;
	}

	/**
	 * Called by Jenkins. Used to aggregate Matrix results into one result.
	 */
	@Override
	public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
		List<TestDataPublisher> testDataPublisherList = Collections.emptyList();
		if(testDataPublishers != null) {
			testDataPublisherList = new ArrayList<TestDataPublisher>(testDataPublishers.size());
			for (TestDataPublisher testDataPublisher : testDataPublishers) {
				testDataPublisherList.add(testDataPublisher);
			}
		}
		return new TestResultMatrixAggregator(build, launcher, listener, testDataPublisherList);
	}

	private String[] findResultsArtifacts(AbstractBuild<?, ?> build, Launcher launcher, String resultsFilePattern, BuildListener listener) throws IOException, InterruptedException {
		FilePath workspace = build.getWorkspace();
		if(workspace == null) {
			String errorMessage = "There was no workspace! Was the machine torn down?";
			log.warning(errorMessage + " " + build.getFullDisplayName());
			listener.error(errorMessage);
			return new String[0];
		}
		return workspace.act(new WorkspaceIteratorCallable(resultsFilePattern, launcher.isUnix()));
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

	@Exported
	public DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> getTestDataPublishers() {
		return testDataPublishers;
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public static final String NAME = "Publish AtTask Test Results";

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return NAME;
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
			String resultsFilePattern = formData.getString("resultsFilePattern");
			String uniquifier = formData.getString("uniquifier");

			DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers = new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(Saveable.NOOP);
			try {
				testDataPublishers.rebuild(req, formData, TestDataPublisher.all());
			} catch (IOException e) {
				throw new FormException(e,null);
			}

			return new TestRecorder(resultsFilePattern, uniquifier, testDataPublishers);
		}

	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	public static class WorkspaceIteratorCallable implements FilePath.FileCallable<String[]> {
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
