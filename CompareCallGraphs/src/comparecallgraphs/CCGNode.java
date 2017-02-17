package comparecallgraphs;

/**
 * @author Max Schlueter
 *
 */
public class CCGNode {
	
	public static class SourcePosition {
		int getFirstLine;
		int getLastLine;
		int getFirstCol;
		int getLastCol;
		
		public SourcePosition(int getFirstLine, int getFirstCol, int getLastLine, int getLastCol) {
			this.getFirstLine = getFirstLine;
			this.getFirstCol = getFirstCol;
			this.getLastLine = getLastLine;
			this.getLastCol = getLastCol;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getFirstCol;
			result = prime * result + getFirstLine;
			result = prime * result + getLastCol;
			result = prime * result + getLastLine;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SourcePosition other = (SourcePosition) obj;
			if (getFirstCol != other.getFirstCol)
				return false;
			if (getFirstLine != other.getFirstLine)
				return false;
			if (getLastCol != other.getLastCol)
				return false;
			if (getLastLine != other.getLastLine)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "[" + getFirstLine + ":" + getFirstCol + ":" + getLastLine + ":" + getLastCol + "]";
		}
	}
	
	private final SourcePosition position;
	
	private final String name;

	public CCGNode(SourcePosition position, String name) {
		this.position = position;
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((position == null) ? 0 : position.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CCGNode other = (CCGNode) obj;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (name.length() == 0) {
			return "??? at " + position.toString();
		}
		return name + " at " + position;
	}
	
}
