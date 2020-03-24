package sfdc.sfdx.scanner.xml;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import sfdc.sfdx.scanner.SfdxScannerException;
import sfdc.sfdx.scanner.EventKey;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
   * @param path - The path to a resource.
   * @return     - An InputStream for the provided resource.
   */
  private InputStream getResourceAsStream(String path) {
    final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    return in == null ? getClass().getResourceAsStream(path) : in;
  }


  /**
   * Accepts the path to an XML resource, and returns a Document.
   * @param path - The path to an XMML resource.
   * @return     - A Document object representing the parsed resource.
   */
  public Document getDocumentFromPath(String path) {
    Document doc = null;
    try (
      InputStream in = getResourceAsStream(path)
    ) {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      doc = db.parse(in);
    } catch (IOException ioe) {
      throw new SfdxScannerException(EventKey.ERROR_EXTERNAL_XML_NOT_READABLE, new String[]{path, ioe.getMessage()}, ioe);
    } catch (ParserConfigurationException | SAXException e) {
      throw new SfdxScannerException(EventKey.ERROR_EXTERNAL_XML_NOT_PARSABLE, new String[]{path, e.getMessage()}, e);
    }
    return doc;
  }
}
