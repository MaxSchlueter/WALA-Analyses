package comparecallgraphs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Max Schlueter
 *
 */
public class ComparableCGImpl implements ComparableCG {
	
    private final Set<CCGNode> nodes = new HashSet<CCGNode>();
    
    private final Map<CCGNode,Set<CCGNode>> successors = new HashMap<CCGNode, Set<CCGNode>>();
    
    private final Map<CCGNode,Set<CCGNode>> predecessors = new HashMap<CCGNode, Set<CCGNode>>();

    @Override
	public Set<CCGNode> getCallers() {
		return nodes.stream().filter(n -> successors.get(n) != null).collect(Collectors.toSet());
	}

	@Override
	public Set<CCGNode> getTargets(CCGNode node) {
		return successors.get(node);
	}

	@Override
	public void removeNodeAndEdges(CCGNode n) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<CCGNode> iterator() {
		return nodes.iterator();
	}

	@Override
	public int getNumberOfNodes() {
		return nodes.size();
	}

	@Override
	public void addNode(CCGNode n) {
		nodes.add(n);
	}

	@Override
	public void removeNode(CCGNode n) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsNode(CCGNode n) {
		return nodes.contains(n);
	}

	@Override
	public Iterator<CCGNode> getPredNodes(CCGNode n) {
		return predecessors.get(n).iterator();
	}

	@Override
	public int getPredNodeCount(CCGNode n) {
		return predecessors.get(n).size();
	}

	@Override
	public Iterator<CCGNode> getSuccNodes(CCGNode n) {
		return successors.get(n).iterator();
	}

	@Override
	public int getSuccNodeCount(CCGNode N) {
		return successors.get(N).size();
	}

	@Override
	public void addEdge(CCGNode src, CCGNode dst) {
        Set<CCGNode> predSet = predecessors.get(dst);
        if(predSet == null) {
            predSet = new HashSet<CCGNode>();
            predecessors.put(dst,predSet);
            nodes.add(dst);
        }
        predSet.add(src);
        Set<CCGNode> succSet = successors.get(src);
        if(succSet == null) {
            succSet = new HashSet<CCGNode>();
            successors.put(src,succSet);
            nodes.add(src);
        }
        succSet.add(dst);
	}

	@Override
	public void removeEdge(CCGNode src, CCGNode dst) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAllIncidentEdges(CCGNode node) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeIncomingEdges(CCGNode node) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeOutgoingEdges(CCGNode node) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasEdge(CCGNode src, CCGNode dst) {
		Set<CCGNode> succSet = successors.get(src);
		if (succSet == null) {
			return false;
		}
		return succSet.contains(dst);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<CCGNode, Set<CCGNode>> caller : successors.entrySet()) {
			sb.append(caller.getKey() + " calls:\n");
			for (CCGNode callee : caller.getValue()) {
				sb.append("\t" + callee + "\n");
			}
		}
		return sb.toString();
	}

}
