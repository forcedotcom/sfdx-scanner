package sfdc.isv.swat;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

class XmlReader {
  private static XmlReader INSTANCE = null;
  static XmlReader getInstance() {
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
  Document getDocumentFromPath(String path) {
    Document doc = null;
    try (
      InputStream in = getResourceAsStream(path)
    ) {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      doc = db.parse(in);
    } catch (IOException ioe) {
      // TODO: Better error handling here.
      System.out.println("IOException: " + ioe.getMessage());
      System.exit(1);
    } catch (ParserConfigurationException pce) {
      // TODO: Better error handling here.
      System.out.println("ParserConfigurationException: " + pce.getMessage());
      System.exit(1);
    } catch (SAXException saxe) {
      // TODO: Better error handling here.
      System.out.println("SAXException: " + saxe.getMessage());
      System.exit(1);
    }
    return doc;
  }
}
