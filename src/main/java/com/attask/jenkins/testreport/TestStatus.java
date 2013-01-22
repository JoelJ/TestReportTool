package com.attask.jenkins.testreport;

/**
 * User: Joel Johnson
 * Date: 1/19/13
 * Time: 2:14 PM
 */
public enum TestStatus {
	ADDED, STARTED, FINISHED, SKIPPED, FAILED;

	public boolean isMoreInterestingThan(TestStatus status) {
		if(status == null) {
			throw new NullPointerException("status");
		}

		return this.ordinal() > status.ordinal();
	}
}
