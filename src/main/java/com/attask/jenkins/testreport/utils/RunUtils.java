package com.attask.jenkins.testreport.utils;

import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Run;

import java.util.List;
import java.util.logging.Logger;

/**
 * User: Joel Johnson
 * Date: 2/20/13
 * Time: 11:47 AM
 */
public class RunUtils {
	public static final Logger log = Logger.getLogger("TestReportTool");

	public static String getRealExternalizableId(Run build) {
		if(build instanceof MatrixRun) {
			MatrixBuild parentBuild = ((MatrixRun) build).getParentBuild();
			String matrixId = parentBuild.getExternalizableId();
			return matrixId + "$$" + build.getExternalizableId();
		}
		return build.getExternalizableId();
	}

	public static Run findRun(String id) {
		if(id == null) {
			return null;
		}
		if(!id.contains("$$")) {
			return AbstractBuild.fromExternalizableId(id);
		}

		String[] ids = id.split("\\$\\$", 2);
		if(ids.length != 2) {
			return null;
		}

		String parentMatrixId = ids[0];
		String childMatrixId = ids[1];

		if(parentMatrixId == null || childMatrixId == null) {
			//There were NPE showing up in the log from parentMatrixId being null. Adding this to get more visibility.
			log.severe("String.split returned null in the array. How is that possible? original string:`" + id + "` split[0]: `" + parentMatrixId + "` split[1]: `" + childMatrixId + "`");
			return null;
		}

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

		return null;
	}
}
