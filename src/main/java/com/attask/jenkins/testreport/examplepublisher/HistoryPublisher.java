package com.attask.jenkins.testreport.examplepublisher;

import com.attask.jenkins.testreport.TestDataPublisher;
import com.attask.jenkins.testreport.TestResult;
import com.attask.jenkins.testreport.TestResultAction;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Run;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: Joel Johnson
 * Date: 2/8/13
 * Time: 11:51 AM
 */
public class HistoryPublisher extends TestDataPublisher {
	public List<TestResult> history;
	public int maxTime;

	@DataBoundConstructor
	public HistoryPublisher() {

	}

	@Override
	public String getDisplayName() {
		return "History";
	}

	@Override
	public boolean includeFloat(AbstractBuild<?, ?> build, TestResult testResult) {
		int historyCount = 10;
		StaplerRequest currentRequest = Stapler.getCurrentRequest();
		if(currentRequest != null) {
			String historyCountParam = currentRequest.getParameter("historyCount");
			if(historyCountParam != null && !historyCountParam.isEmpty()) {
				historyCount = Integer.parseInt(historyCountParam);
			}
		}

		populateHistory(build, testResult, historyCount);

		return true;
	}

	@Override
	public boolean includeSummary(AbstractBuild<?, ?> build, TestResult testResult) {
		return false;
	}

	@Override
	public boolean before(AbstractBuild<?, ?> build, Collection<TestResult> testResults) throws IOException, InterruptedException {
		return false;
	}

	@Override
	public boolean each(AbstractBuild<?, ?> build, TestResult testResult) throws IOException, InterruptedException {
		StaplerRequest currentRequest = Stapler.getCurrentRequest();
		if(currentRequest != null) {
			if(currentRequest.getParameter("name") != null) {
				return true;
			}
		}

		populateHistory(build, testResult, 5);
		return true;
	}

	@Override
	public boolean after(AbstractBuild<?, ?> build, Collection<TestResult> testResults) throws IOException, InterruptedException {
		return false;
	}

	private void populateHistory(AbstractBuild<?, ?> build, TestResult testResult, int historyCount) {
		List<TestResult> history = new ArrayList<TestResult>(historyCount);
		Run next = build;
		int maxTime = Integer.MIN_VALUE;
		while(historyCount > 0 && next != null) {
			TestResultAction action = next.getAction(TestResultAction.class);
			if(action != null) {
				TestResult oldTestResult = action.getTestResults().get(testResult.getName());
				if(oldTestResult != null && oldTestResult.getName().equals(testResult.getName())) {
					historyCount--;
					history.add(0, oldTestResult);
					maxTime = Math.max(maxTime, oldTestResult.getTime());
				}
			}
			next = next.getPreviousBuild();
		}
		this.history = history;
		this.maxTime = maxTime;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
		@Override
		public String getDisplayName() {
			return "History";
		}
	}
}
