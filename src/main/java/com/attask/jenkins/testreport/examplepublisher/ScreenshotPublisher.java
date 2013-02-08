package com.attask.jenkins.testreport.examplepublisher;

import com.attask.jenkins.testreport.TestDataPublisher;
import com.attask.jenkins.testreport.TestResult;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Associates tests with screenshots (if available).
 * If the the test matches the user-defined regular expression,
 * 	then a column is added to the test failure table with a link to that image and also the image will be shown on the test result page under the test's stacktrace.
 *
 *
 * User: Joel Johnson
 * Date: 2/4/13
 * Time: 9:57 AM
 */
public class ScreenshotPublisher extends TestDataPublisher {
	private final String pattern;

	private transient String currentUrl;

	@DataBoundConstructor
	public ScreenshotPublisher(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public String getDisplayName() {
		return "Screenshot";
	}

	@Override
	public boolean before(AbstractBuild<?, ?> build, Collection<TestResult> testResults) throws IOException, InterruptedException {
		return false;
	}

	@Override
	public boolean each(AbstractBuild<?, ?> build, TestResult testResult) throws IOException, InterruptedException {
		EnvVars envVars = new EnvVars();
		envVars.put("TEST_NAME", Pattern.quote(testResult.getName().replace("#", ".")));
		String pattern = envVars.expand(this.pattern);

		Pattern compiledPattern = Pattern.compile(pattern);

		Run run = testResult.findRun();

		@SuppressWarnings("unchecked")
		List<Run.Artifact> artifacts = run.getArtifacts();

		String url = null;
		if(artifacts != null) {
			for (Run.Artifact artifact : artifacts) {
				String displayPath = artifact.getDisplayPath();
				if(displayPath != null) {
					if(compiledPattern.matcher(displayPath.replace("#", ".")).matches()) {
						url = artifact.getHref();
						break;
					}
				}
			}
		}
		if(url == null) {
			return false;
		}
		currentUrl = Jenkins.getInstance().getRootUrl() + run.getUrl() + "artifact/" + url;
		return true;
	}

	@Override
	public boolean after(AbstractBuild<?, ?> build, Collection<TestResult> testResults) throws IOException, InterruptedException {
		return false;
	}

	public String getPattern() {
		return pattern;
	}

	public String getCurrentUrl() {
		return currentUrl;
	}

	@Override
	public boolean includeFloat(AbstractBuild<?, ?> build, TestResult testResult) {
		return false;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
		@Override
		public String getDisplayName() {
			return "Screenshots";
		}
	}
}
