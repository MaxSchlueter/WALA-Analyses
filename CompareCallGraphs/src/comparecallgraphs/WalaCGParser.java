package comparecallgraphs;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.traverse.BFSIterator;

import comparecallgraphs.CCGNode.SourcePosition;

/**
 * @author Max Schlueter
 *
 */
public class WalaCGParser implements CGParser {
	
	public static boolean DEBUG = false;

	@Override
	public ComparableCG parse(File file) {
		try {
			return buildCallGraph(file);
		} catch (IllegalArgumentException | IOException | WalaException | CancelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private int[] offsetToColumnNumbers;
	private int[] offsetToLineNumbers; 
	
	private void computeLineAndColumnNumbers(File file) throws IOException {
		int fileLength = (int) file.length() + 1;
		offsetToColumnNumbers = new int[fileLength];
		offsetToLineNumbers = new int[fileLength];
		List<String> lines = FileUtils.readLines(file);
		
		int index = 0, lineNumber = 0;
		for (String l : lines) {
			lineNumber++;
			for (int i = 1; i <= l.length() + 1; i++) {
				offsetToColumnNumbers[index] = i;
				offsetToLineNumbers[index] = lineNumber;
				index++;
			}
		}
		if (index + 1 == fileLength) {
			offsetToColumnNumbers[index] = 1;
			offsetToLineNumbers[index] = lineNumber + 1;
		}
		if (DEBUG) {
			System.out.println("----------OFFSET TO LINE AND COLUMN NUMBERS-----------\n");
			System.out.printf("%4d", 0);
			for (int i = 1; i < offsetToLineNumbers.length; i++) System.out.printf(", %4d", i);
			System.out.println("");
			System.out.printf("%4d", offsetToLineNumbers[0]);
			for (int i = 1; i < offsetToLineNumbers.length; i++) System.out.printf(", %4d", offsetToLineNumbers[i]);
			System.out.println("");
			System.out.printf("%4d", offsetToColumnNumbers[0]);
			for (int i = 1; i < offsetToColumnNumbers.length; i++) System.out.printf(", %4d", offsetToColumnNumbers[i]);
			System.out.println("\n");
			System.out.println("------------------------------------------------------\n");
		}
	}
	
	private CCGNode getFunctionNode(CGNode node) {
		String typeName = node.getMethod().getDeclaringClass().getName().toString();
		// Because name is a typename like Lprologue.js we need to get rid of the first char
		String funName = FilenameUtils.getName(typeName.substring(1));
		Position position = ((AstMethod)node.getMethod()).getSourcePosition();
		int firstOffset = position.getFirstOffset();
		int lastOffset = position.getLastOffset();
		if (funName.endsWith(".js")) {
			lastOffset = offsetToLineNumbers.length - 1;
		}
		SourcePosition sourcePosition = new SourcePosition(offsetToLineNumbers[firstOffset],
				offsetToColumnNumbers[firstOffset], offsetToLineNumbers[lastOffset], offsetToColumnNumbers[lastOffset]);
		return new CCGNode(sourcePosition, funName);
	}
	
	private ComparableCG buildCallGraph(File file) throws IOException, WalaException, IllegalArgumentException, CancelException {
		JavaScriptTranslatorFactory translatorFactory = new CAstRhinoTranslatorFactory();
		JSCallGraphUtil.setTranslatorFactory(translatorFactory);
		
		JSCFABuilder builder = JSCallGraphBuilderUtil.makeScriptCGBuilder(file.getParent(), file.getName());
		
		CallGraph cg = builder.makeCallGraph(builder.getOptions());
		
		if (DEBUG) {
			System.err.println(CallGraphStats.getStats(cg));
			System.out.println(cg);
		}
		
		Map<Integer, HashSet<Integer>> callerIdToCalleeIds = pruneWalaCallGraph(cg);
				
		ComparableCG walaCG = new ComparableCGImpl();
		
		computeLineAndColumnNumbers(file);
		
		for (int callerId : callerIdToCalleeIds.keySet()) {
			CCGNode caller = getFunctionNode(cg.getNode(callerId));
			for (int calleeId : callerIdToCalleeIds.get(callerId)) {
				CCGNode callee = getFunctionNode(cg.getNode(calleeId));
				walaCG.addEdge(caller, callee);
			}
		}
		
		if (DEBUG) {
			System.out.println("----------STATIC CALL GRAPH-----------\n");
			System.out.println(walaCG);
			System.out.println("--------------------------------------\n");
		}
	
		return walaCG;
	}
	
	private boolean isValidFunction(CGNode node) {
		IClass nodeClass = node.getMethod().getDeclaringClass();
		// only allow targets to functions that are declared in the code
		if (nodeClass.getMethod(AstMethodReference.fnSelector) == null) return false;
		
		String nodeClassName = nodeClass.getName().toString();
		
		if (nodeClassName.startsWith("Lprologue.js")) return false;
		
		if (node.getMethod().isSynthetic()) return false;
		
		if (nodeClassName.matches("(.*)_forin_body_\\d+")) return false;
		
		return true;
	}
	
	private Map<Integer, HashSet<Integer>> pruneWalaCallGraph(CallGraph cg) {
		Map<Integer, HashSet<Integer>> callerIdToCalleeIds = new HashMap<Integer, HashSet<Integer>>();
		
		for (CGNode node : cg) {

			if (node.equals(cg.getFakeRootNode())) continue;
			
			if (!isValidFunction(node)) continue;

			for(Iterator<CGNode> targets = cg.getSuccNodes(node); targets.hasNext(); ) {
				CGNode target = targets.next();
				IMethod targetMethod = target.getMethod();
				IClass targetClass = targetMethod.getDeclaringClass();
				String targetClassName = targetClass.getName().toString();
				
				// only allow targets to functions that are declared in the code
				if (targetMethod.getDeclaringClass().getMethod(AstMethodReference.fnSelector) == null) continue;
	
				// apply, call and constructors create snythetic methods
				// correlation tracking creates new methods that end with "_forin_body_1"??
				if (targetMethod.isSynthetic() || targetClassName.startsWith("Lprologue.js") || targetClassName.matches("(.*)_forin_body_\\d+")) {
					BFSIterator<CGNode> it = new BFSIterator<CGNode>(cg, target) {
						@Override
						protected Iterator<CGNode> getConnected(CGNode n) {
							if (n.getMethod().isSynthetic() || targetClassName.startsWith("Lprologue.js") || targetClassName.matches("(.*)_forin_body_\\d+")) {
								return G.getSuccNodes(n);
							}
							return Collections.emptyIterator();
						}
					};
					while (it.hasNext()) {
						CGNode callee = it.next();
						
						if (isValidFunction(callee)) {
							int callerId = node.getGraphNodeId();
							if (!callerIdToCalleeIds.containsKey(callerId)) {
								callerIdToCalleeIds.put(callerId, new HashSet<Integer>());
							}
							int calleeId = callee.getGraphNodeId();
							callerIdToCalleeIds.get(callerId).add(calleeId);
						}
					}
				} else {
					int callerId = node.getGraphNodeId();
					if (!callerIdToCalleeIds.containsKey(callerId)) {
						callerIdToCalleeIds.put(callerId, new HashSet<Integer>());
					}
					int calleeId = target.getGraphNodeId();
					callerIdToCalleeIds.get(callerId).add(calleeId);
				}
			}
		}
		
		return callerIdToCalleeIds;
	}

}
