package com.attask.jenkins.testreport;

import hudson.Launcher;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.BuildListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Joel Johnson
 * Date: 1/21/13
 * Time: 7:26 PM
 */
public class TestResultMatrixAggregator extends MatrixAggregator {
	protected TestResultMatrixAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
		super(build, launcher, listener);
	}

	@Override
	public boolean endBuild() throws InterruptedException, IOException {
		List<MatrixRun> runs = build.getRuns();
		if(runs != null && runs.size() > 0) {
			List<TestResult> testResults = new ArrayList<TestResult>();
			String uniquifier = null;
			String url = "testReport";
			for (MatrixRun run : runs) {
				List<TestResultAction> actions = run.getActions(TestResultAction.class);
				if(actions != null) {
					for (TestResultAction action : actions) {
						testResults.addAll(action.getTestResults().values());
						uniquifier = action.getUniquifier();
					}
				}
			}
			if(uniquifier != null) {
				build.addAction(new TestResultAction(build, testResults, uniquifier, url));
			}
		}

		return true;
	}

	private List<TestResult> mergeResults(List<MatrixRun> runs) {
		List<TestResult> testResults = new ArrayList<TestResult>();

		for (MatrixRun run : runs) {
			List<TestResultAction> actions = run.getActions(TestResultAction.class);
			if(actions != null) {
				for (TestResultAction action : actions) {
					testResults.addAll(action.getTestResults().values());
				}
			}
		}
		return testResults;
	}
}
