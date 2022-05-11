package com.salesforce.metainfo;

import com.google.common.annotations.VisibleForTesting;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VisualForceHandlerImpl extends AbstractMetaInfoCollector {
    private static final Logger LOGGER = LogManager.getLogger(VisualForceHandlerImpl.class);

    @Override
    protected TreeSet<String> getAcceptedExtensions() {
        return CollectionUtil.newTreeSetOf(".page", ".component");
    }

    /** The name of the regex capture group that contains the rest of the line being scanned. */
    private static final String REST_OF_LINE = "restOfLine";

    /** The name of the regex capture group that contains referenced Apex classes. */
    private static final String APEX_CLASSES = "apexClasses";

    /**
     * After identifying the start of a VF tag, this is how many lines we'll scan looking for
     * referenced apex classes.
     */
    private static final int MAX_TAG_LINES = 15;

    /**
     * This regex identifies the start of a comment. It matches any amount of whitespace, followed
     * by `
     * <!--`. Then it
     * reluctantly matches characters until it sees `-->
     * `, and everything past that is captured in the {@link #REST_OF_LINE} group.
     */
    private static final Pattern COMMENT_PATTERN =
            Pattern.compile("^\\s*<!--.*?(?:-->(?<" + REST_OF_LINE + ">.*))?$");

    /**
     * This regex identifies when acomment ends. It matches all characters until it sees `-->`, and
     * everything after that // is captured in the {@link #REST_OF_LINE} group.
     */
    private static final Pattern END_OF_COMMENT_PATTERN =
            Pattern.compile(".*-->(?<" + REST_OF_LINE + ">.*)$");

    /**
     * This regex identifies `<apex:page` and `<apex:component` opening tags, then captures
     * everything else in the {@link #REST_OF_LINE} group. The `i` flag makes it case-insensitive.
     */
    private static final Pattern VF_TAG_PATTERN =
            Pattern.compile("(?i)^\\s*<apex:(?:component|page)(?<" + REST_OF_LINE + ">.*)");

    /**
     * This regex is one of several that captures information about scanned VF files. It matches
     * `controller` at the start of a word, then any amount of whitespace, then `=`, any whitespace,
     * double-quotes, and then any word characters, commas, or whitespace is captured in the {@link
     * #APEX_CLASSES} group. The `i` flag makes it case-insensitive, and the `m` flag makes it
     * multi-line.
     */
    private static final Pattern CONTROLLER_CAPTURE_PATTERN =
            Pattern.compile(
                    "(?im)\\bcontroller\\s*=\\s*\"(?<" + APEX_CLASSES + ">[\\w,\\s]+)\"\\W");
    /**
     * This regex is one of several that captures information about scanned VF files. It matches
     * `extensions` at the start of a word, then any amount of whitespace, then `=`, any whitespace,
     * double-quotes, and then any word characters, commas, or whitespace is captured in the {@link
     * #APEX_CLASSES} group. The `i` flag makes it case-insensitive, and the `m` flag makes it
     * multi-line.
     */
    private static final Pattern EXTENSIONS_CAPTURE_PATTERN =
            Pattern.compile(
                    "(?im)\\bextensions\\s*=\\s*\"(?<" + APEX_CLASSES + ">[\\w,\\s]+)\"\\W");

    @VisibleForTesting
    protected VisualForceHandlerImpl() {
        super();
    }

    @Override
    protected void processProjectFile(Path path) {
        if (!pathMatches(path)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Skipping non-VF file. path=" + path);
            }
            return;
        }
        File file = new File(path.toString());
        if (file.exists() && file.isFile()) {
            findAndProcessVfTags(file);
        }
    }

    private void findAndProcessVfTags(File file) {
        try (final Scanner scanner = new Scanner(file)) {
            boolean insideComment = false;
            while (scanner.hasNextLine()) {
                // Get the next line in the file.
                String lineContents = scanner.nextLine();

                // While inside a comment, we're just looking for the end of that comment.
                if (insideComment) {
                    // If we find the end of the comment, we note that we've left the comment, and
                    // we'll process the
                    // remainder of the line as though it was its own line altogether.
                    final Matcher commentEndMatcher = END_OF_COMMENT_PATTERN.matcher(lineContents);
                    if (commentEndMatcher.lookingAt()) {
                        insideComment = false;
                        lineContents = commentEndMatcher.group(REST_OF_LINE);
                        // If necessary, default to an empty string to avoid NPEs later.
                        lineContents = lineContents != null ? lineContents : "";
                    }
                }

                // If we're not in a comment, the first thing we want to look for is a comment.
                // Since comments can be inline,
                // it's possible for multiple consecutive comments to exist on the same line. So we
                // want to apply the
                // comment regex in a loop, to make sure we fully process the line.
                while (true) {
                    final Matcher commentMatcher = COMMENT_PATTERN.matcher(lineContents);
                    final boolean matchFound = commentMatcher.lookingAt();
                    if (!matchFound) {
                        // If the regex didn't match at all, then the line isn't inside of a
                        // comment.
                        break;
                    } else if (commentMatcher.group(REST_OF_LINE) != null) {
                        // If the regex matched and the capture group is non-null, then the comment
                        // was only one line long,
                        // and the group is the rest of the line. Update the line contents and keep
                        // matching.
                        lineContents = commentMatcher.group(REST_OF_LINE);
                    } else {
                        // If the regex matched but the capture group is null, then the comment is
                        // multi-line. Stop matching
                        // this line.
                        insideComment = true;
                        break;
                    }
                }

                // If we're looking at an empty string or we're still inside a comment, just skip to
                // the next line.
                if (lineContents.isEmpty() || insideComment) {
                    continue;
                }

                // Once we know that this line isn't a comment or an empty string, we should inspect
                // it to see if it's
                // one of our desired VF tags.
                final Matcher vfTagMatcher = VF_TAG_PATTERN.matcher(lineContents);
                if (!vfTagMatcher.lookingAt()) {
                    // If we're not in a comment and this isn't one of our desired tags, then this
                    // file is not syntactically
                    // valid VF, and we should stop.
                    return;
                } else {
                    String tagContents = vfTagMatcher.group(REST_OF_LINE);
                    // ASSUMPTION: If the desired properties are present, we can find them by
                    // scanning a set number of
                    // lines afterwards instead of by looking for the actual close-point of the tag.
                    // If this is wrong, we may see false negatives as Apex classes that should have
                    // been scanned are skipped.
                    int i = 0;
                    while (i < MAX_TAG_LINES) {
                        processTagLine(tagContents);
                        i += 1;
                        // If there's a next line, use it. Otherwise, exit early.
                        if (scanner.hasNextLine()) {
                            tagContents = scanner.nextLine();
                        } else {
                            break;
                        }
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            // This should be impossible, since we've already checked that the file exists and is a
            // file. But, just in case,
            // we'll rethrow as an UnexpectedException
            throw new UnexpectedException(ex);
        }
    }

    private void processTagLine(String tagContents) {
        // Apply both regexes against the line.
        // ASSUMPTION: The desired attributes do not appear outside of the relevant tag, and are not
        // commented out.
        // If this is wrong, we may see performance degrade as Apex classes that don't need to be
        // scanned are included.
        Matcher controllerMatcher = CONTROLLER_CAPTURE_PATTERN.matcher(tagContents);
        Matcher extensionsMatcher = EXTENSIONS_CAPTURE_PATTERN.matcher(tagContents);

        if (controllerMatcher.find()) {
            addReferencedNames(controllerMatcher.group(APEX_CLASSES));
        }

        if (extensionsMatcher.find()) {
            addReferencedNames(extensionsMatcher.group(APEX_CLASSES));
        }
    }

    private void addReferencedNames(String concatenatedNames) {
        String[] names = concatenatedNames.split(",");
        for (String name : names) {
            collectedMetaInfo.add(name.trim());
        }
    }

    protected static final class LazyHolder {
        // Postpone initialization until first use.
        protected static final VisualForceHandlerImpl INSTANCE = new VisualForceHandlerImpl();
    }

    static VisualForceHandlerImpl getInstance() {
        return VisualForceHandlerImpl.LazyHolder.INSTANCE;
    }
}
