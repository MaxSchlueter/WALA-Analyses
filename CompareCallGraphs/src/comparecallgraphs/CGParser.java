package comparecallgraphs;

import java.io.File;

/**
 * @author Max Schlueter
 *
 */
public interface CGParser {
	
	public ComparableCG parse(File file);
	
}
