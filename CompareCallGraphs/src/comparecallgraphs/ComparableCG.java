package comparecallgraphs;

import java.util.Set;

import com.ibm.wala.util.graph.Graph;

/**
 * @author Max Schlueter
 *
 */
public interface ComparableCG extends Graph<CCGNode> {
	
	public Set<CCGNode> getTargets(CCGNode node);
	
	public Set<CCGNode> getCallers();
}
