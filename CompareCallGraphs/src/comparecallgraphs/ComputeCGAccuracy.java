package comparecallgraphs;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import com.ibm.wala.util.io.FileProvider;

/**
 * @author Max Schlueter
 *
 */
public class ComputeCGAccuracy {

	public static void main(String[] args) {
		try {
			run(args);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void run(String[] paths) throws IOException {
		FileProvider provider = new FileProvider();
		
		String pathToJs = paths[0], pathToJson;
		if (paths.length == 1) {
			String baseName = FilenameUtils.getBaseName(pathToJs);
			pathToJson = "jalangi/cgs/" + baseName + ".json";
		} else {
			pathToJson = paths[1];
		}
		
		File jsFile = provider.getFile(pathToJs);
		CGParser parser = new WalaCGParser();
		ComparableCG staticCG = parser.parse(jsFile);
		
		File jsonFile = provider.getFile(pathToJson);
		parser = new JalangiCGParser();
		ComparableCG dynamicCG = parser.parse(jsonFile);
		
		System.out.println("Computing precision and recall for file: " + jsFile.getName());
		computePrecisionAndRecall(dynamicCG, staticCG);
	}
	
	private static void computePrecisionAndRecall(ComparableCG dynamicCG, ComparableCG staticCG) {
		double accumulatedPrecision = 0, accumulatedRecall = 0;
		Set<CCGNode> dynamicCallers = dynamicCG.getCallers();
		for (CCGNode node : dynamicCallers) {
			if (!staticCG.containsNode(node)) {
				continue;
			}
			
			Set<CCGNode> dynamicTargets = dynamicCG.getTargets(node);
			Set<CCGNode> staticTargets = staticCG.getTargets(node);
			
			long intersectionSize = dynamicTargets.stream().filter(n -> staticTargets.contains(n)).count();
			
			accumulatedPrecision += (double) intersectionSize / (double) staticTargets.size();
			
			accumulatedRecall += (double) intersectionSize / (double) dynamicTargets.size();	
		}

		System.out.println("Average precision: " + accumulatedPrecision / dynamicCallers.size());
		System.out.println("Average recall:    " + accumulatedRecall / dynamicCallers.size());
	}

}
