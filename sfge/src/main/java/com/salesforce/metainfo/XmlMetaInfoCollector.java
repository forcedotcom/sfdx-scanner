package com.salesforce.metainfo;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.ProgrammingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Abstract implementation of {@link AbstractMetaInfoCollector} to collect information from XML
 * files.
 */
public abstract class XmlMetaInfoCollector extends AbstractMetaInfoCollector {
    private static final Logger LOGGER = LogManager.getLogger(XmlMetaInfoCollector.class);

    private final HashSet<Pattern> pathPatterns;
    private final DocumentBuilderFactory documentBuilderFactory;

    public XmlMetaInfoCollector() {
        super();
        pathPatterns = buildPathPatterns();
        documentBuilderFactory = getDocumentBuilderFactory();
    }

    /**
     * @return Path patterns that the implementation is interested in
     */
    abstract HashSet<String> getPathPatterns();

    /** Collect meta info from XML file into {@link AbstractMetaInfoCollector#collectedMetaInfo} */
    abstract void collectMetaInfo(Path path, Document xmlDocument);

    @Override
    protected TreeSet<String> getAcceptedExtensions() {
        return CollectionUtil.newTreeSetOf(".xml");
    }

    @Override
    protected void processProjectFile(Path path) {
        boolean pathMatched = false;
        // Make sure the current file matches at least one of the path patterns of implementation
        for (Pattern pathPattern : pathPatterns) {
            if (pathPattern.matcher(path.toString()).find()) {
                pathMatched = true;
                break;
            }
        }

        if (pathMatched) {
            // This is a file the implementation is interested in.
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Processing XML file {}", path);
            }
            final Optional<Document> xmlDocument = loadXml(path);
            if (xmlDocument.isPresent()) {
                collectMetaInfo(path, xmlDocument.get());
            }
        }
    }

    /** Get Xml document from Path */
    private Optional<Document> loadXml(Path path) {
        Document doc;
        File file = new File(path.toString());
        if (file.isFile()) {
            try {
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                doc = documentBuilder.parse(file);
                return Optional.of(doc);
            } catch (IOException | ParserConfigurationException | SAXException ex) {
                // Wrap these exceptions in our own custom exception, for convenience.
                throw new MetaInfoLoadException("Failed to read XML file " + path, ex);
            }
        }
        return Optional.empty();
    }

    /** Builds {@link Pattern} instances for all the pattern strings. */
    private HashSet<Pattern> buildPathPatterns() {
        final HashSet<Pattern> pathPatternsInternal = new HashSet<>();
        final HashSet<String> pathPatternStrings = getPathPatterns();
        for (String pathPatternString : pathPatternStrings) {
            // Keeping patterns case sensitive
            pathPatternsInternal.add(Pattern.compile(pathPatternString));
        }

        return pathPatternsInternal;
    }

    /**
     * @return new instance of {@link DocumentBuilderFactory}
     */
    private DocumentBuilderFactory getDocumentBuilderFactory() {
        DocumentBuilderFactory documentBuilderFactoryInternal =
                DocumentBuilderFactory.newInstance();

        try {
            documentBuilderFactoryInternal.setFeature(
                    "http://xml.org/sax/features/external-general-entities", false);
            documentBuilderFactoryInternal.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities", false);
            documentBuilderFactoryInternal.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            documentBuilderFactoryInternal.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException e) {
            throw new ProgrammingException(e);
        }

        return documentBuilderFactoryInternal;
    }
}
