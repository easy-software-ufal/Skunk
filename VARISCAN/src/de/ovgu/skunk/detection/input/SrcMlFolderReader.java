package de.ovgu.skunk.detection.input;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.ovgu.skunk.detection.data.Feature;
import de.ovgu.skunk.detection.data.FeatureExpressionCollection;
import de.ovgu.skunk.detection.data.FeatureReference;
import de.ovgu.skunk.detection.data.FileCollection;
import de.ovgu.skunk.detection.data.MethodCollection;

/**
 * The Class SrcMlFolderReader.
 */
public class SrcMlFolderReader {

	Map<String, Document> map = new HashMap<>();

	/**
	 * Instantiates a new srcmlfolderreader.
	 */
	public SrcMlFolderReader() {
	}

	/**
	 * Process files to get metrics from srcMl
	 */
	public void ProcessFiles() {
        System.out.println();
        System.out.print("Processing SrcML files ...");

		// go through each feature location and calculate granularity
		for (Feature feat : FeatureExpressionCollection.GetFeatures()) {
			for (FeatureReference loc : feat.getConstants()) {
				this.processFileFromLocations(loc);
			}
		}

        System.out.println(" done.");
	}

	/**
	 * Calculate granularity of the feature location by checking parent nodes
	 *
	 * @param loc
	 *            the feature location
	 */
	private void processFileFromLocations(FeatureReference loc) {
		Document doc = null;
		// Get all lines of the xml and open a positional xml reader
		if (!map.containsKey(loc.filePath)) {
			InputStream fileInput = new ByteArrayInputStream(getFileString(loc.filePath).getBytes());
			try {
				doc = PositionalXmlReader.readXML(fileInput);
			} catch (IOException e) {
				throw new RuntimeException("I/O exception reading stream of file " + loc.filePath, e);
			} catch (SAXException e) {
				throw new RuntimeException("Cannot parse file " + loc.filePath, e);
			}
			try {
				fileInput.close();
			} catch (IOException e) {
				// we don't care if file closing fails
			}
			map.put(loc.filePath, doc);
		} else {
			doc = map.get(loc.filePath);
		}

		// Assign to file
		de.ovgu.skunk.detection.data.File file = FileCollection.GetOrAddFile(loc.filePath);
		file.AddFeatureConstant(loc);

		// go through each directive and find the directive of the specific
		// location by using the start position
		NodeList directives = doc.getElementsByTagName("cpp:directive");
		for (int i = 0; i < directives.getLength(); i++) {
			Node current = directives.item(i);
			if (Integer.parseInt((String) current.getUserData("lineNumber")) == loc.start + 1) {
				// parent contains the if/endif values

				current = current.getParentNode();
				// calculate the granularty by checking each sibling node
				// from start to end of the annotation
				this.calculateGranularityOfConstant(loc, current);

				// assign this location to its corresponding method
				this.assignFeatureConstantToMethod(loc, current);

				break;
			}
		}
	}

	private void calculateGranularityOfConstant(FeatureReference loc, Node current) {
		// check sibling nodes until a granularity defining tag is found or
		// until the end of the annotation
		Node sibling = current;

		// System.out.println(loc.start + 1);
		while (sibling != null && Integer.parseInt((String) sibling.getUserData("lineNumber")) <= loc.end + 1) {
			// set granularity and try to assign a discpline
			loc.SetGranularity(sibling);
			loc.SetDiscipline(sibling);

			// text nodes do not contain line numbers --> next until not #text
			sibling = sibling.getNextSibling();
			while (sibling != null && sibling.getNodeName().equals("#text"))
				sibling = sibling.getNextSibling();
		}
	}

	/**
	 * Gets the file string.
	 *
	 * @param filePath
	 *            the file path
	 * @return the file string
	 */
	private static String getFileString(String filePath) {
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(filePath));
			return new String(encoded, Charset.forName(("UTF-8")));
		} catch (IOException e) {
			throw new RuntimeException("I/O exception reading contents of file " + filePath, e);
		}
	}

	/**
	 * Assign feature constant to method.
	 *
	 * @param constant
	 *            the feature constant
	 * @param annotationNode
	 *            the annotation node
	 */
	private void assignFeatureConstantToMethod(FeatureReference constant, Node annotationNode) {
		// check parent nodes of the annotation until it is of type
		// function/unit
		Node parent = annotationNode.getParentNode();
		while (!parent.getNodeName().equals("function") && !parent.getNodeName().equals("unit")) {
			parent = parent.getParentNode();
		}

		// if parent node is unit, it does not belong to a function
		if (parent.getNodeName().equals("unit"))
			return;
		else {
			// get function signature
			String functionSignature = this.createFunctionSignature(parent);

			// get or create method
			de.ovgu.skunk.detection.data.Method method = MethodCollection.GetMethod(constant.filePath, functionSignature);

			if (method == null) {
				method = new de.ovgu.skunk.detection.data.Method(functionSignature, Integer.parseInt((String) parent.getUserData("lineNumber")),
						this.countLines(parent.getTextContent()));
				MethodCollection.AddMethodToFile(constant.filePath, method);
			}

			// add method to file
			de.ovgu.skunk.detection.data.File file = FileCollection.GetFile(constant.filePath);
			if (file != null)
				file.AddMethod(method);

			// add location to the method
			method.AddFeatureConstant(constant);

		}

	}

	/**
	 * Creates the function signature from the function node
	 *
	 * @param functionNode
	 *            the function node
	 * @return the string
	 */
	private String createFunctionSignature(Node functionNode) {
		// get the whole text content of the node (signature + method content),
		// and remove method content until beginning of block
		String result = functionNode.getTextContent();
		if (result.indexOf('{') == -1) {
			System.out.println(result);
		}
		result = result.substring(0, result.indexOf('{')).trim();

		// clean signature
		if (result.contains("\n"))
			result = result.replace("\n", " ");

		return result;
	}

	/**
	 * Count the amount of lines in a string.
	 *
	 * @param str
	 *            the string
	 * @return amount of lines
	 */
	private int countLines(String str) {
		String[] lines = str.split("\r\n|\r|\n");
		return lines.length;
	}

}
