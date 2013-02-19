package com.attask.jenkins.testreport;

/**
 * User: Joel Johnson
 * Date: 1/19/13
 * Time: 2:14 PM
 */
public enum TestStatus {
	// These are in order of importance.
	ADDED,
	STARTED,
	SKIPPED,
	FAILED,
	FINISHED;

	public boolean isMoreInterestingThan(TestStatus status) {
		if(status == null) {
			throw new NullPointerException("status");
		}

		return this.ordinal() > status.ordinal();
	}
}
