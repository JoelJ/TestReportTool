package com.attask.jenkins.testreport;

import hudson.FilePath;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * User: Joel Johnson
 * Date: 1/19/13
 * Time: 2:09 PM
 */
@ExportedBean
public class TestResult implements Comparable<TestResult> {
	private final String name;
	private final int time;
	private final String threadId;
	private final TestStatus status;
	private final String runId;
	private final String stackTrace;
	private final int age;
	private final String firstFailingBuildId;
	private final String url;

	public TestResult(String name, int time, String threadId, TestStatus status, String runId, String stackTrace, int age, String firstFailingBuildId, String url) {
		this.name = name;
		this.time = time;
		this.threadId = threadId;
		this.status = status;
		this.runId = runId;
		this.stackTrace = stackTrace;
		this.age = age;
		this.firstFailingBuildId = firstFailingBuildId;
		this.url = url;
	}

	@Exported
	public String getName() {
		return name;
	}

	@Exported
	public String getNameUrlEncoded() {
		return name.replace("#", "%23");
	}

	@Exported
	public int getTime() {
		return time;
	}

	@Exported
	public String getThreadId() {
		return threadId;
	}

	@Exported
	public TestStatus getStatus() {
		return status;
	}

	@Exported
	public String getRunId() {
		return runId;
	}

	@Exported
	public String getStackTrace() {
		return stackTrace;
	}

	@Exported
	public int getAge() {
		return age;
	}

	@Exported
	public String getFirstFailingBuildId() {
		return firstFailingBuildId;
	}

