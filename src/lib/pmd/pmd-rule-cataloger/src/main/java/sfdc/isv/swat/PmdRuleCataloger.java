package sfdc.isv.swat;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;


class PmdRuleCataloger {
  private String pmdVersion;
  private String pmdPath;
  private List<String> languages;
  private Map<String,List<String>> categoryFilesByLanguage = new HashMap<>();
  private Map<String,List<String>> rulesetFilesByLanguage = new HashMap<>();


  /**
   *
   * @param pmdVersion   - The version of PMD being used, e.g. 6.20.0.
   * @param pmdPath      - The path to PMD's lib folder.
   * @param languages    - The languages whose rules should be catalogued.
   */
  PmdRuleCataloger(String pmdVersion, String pmdPath, List<String> languages) {
    this.pmdVersion = pmdVersion;
    this.pmdPath = pmdPath;
    this.languages = languages;
  }


  /**
   * TODO: WRITE A REAL HEADER FOR THIS FUNCTION.
   */
  void catalogRules() {
    // STEP 1: Identify all of the ruleset and category files for each language we're looking at.
    for (String language : this.languages) {
      this.mapFileNamesByLanguage(language);
    }

    // STEP 2: Sweep the category files and pull out their rule definitions.
    for (List<String> cats : categoryFilesByLanguage.values()) {
      for (String cat : cats) {
        deriveRulesFromCategoryFile(cat);
      }
      break;
    }

    /*
    remaining steps:
    - parse the category files to get information about the rules.
    - parse the ruleset files to figure out which rules belong to which sets.
    - write it all out to a file somewhere.
     */

    // TODO: REPLACE THIS LOG WITH THE REST.
    System.out.println("Category map");
    System.out.println(this.categoryFilesByLanguage.toString());
    System.out.println("Ruleset map");
    System.out.println(this.rulesetFilesByLanguage.toString());
  }


  /**
   * Returns the name of the JAR in which PMD stores rule definitions for the specified language.
   * @param language - The language for which we should find the corresponding PMD JAR.
   * @return         - The name of the corresponding JAR.
   */
  private String deriveJarNameForLanguage(String language) {
    return "pmd-" + language + "-" + this.pmdVersion + ".jar";
  }


  /**
   * Identifies the JAR associated with the given language, and puts all category and ruleset definition files into the
   * corresponding Map field.
   * @param language - The language that should be processed.
   */
  private void mapFileNamesByLanguage(String language) {
    // First, define our lists.
    List<String> categoryFiles = new ArrayList<>();
    List<String> rulesetFiles = new ArrayList<>();

    // Next, we need to determine the path to the JAR we want to inspect, and then do that inspection.
    String jarPath = this.pmdPath + File.separator + this.deriveJarNameForLanguage(language);
    try {
      // Read in the JAR file.
      JarInputStream jstream = new JarInputStream(new FileInputStream(jarPath));
      JarEntry entry;

      // Iterate over every entry in the stream. These are the names of files/directories in that JAR.
      while ((entry = jstream.getNextJarEntry()) != null) {
        String fName = entry.getName();
        // We only care about .xml files, since those are how rulesets and categories are defined.
        if (fName.endsWith(".xml")) {
          // Rulesets all live in "rulesets/**/*.xml", and Categories live in "category/**/*.xml". It's frustrating that
          // one's plural and the other isn't, but sometimes life involves compromises.
          if (fName.startsWith("category")) {
            categoryFiles.add(fName);
          } else if (fName.startsWith("rulesets")) {
            rulesetFiles.add(fName);
          }
        }
      }
    } catch (FileNotFoundException fnf) {
      // TODO: Better error handling here.
      System.out.println("FileNotFound exception: " + fnf.getMessage());
    } catch (IOException io) {
      // TODO: Better error handling here.
      System.out.println("IOException: " + io.getMessage());
    }

    // Finally, map the files we found by the language name.
    this.categoryFilesByLanguage.put(language, categoryFiles);
    this.rulesetFilesByLanguage.put(language, rulesetFiles);
  }


  /**
   * TODO: WRITE A REAL HEADER FOR THIS FUNCTION.
   * @param path
   */
  private void deriveRulesFromCategoryFile(String path) {
    System.out.println("======");
    System.out.println("Parsing category file: " + path);

    // The category file is an XML, so we'll need to parse that into a Document for us to analyze.
    Document doc = getDocumentFromPath(path);

    // Next, we'll want to pull off our root element (technically of type "ruleset"), and get a list of all of the 'rule' nodes.
    Element catNode = doc.getDocumentElement();
    System.out.println("The category's name is: " + catNode.getTagName());
    NodeList ruleNodes = catNode.getElementsByTagName("rule");
    System.out.println("The category defines this many rules: " + ruleNodes.getLength());
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
  private Document getDocumentFromPath(String path) {
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
