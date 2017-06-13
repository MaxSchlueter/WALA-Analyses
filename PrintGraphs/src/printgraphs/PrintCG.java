package printgraphs;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.DotUtil.DotOutputType;
import com.ibm.wala.viz.PDFViewUtil;

public class PrintCG {

	private final static String SVG_FILE = "cg.svg";

	public static void main(String[] args) {
		try {
			run(args[0]);
		} catch (IllegalArgumentException | IOException | CancelException | WalaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Process run(String path)
			throws IOException, IllegalArgumentException, CancelException, WalaException {
		FileProvider provider = new FileProvider();
		File file = provider.getFile(path);

		JavaScriptTranslatorFactory translatorFactory = new CAstRhinoTranslatorFactory();
		JSCallGraphUtil.setTranslatorFactory(translatorFactory);
		
		JSCFABuilder builder = JSCallGraphBuilderUtil.makeScriptCGBuilder(file.getParent(), file.getName());
		
		CallGraph cg = builder.makeCallGraph(builder.getOptions());

		// new JsViewer(cg, builder.getPointerAnalysis());

		System.out.println(CallGraphStats.getStats(cg));

		// System.out.println(cg.toString());

		Graph<CGNode> graph = pruneCG(cg, file.getName());

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
		DotUtil.setOutputType(DotOutputType.SVG);
		DotUtil.dotify(graph, null, PDFTypeHierarchy.DOT_FILE, svgFile, dotExe);

		String gvExe = p.getProperty(WalaExamplesProperties.SVGVIEW_EXE);
		return PDFViewUtil.launchPDFView(svgFile, gvExe);
	}

	private static Graph<CGNode> pruneCG(final CallGraph cg, final String filename) throws WalaException {
		Predicate<CGNode> f = new Predicate<CGNode>() {
			@Override
			public boolean test(CGNode n) {
				// prune methods that are defined in prologue.js and that are not called in the code
				// (e.g. synthetic constructor functions)
				return !n.getMethod().toString().contains("Lprologue.js")
						&& n.getMethod().getDeclaringClass().getMethod(AstMethodReference.fnSelector) != null;
			}
		};
		return PDFTypeHierarchy.pruneGraph(cg, f);
	}
}
