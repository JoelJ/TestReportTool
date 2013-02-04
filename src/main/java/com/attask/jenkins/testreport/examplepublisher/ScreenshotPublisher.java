package com.attask.jenkins.testreport.examplepublisher;

import com.attask.jenkins.testreport.TestDataPublisher;
import com.attask.jenkins.testreport.TestResult;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;

/**
 * User: Joel Johnson
 * Date: 2/4/13
 * Time: 9:57 AM
 */
public class ScreenshotPublisher extends TestDataPublisher {
	@DataBoundConstructor
	public ScreenshotPublisher() {

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
		return new Random().nextBoolean();
	}

	@Override
	public boolean after(AbstractBuild<?, ?> build, Collection<TestResult> testResults) throws IOException, InterruptedException {
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
