package com.attask.jenkins.testreport.dynamicresults;

import com.attask.jenkins.testreport.IncorrectConfigurationException;
import com.attask.jenkins.testreport.TestDataPublisher;
import com.attask.jenkins.testreport.TestRecorder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.DescribableList;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * User: Joel Johnson
 * Date: 2/20/13
 * Time: 11:23 AM
 */
public class DynamicTestResultsBuildWrapper extends BuildWrapper implements MatrixAggregatable {
	@DataBoundConstructor
	public DynamicTestResultsBuildWrapper() {

	}

	@SuppressWarnings("unchecked") //The getPublishersList.get is messing up.
	@Override
	public void preCheckout(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		EnvVars environment = build.getEnvironment(listener);

		TestRecorder recorder = (TestRecorder) build.getProject().getPublishersList().get(TestRecorder.class);
		if(recorder == null) {
			throw new IncorrectConfigurationException("You must enable \"" + TestRecorder.DescriptorImpl.NAME + "\" to use \"" + this.getDescriptor().getDisplayName() + "\"");
		}

		String failuresFilePattern = recorder.getResultsFilePattern();
		String uniqueId = recorder.getUniquifier();

		String failuresFilePatternExpanded = environment.expand(failuresFilePattern);
		String uniqueIdExpanded = environment.expand(uniqueId);

		DescribableList<TestDataPublisher,Descriptor<TestDataPublisher>> testDataPublishers = recorder.getTestDataPublishers();
		List<TestDataPublisher> testDataPublishersList = Collections.emptyList();
		if(testDataPublishers != null) {
			testDataPublishersList = testDataPublishers.toList();
		}
		build.addAction(new DynamicTestResultsAction(build, failuresFilePatternExpanded, uniqueIdExpanded, launcher.isUnix(), testDataPublishersList));
	}

	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
				return true; //Do nothing.
			}
		};
	}

	@Override
	public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
		return new MatrixAggregator(build, launcher, listener) {
			@Override
			public boolean startBuild() throws InterruptedException, IOException {
				preCheckout(this.build, launcher, listener);
				return true;
			}
		};
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {
		@Override
		public String getDisplayName() {
			return "Enable live test results";
		}

		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}
	}
}
