/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.renderers;

import java.io.IOException;
import java.util.Iterator;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.properties.StringProperty;



/**
 * Renderer to XML format.
 */
public class PolarisRenderer extends AbstractIncrementingRenderer {

    public static final String NAME = "polaris";
    public static boolean isFirstIssue = true;

    // TODO 7.0.0 use PropertyDescriptor<String> or something more specialized

    public static final StringProperty POL_STRIP_PATH = new StringProperty("strip-path",
            "File pathname prefix to be stripped.", "", 0);

    public static final StringProperty POL_VERSION = new StringProperty("version",
            "XML error format version, defaults to 1.", "1", 0);

    public static final StringProperty POL_FORMAT = new StringProperty("format",
            "XML format name, defaults to ''.", "", 0);

    private String stripPathPrefic = "";

    public PolarisRenderer() {
        super(NAME, "Polaris format for importing results.");

        definePropertyDescriptor(POL_VERSION);
        definePropertyDescriptor(POL_FORMAT);
        definePropertyDescriptor(POL_STRIP_PATH);

    }

    @Override
    public String defaultFileExtension() {
        return "xml";
    }

    @Override
    public void start() throws IOException {

        // Read the the strip path prefix once for each run.
        stripPathPrefic = getProperty(POL_STRIP_PATH);

        //Writer writer = getWriter();

        isFirstIssue = true;

    }

    protected String cleanupDoc(String doc) {
        return doc.trim()
                .replaceAll("(^\n)|(\n$)", "")
                .replaceAll("\n", " ")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    protected boolean renderViolation(RuleViolation violation, StringBuilder buffer) {
        boolean result = violation != null && buffer != null;
        if (result) {

            String pathName = violation.getFilename();

            if (!stripPathPrefic.isEmpty() && pathName.startsWith(stripPathPrefic)) {
                pathName = pathName.substring(stripPathPrefic.length());
            }

            if (!isFirstIssue) {
                buffer.append(",\n\t\t");
            }

            String domain = "STATIC_JAVA";
            String ruleLanguage = violation.getRule().getLanguage().getShortName();
            if ("Java".equals(ruleLanguage)) {

            } else {

            }
            isFirstIssue = false;

            buffer.append("<error>" + PMD.EOL);
            buffer.append("\t<domain>" + domain + "</domain>" + PMD.EOL);
            buffer.append("\t<lang>" + ruleLanguage + "</lang>" + PMD.EOL);
            buffer.append("\t<checker>PMD." + violation.getRule().getName() + "</checker>" + PMD.EOL);
            buffer.append("\t<type>" + violation.getRule().getName() + "</type>" + PMD.EOL);
            buffer.append("\t<subtype>" + violation.getRule().getName() + "</subtype>" + PMD.EOL);
            buffer.append("\t<file>" + pathName + "</file>" + PMD.EOL);
            buffer.append("\t<function>" + violation.getMethodName() + "</function>" + PMD.EOL);
            buffer.append("\t<score>" + "100" + "</score>" + PMD.EOL);
            buffer.append("\t<ordered>" + "true" + "</ordered>" + PMD.EOL);
            buffer.append("\t<event>" + PMD.EOL);
            buffer.append("\t\t<main>true</main>" + PMD.EOL);
            buffer.append("\t\t<tag>PMD Violation</tag>" + PMD.EOL);
            buffer.append("\t\t<description>" + cleanupDoc(violation.getDescription()) + "</description>" + PMD.EOL);
            buffer.append("\t\t<line>" + violation.getBeginLine() + "</line>" + PMD.EOL);
            buffer.append("\t\t<file>" + pathName + "</file>" + PMD.EOL);
            buffer.append("\t\t<linkUrl>" + violation.getRule().getExternalInfoUrl() + "</linkUrl>" + PMD.EOL);
            buffer.append("\t\t<linkText>PMD Doc</linkText>" + PMD.EOL);
            buffer.append("\t</event>" + PMD.EOL);
            buffer.append("</error>" + PMD.EOL);
        }
        return result;
    }

    @Override
    public void renderFileViolations(Iterator<RuleViolation> violations) throws IOException {
        //Writer writer = getWriter();
        StringBuilder buf = new StringBuilder(500);


        // rule violations
        while (violations.hasNext()) {
            buf.setLength(0);
            RuleViolation rv = violations.next();
            if (renderViolation(rv, buf)) {
            }
            writer.write(buf.toString());
        }
    }

    @Override
    public void end() throws IOException {
        //Writer writer = getWriter();

    }

}
