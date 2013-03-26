package com.attask.jenkins.testreport.dynamicresults;

import com.attask.jenkins.testreport.*;
import com.attask.jenkins.testreport.utils.RunUtils;
import hudson.FilePath;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * User: Joel Johnson
 * Date: 2/20/13
 * Time: 11:22 AM
 */
public class DynamicTestResultsAction extends AbstractTestResultAction {
	private static final Logger LOGGER = Logger.getLogger("TestReportTool");
	private static final int SECONDS = 1000;

	private final String buildId;
	private final String failuresFilePattern;
	private final String uniqueId;
	private final boolean unix;

	private transient TestResultAction cachedTestResultsAction = null;
	private transient long cacheCreateTime = 0;
	private final List<TestDataPublisher> testDataPublishers;

	public DynamicTestResultsAction(AbstractBuild owner, String failuresFile, String uniqueId, boolean isUnix, List<TestDataPublisher> testDataPublishers) throws IOException {
		super(owner);
		this.testDataPublishers = testDataPublishers;
		this.buildId = RunUtils.getRealExternalizableId(owner);
		this.failuresFilePattern = failuresFile;
		this.uniqueId = uniqueId;
		this.unix = isUnix;
	}

	/**
	 * Creates and caches a TestResultAction to delegate the UI to.
	 */
	public TestResultAction createDelegatedAction() throws IOException, InterruptedException {
		if((System.currentTimeMillis() - cacheCreateTime) > (10 * SECONDS)) {
			cachedTestResultsAction = null;
		}

		if(cachedTestResultsAction != null) {
			return cachedTestResultsAction;
		}

		Run run = RunUtils.findRun(buildId);
		if(run == null) {
			LOGGER.warning("No run for id: " + buildId);
			throw new NullPointerException("there was no run for id: " + buildId);
		}
		assert run instanceof AbstractBuild : "the run should be an abstract build since that is what was passed into the constructor.";

		return createDelegatedAction((AbstractBuild) run);
	}

	private TestResultAction createDelegatedAction(AbstractBuild abstractBuild) throws IOException, InterruptedException {
		cachedTestResultsAction = new TestResultAction(abstractBuild, findTestResults(), uniqueId, getUrlName(), this.getTestDataPublishers());
		cacheCreateTime = System.currentTimeMillis();
		return cachedTestResultsAction;
	}

	public List<TestResult> findTestResults() throws IOException, InterruptedException {
		Run run = RunUtils.findRun(buildId);
		if(run == null) {
			throw new NullPointerException("there was no run for id: " + buildId);
		}
		assert run instanceof AbstractBuild : "the run should be an abstract build since that is what was passed into the constructor.";

		AbstractBuild abstractBuild = (AbstractBuild)run;
		List<AbstractBuild> builds = new LinkedList<AbstractBuild>();

		if(abstractBuild instanceof MatrixBuild) {
			List<MatrixRun> runs = ((MatrixBuild) abstractBuild).getRuns();
			for (MatrixRun matrixRun : runs) {
				if(matrixRun.getNumber() == abstractBuild.getNumber()) {
					builds.add(matrixRun);
				}
			}
		} else {
			builds.add(abstractBuild);
		}

		List<TestResult> result = new LinkedList<TestResult>();
		for (AbstractBuild build : builds) {
			FilePath workspace = build.getWorkspace();

			String[] files = workspace.act(new TestRecorder.WorkspaceIteratorCallable(failuresFilePattern, unix));
			for (String file : files) {
				FilePath resultFile = new FilePath(workspace, file);
				Collection<TestResult> parsed = TestResult.parse(resultFile, run, uniqueId, getUrlName());
				result.addAll(parsed);
			}
		}
		return Collections.unmodifiableList(result);
	}

	public void doGetStackTrace(StaplerRequest request, StaplerResponse response) throws IOException, InterruptedException {
		createDelegatedAction().doGetStackTrace(request, response);
	}

	@Override
	public String getIconFileName() {
		return "clipboard.png";
	}

	@Override
	public Object getResult() {
		return this;
	}

	@Override
	public String getDisplayName() {
		return "Running Test Report";
	}

	@Exported(visibility = 2)
	@Override
	public String getUrlName() {
		return "testReport";
	}

	@Exported
	public String getBuildId() {
		return buildId;
	}

	@Exported
	public String getFailuresFilePattern() {
		return failuresFilePattern;
	}

	@Exported
	public String getUniqueId() {
		return uniqueId;
	}

	@Exported
	public boolean getUnix() {
		return unix;
	}

	public List<TestDataPublisher> getTestDataPublishers() {
		return testDataPublishers;
	}

	@Exported
	public Map<String, TestResult> getTestResults() throws IOException, InterruptedException {
		return createDelegatedAction().getTestResults();
	}

	@Exported
	public List<TestResult> getFailures() throws IOException, InterruptedException {
		return createDelegatedAction().getFailures();
	}

	@Exported
	public Collection<TestResult> findAllResults() throws IOException, InterruptedException {
		return createDelegatedAction().findAllResults();
	}

	@Exported
	public int getFailCount() {
		try {
			return createDelegatedAction().getFailCount();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Exported
	public int getTotalCount() {
		try {
			return createDelegatedAction().getTotalCount();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Exported
	public int getSkipCount() {
		try {
			return createDelegatedAction().getSkipCount();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Exported
	public String getUniquifier() throws IOException, InterruptedException {
		return createDelegatedAction().getUniquifier();
	}
}
