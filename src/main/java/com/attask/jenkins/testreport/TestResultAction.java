package com.attask.jenkins.testreport;

import com.attask.jenkins.testreport.utils.RunUtils;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * User: Joel Johnson
 * Date: 1/19/13
 * Time: 2:06 PM
 */
@ExportedBean
public class TestResultAction extends AbstractTestResultAction {
	private final String buildId;
	private final Map<String, TestResult> testResults;
	private final Map<TestStatus, List<TestResult>> testResultByStatus;

	private final String uniquifier;
	private final String urlName;

	private List<TestDataPublisher> testDataPublishers;

	public TestResultAction(AbstractBuild<?, ?> build, Collection<TestResult> testResults, String uniquifier, String url, List<TestDataPublisher> testDataPublishers) {
		super(build);
		if(build == null) {
			throw new NullPointerException("build");
		}
		if(testResults == null) {
			throw new NullPointerException("testResults");
		}
		if(url == null) {
			throw new NullPointerException("url");
		}

		this.buildId = RunUtils.getRealExternalizableId(build);
		this.testResults = new HashMap<String, TestResult>(testResults.size());
		this.testResultByStatus = new HashMap<TestStatus, List<TestResult>>();
		for (TestResult testResult : testResults) {
			this.testResults.put(testResult.getName(), testResult);

			List<TestResult> testResultList = this.testResultByStatus.get(testResult.getStatus());
			if(testResultList == null) {
				testResultList = new ArrayList<TestResult>();
				this.testResultByStatus.put(testResult.getStatus(), testResultList);
			}
			testResultList.add(testResult);
		}
		for (List<TestResult> results : this.testResultByStatus.values()) {
			Collections.sort(results);
		}
		this.uniquifier = uniquifier;
		this.urlName = url;
		this.testDataPublishers = testDataPublishers;
	}

	@SuppressWarnings("UnusedDeclaration") //used in index.jelly
	public Run findBuild() {
		if(buildId.contains("$$")) {
			String[] ids = buildId.split("\\$\\$", 2);
			if(ids.length == 2) {
				String parentMatrixId = ids[0];
				String childMatrixId = ids[1];

				Run<?, ?> run = Run.fromExternalizableId(parentMatrixId);
				if(run != null) {
					if(run instanceof MatrixBuild) {
						List<MatrixRun> runs = ((MatrixBuild) run).getRuns();
						for (MatrixRun matrixRun : runs) {
							if(matrixRun.getExternalizableId().equals(childMatrixId)) {
								return matrixRun;
							}
						}
					}
				}
			}
		}
		return Run.fromExternalizableId(buildId);
	}

	public void doGetStackTrace(StaplerRequest request, StaplerResponse response) throws IOException {
		String name = request.getParameter("name");
		TestResult testResult = testResults.get(name);
		ServletOutputStream outputStream = response.getOutputStream();
		String stackTrace = testResult.htmlifyStackTrace();
		outputStream.print(stackTrace);
		outputStream.flush();
	}

	public String findStatusUrl(Run build) {
		Result result = build.getResult();
		if(result == null) {
			result = Result.NOT_BUILT;
		}
		String imageUrl = result.color.getImageOf("48x48");
		if(build.isBuilding()) {
			imageUrl = imageUrl.replace(".png", "_anime.gif");
		}
		return imageUrl;
	}

	@Exported
	public String getBuildId() {
		return buildId;
	}

	@Exported
	public Map<String, TestResult> getTestResults() {
		return testResults;
	}

	@Exported
	public List<TestResult> getFailures() {
		List<TestResult> allFailures = new ArrayList<TestResult>();

		List<TestResult> failures = testResultByStatus.get(TestStatus.FAILED);
		if(failures != null) {
			allFailures.addAll(failures);
		}

		List<TestResult> addedTests = testResultByStatus.get(TestStatus.ADDED);
		if (addedTests != null) {
			allFailures.addAll(addedTests);
		}

		List<TestResult> startedTests = testResultByStatus.get(TestStatus.STARTED);
		if (startedTests != null) {
			allFailures.addAll(startedTests);
		}

		return allFailures;
	}

	public int getAddedSize() {
		if(testResultByStatus.get(TestStatus.ADDED) != null) {
			return testResultByStatus.get(TestStatus.ADDED).size();
		} else {
			return 0;
		}
	}

	public int getFinishedSize() {
		if(testResultByStatus.get(TestStatus.FINISHED) != null) {
			return testResultByStatus.get(TestStatus.FINISHED).size();
		} else {
			return 0;
		}
	}

	public int getStartedSize() {
		if(testResultByStatus.get(TestStatus.STARTED) != null) {
			return testResultByStatus.get(TestStatus.STARTED).size();
		} else {
			return 0;
		}
	}

	public Collection<TestResult> findAllResults() {
		TreeSet<TestResult> result = new TreeSet<TestResult>(new Comparator<TestResult>() {
			public int compare(TestResult r1, TestResult r2) {
				return r1.getName().compareTo(r2.getName());
			}
		});
		for (Collection<TestResult> testResults : testResultByStatus.values()) {
			result.addAll(testResults);
		}
		return result;
	}

	@Override
	public Object getResult() {
		return this;
	}

	@Override
	public int getFailCount() {
		if(testResultByStatus.get(TestStatus.FAILED) != null) {
			return testResultByStatus.get(TestStatus.FAILED).size();
		} else {
			return 0;
		}
	}

	@Override
	public int getTotalCount() {
		return testResults.size();
	}

	@Override
	public int getSkipCount() {
		List<TestResult> skipped = testResultByStatus.get(TestStatus.SKIPPED);
		return skipped == null ? 0 :skipped.size();
	}

	@Exported
	public List<TestDataPublisher> getTestDataPublishers() {
		return testDataPublishers;
	}

	@Override
	public AbstractTestResultAction getPreviousResult() {
		Run build = findBuild();
		if(build == null) {
			return null;
		}

		Run previousBuild = build;
		while((previousBuild = previousBuild.getPreviousBuild()) != null) {
			TestResultAction action = previousBuild.getAction(TestResultAction.class);
			if(action != null) {
				return action;
			}
		}

		return null;
	}

	@Exported
	public String getUniquifier() {
		return uniquifier;
	}

	@Override
	public String getIconFileName() {
		return "clipboard.png";
	}

	@Override
	public String getDisplayName() {
		return "Test Result";
	}

	@Exported
	@Override
	public String getUrlName() {
		return urlName;
	}
}
