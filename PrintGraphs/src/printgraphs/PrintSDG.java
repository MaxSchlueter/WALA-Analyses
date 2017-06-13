package printgraphs;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.modref.JavaScriptModRef;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.MethodEntryStatement;
import com.ibm.wala.ipa.slicer.MethodExitStatement;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.DotUtil.DotOutputType;
import com.ibm.wala.viz.PDFViewUtil;

public class PrintSDG {

	private final static String SVG_FILE = "sdg.svg";

	private static Collection<Statement> slice;

	public static void main(String[] args) {
		try {
			run(args[0]);
		} catch (IOException | IllegalArgumentException | CancelException | WalaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Process run(String path) throws IOException, IllegalArgumentException, CancelException, WalaException {
		FileProvider provider = new FileProvider();
		File file = provider.getFile(path);

		JavaScriptTranslatorFactory translatorFactory = new CAstRhinoTranslatorFactory();
		JSCallGraphUtil.setTranslatorFactory(translatorFactory);
		
		JSCFABuilder builder = JSCallGraphBuilderUtil.makeScriptCGBuilder(file.getParent(), file.getName());
		
		CallGraph cg = builder.makeCallGraph(builder.getOptions());

		// new JsViewer(cg, builder.getPointerAnalysis());

		System.out.println(CallGraphStats.getStats(cg));

		// System.out.println(cg.toString());

		SDG<InstanceKey> sdg = new SDG<InstanceKey>(cg, builder.getPointerAnalysis(),
				new JavaScriptModRef<InstanceKey>(), Slicer.DataDependenceOptions.NO_EXCEPTIONS,
				Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);

//		System.out.println(sdg.getNumberOfNodes());

		Statement entry = new MethodEntryStatement(getEntryNode(cg));
		slice = Slicer.computeForwardSlice(sdg, entry);

		Properties p = null;
		try {
			p = WalaExamplesProperties.loadProperties();
			p.putAll(WalaProperties.loadProperties());
		} catch (WalaException e) {
			e.printStackTrace();
			Assertions.UNREACHABLE();
		}

		String svgFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + SVG_FILE;
		String dotExe = p.getProperty(WalaExamplesProperties.DOT_EXE);

		Graph<Statement> g = pruneSDG(sdg);
		DotUtil.setOutputType(DotOutputType.SVG);
		DotUtil.dotify(g, null, PDFTypeHierarchy.DOT_FILE, svgFile, dotExe);

		String gvExe = p.getProperty(WalaExamplesProperties.SVGVIEW_EXE);
		return PDFViewUtil.launchPDFView(svgFile, gvExe);
	}

	private static Graph<Statement> pruneSDG(final SDG<InstanceKey> sdg) {
		Predicate<Statement> f = new Predicate<Statement>() {
			@Override
			public boolean test(Statement s) {
				return slice.contains(s);
			}
		};
		return GraphSlicer.prune(sdg, f);
	}

	private static CGNode getEntryNode(CallGraph cg) {
		CGNode fake = cg.getFakeRootNode();
		for (Iterator<CGNode> it = cg.getSuccNodes(fake); it.hasNext();) {
			CGNode node = it.next();
			if (!node.getMethod().toString().contains("Lprologue.js")) {
				// take the first one
				return node;
			}
		}
		return null;
	}

}
