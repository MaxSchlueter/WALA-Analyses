package comparecallgraphs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import comparecallgraphs.CCGNode.SourcePosition;

/**
 * @author Max Schlueter
 *
 */
public class JalangiCGParser implements CGParser {
	
	public static boolean DEBUG = false;

	@Override
	public ComparableCG parse(File file) {
		try {
			return parseJson(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private CCGNode getFunctionNode(String str) {
		String[] parts = str.split("@");
		String funName = parts[0];
		int[] location = Arrays.stream(parts[1].split(",")).mapToInt(d -> Integer.parseInt(d)).toArray();
		SourcePosition sourcePosition = new SourcePosition(location[0], location[1], location[2], location[3]);
		return new CCGNode(sourcePosition, funName);
	}

	private ComparableCG parseJson(File file) throws IOException {
		String json = FileUtils.readFileToString(file);
		
		ComparableCG jalangiCG = new ComparableCGImpl();
		
		JsonParser parser = new JsonParser();
		JsonObject callerIds = parser.parse(json).getAsJsonObject();
		
		for (Map.Entry<String, JsonElement> callerId : callerIds.entrySet()) {
			CCGNode caller = getFunctionNode(callerId.getKey());
			JsonArray calleeIds = callerId.getValue().getAsJsonArray();
			for (JsonElement calleeId : calleeIds) {
				CCGNode callee = getFunctionNode(calleeId.getAsString());
				jalangiCG.addEdge(caller, callee);
			}
		}
		
		if (DEBUG) {
			System.out.println("----------DYNAMIC CALL GRAPH----------\n");
			System.out.println(jalangiCG);
			System.out.println("--------------------------------------\n");
		}
		
		return jalangiCG;
	}

}
