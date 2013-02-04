package com.attask.jenkins.testreport;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Collection;

/**
 * User: Joel Johnson
 * Date: 2/1/13
 * Time: 4:55 PM
 */
public abstract class TestDataPublisher extends AbstractDescribableImpl<TestDataPublisher> implements ExtensionPoint {
	/**
	 * @return The desired width of the column. The default is '4em', which is pretty small.
	 */
	public String getWidth() {
		return "4em";
	}

	/**
	 * @return The name of the column.
	 */
	public abstract String getDisplayName();

	/**
	 * Is run before any of the rows are rendered.
	 * Determines if the before.jelly should be rendered for this publisher.
	 * @param build The build the test results belong to
	 * @param testResults All the test.
	 * @return True if the before.jelly should be rendered above the table.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public abstract boolean before(AbstractBuild<?, ?> build, Collection<TestResult> testResults) throws IOException, InterruptedException;

	/**
	 * Is run just before an individual row is rendered.
	 * Determines if the cell.jelly should be rendered in the custom column for this row.
	 * Currently is only applied to failures.
	 * @param build The build the give test result belongs to.
	 * @param testResult The test result that is about to be rendered.
	 * @return True if the cell.jelly should be rendered.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public abstract boolean each(AbstractBuild<?, ?> build, TestResult testResult) throws IOException, InterruptedException;

	/**
	 * Is run after all of the rows have been rendered.
	 * Determines if the after.jelly should be rendered for this publisher.
	 * @param build The build the test results belong to.
	 * @param testResults All the test results.
	 * @return True if the before.jelly should be rendered above the table.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public abstract boolean after(AbstractBuild<?, ?> build, Collection<TestResult> testResults) throws IOException, InterruptedException;

	public static DescriptorExtensionList<TestDataPublisher, Descriptor<TestDataPublisher>> all() {
		return Jenkins.getInstance().<TestDataPublisher, Descriptor<TestDataPublisher>>getDescriptorList(TestDataPublisher.class);
	}
}
