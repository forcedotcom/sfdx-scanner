package sfdc.sfdx.scanner.xml;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import sfdc.sfdx.scanner.messaging.SfdxScannerException;
import sfdc.sfdx.scanner.messaging.EventKey;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class XmlReader {
	private static XmlReader INSTANCE = null;

	public static XmlReader getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new XmlReader();
		}
		return INSTANCE;
	}

	/**
	 * Given the path to a resource, returns an InputStream for that resource.
	 *
	 * @param path - The path to a resource.
	 * @return - An InputStream for the provided resource.
	 * @throws FileNotFoundException - if path is a regular file but that file can't be read
	 */
	private InputStream getResourceAsStream(String path) throws FileNotFoundException {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		if (in == null) {
			in = getClass().getResourceAsStream(path);
		}

		if (in == null) {
			File file = new File(path);
			if (file.isFile()) {
				in = new FileInputStream(file);
			}
		}

		return in;
	}


	/**
	 * Accepts the path to an XML resource, and returns a Document.
	 *
	 * @param path - The path to an XMML resource.
	 * @return - A Document object representing the parsed resource.
	 */
	public Document getDocumentFromPath(String path) {
		Document doc = null;
		try (
			InputStream in = getResourceAsStream(path)
		) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(in);
		} catch (FileNotFoundException fnf) {
			throw new SfdxScannerException(EventKey.ERROR_INTERNAL_CLASSPATH_DOES_NOT_EXIST, path);
		} catch (IOException ioe) {
			throw new SfdxScannerException(EventKey.ERROR_EXTERNAL_XML_NOT_READABLE, ioe, path, ioe.getMessage());
		} catch (ParserConfigurationException | SAXException e) {
			throw new SfdxScannerException(EventKey.ERROR_EXTERNAL_XML_NOT_PARSABLE, e, path, e.getMessage());
		} catch (IllegalArgumentException iae) {
			throw new SfdxScannerException(EventKey.ERROR_INTERNAL_XML_MISSING_IN_CLASSPATH, iae, path);
		}
		return doc;
	}
}