	@Exported
	public String getUrl() {
		return url;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String findFirstFailureUrl() {
		String firstFailingBuildId = getFirstFailingBuildId();
		if(firstFailingBuildId != null) {
			if(firstFailingBuildId .contains("$$slice=")) {
				String matrixBuild = findMatrixBuildUrl(firstFailingBuildId);
				if(matrixBuild != null) {
					return matrixBuild;
				}
			}
			Run<?, ?> firstFailingBuild = Run.fromExternalizableId(firstFailingBuildId);
			if(firstFailingBuild != null) {
				return firstFailingBuild.getUrl();
			}
		}
		return null;
	}

	private String findMatrixBuildUrl(String matrixId) {
		String[] ids = matrixId.split("\\$\\$", 2);
		if(ids.length != 2) {
			return null;
		}

		String parentMatrixId = ids[0];
		String childMatrixId = ids[1];

		Run<?, ?> run = Run.fromExternalizableId(parentMatrixId);
		if(run != null) {
			if(run instanceof MatrixBuild) {
				List<MatrixRun> runs = ((MatrixBuild) run).getRuns();
				for (MatrixRun matrixRun : runs) {
					if(matrixRun.getExternalizableId().equals(childMatrixId)) {
						return matrixRun.getUrl();
					}
				}
			}
		}

		return null;
	}

	public static Collection<TestResult> parse(FilePath file, AbstractBuild build, String uniqueId, String url) throws IOException, IllegalFormatException {
		Map<TestResult, TestStatus> results = new HashMap<TestResult, TestStatus>();

		InputStream fileStream = file.read();
		try {
			Scanner scanner = new Scanner(fileStream);
			try {
				if(scanner.hasNextLine()) {
					String header = scanner.nextLine();
					if(!"AtTask Failures v2".equals(header)) {
						throw new IllegalFailureFileFormatException(file, 1, "Illegal Header: `" + header + "`");
					}
				}
				int lineNumber = 0;
				while(scanner.hasNextLine()) {
					lineNumber++; //increment first since we already read the header.
					String line = scanner.nextLine();
					parseLine(file, build, uniqueId, results, scanner, lineNumber, line, url);
				}
			} finally {
				scanner.close();
			}
		} finally {
			fileStream.close();
		}

		return results.keySet();
	}

	private static void parseLine(FilePath file, AbstractBuild build, String uniqueId, Map<TestResult, TestStatus> results, Scanner scanner, int lineNumber, String line, String url) {
		if(line == null || line.trim().isEmpty()) {
			return;
		}

		int i = line.indexOf(" ");
		if(i < 0) {
			throw new IllegalFailureFileFormatException(file, lineNumber, "Not whitespace separated: " + line);
		}
		String statusString = line.substring(0, i);

		TestStatus status;
		try {
			status = TestStatus.valueOf(statusString.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalFailureFileFormatException(file, lineNumber, "No definition for status: " + statusString + ". Line: '" + line + "'");
		}

		String token = line.substring(i+1);

		TestResult parsedResult = parseLine(file, status, lineNumber, token, scanner, build, uniqueId, url);
		TestStatus oldStatus = results.get(parsedResult);
		if(oldStatus == null || parsedResult.getStatus().isMoreInterestingThan(oldStatus)) {
			results.remove(parsedResult);
			results.put(parsedResult, parsedResult.getStatus());
		}
	}

	private static TestResult parseLine(FilePath file, TestStatus status, int lineNumber, String token, Scanner scanner, AbstractBuild build, String uniqueId, String url) {
		switch (status) {
			case ADDED:
			case STARTED:
				return parseSimple(status, token, build, url);
			case FINISHED:
			case SKIPPED:
				return parseSimplePlusMetadata(file, lineNumber, status, token, build, url);
			case FAILED:
				return parseFailure(file, status, lineNumber, token, scanner, build, uniqueId, url);
			default:
				throw new IllegalFailureFileFormatException(file, lineNumber, "Unsupported Test Status: " + status);
		}
	}

	private static TestResult parseSimple(TestStatus status, String token, AbstractBuild build, String url) {
		return new TestResult(token.trim(), -1, null, status, getRealExternalizableId(build), null, 0, null, url);
	}

	private static TestResult parseSimplePlusMetadata(FilePath file, int lineNumber, TestStatus status, String token, AbstractBuild build, String url) {
		String[] split = token.split("\\s");
		String name = split[0];
		String threadId;
		if (split.length > 1) {
			threadId = split[1];
		} else {
			throw new IllegalFailureFileFormatException(file, lineNumber, "Missing Thread ID");
		}
		int runTime;
		if (split.length > 2) {
			runTime = Integer.parseInt(split[2]);
		} else {
			throw new IllegalFailureFileFormatException(file, lineNumber, "Missing Runtime");
		}
		return new TestResult(name, runTime, threadId, status, getRealExternalizableId(build), null, 0, null, url);
	}

	private static TestResult parseFailure(FilePath file, TestStatus status, int lineNumber, String token, Scanner scanner, AbstractBuild build, String uniqueId, String url) {
		String[] split = token.split("\\s");
		String name = split[0];
		String threadId;
		if (split.length > 1) {
			threadId = split[1];
		} else {
			throw new IllegalFailureFileFormatException(file, lineNumber, "Missing Thread ID");
		}
		int runTime;
		if (split.length > 2) {
			runTime = Integer.parseInt(split[2]);
		} else {
			throw new IllegalFailureFileFormatException(file, lineNumber, "Missing Runtime");
		}

		String stackTrace = readStackTrace(scanner);
		AgeStat ageStat = findAge(name, build, uniqueId);
		return new TestResult(name, runTime, threadId, status, getRealExternalizableId(build), stackTrace, ageStat.age, ageStat.firstFailingBuild, url);
	}

	static String getRealExternalizableId(Run build) {
		if(build instanceof MatrixRun) {
			MatrixBuild parentBuild = ((MatrixRun) build).getParentBuild();
			String matrixId = parentBuild.getExternalizableId();
			return matrixId + "$$" + build.getExternalizableId();
		}
		return build.getExternalizableId();
	}

	/**
	 * TODO: Optimize if needed. If we need to optimize: we should be finding the age in groups so we don't have to iterate all the builds multiple times.
	 */
	private static AgeStat findAge(String testName, Run build, String uniqueId) {
		assert uniqueId != null : "null uniqueId";
		AgeStat ageStat = new AgeStat();
		ageStat.age = 1;
		ageStat.firstFailingBuild = getRealExternalizableId(build);
		while((build = build.getPreviousBuild()) != null) {
			TestResultAction testResultAction = build.getAction(TestResultAction.class);
			if(testResultAction != null) {
				if(uniqueId.equals(testResultAction.getUniquifier())) {
					TestResult oldTestResult = testResultAction.getTestResults().get(testName);
					if(oldTestResult != null) {
						TestStatus oldStatus = oldTestResult.getStatus();
						if(oldStatus == TestStatus.FAILED) {
							// FAILED tests should always have an accurate count. So just add that to our running total and return.
							ageStat.age += oldTestResult.getAge();
							ageStat.firstFailingBuild = oldTestResult.getFirstFailingBuildId();
							if(ageStat.firstFailingBuild == null || ageStat.firstFailingBuild.isEmpty()) {
								ageStat.firstFailingBuild = oldTestResult.getRunId();
							}
							return ageStat;
						} else if(oldStatus == TestStatus.STARTED || oldStatus == TestStatus.ADDED) {
							//age isn't calculated on STARTED or ADDED to save time, but we include them in our age, so we should add here, and then continue counting.
							ageStat.age++;
							ageStat.firstFailingBuild = oldTestResult.getRunId();
						} else {
							return ageStat;
						}
					}
				}
			}
		}
		return ageStat;
	}

	private static String readStackTrace(Scanner scanner) {
		String line = scanner.nextLine();
		StringBuilder sb = new StringBuilder();
		while(!line.isEmpty()) {
			sb.append(line).append("\n");
			line = scanner.nextLine().trim();
		}
		return sb.toString().trim();
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public boolean equals(Object other) {
		return other != null && other instanceof TestResult && this.getName().equals(((TestResult) other).getName());
	}

	@Override
	public int compareTo(TestResult o) {
		return this.getName().compareTo(o.getName());
	}

	private static class AgeStat {
		private int age;
		private String firstFailingBuild;
	}
}
