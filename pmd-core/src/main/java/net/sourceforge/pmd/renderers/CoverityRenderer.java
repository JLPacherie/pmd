/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.renderers;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.properties.StringProperty;



/**
 * Renderer to XML format.
 */
public class CoverityRenderer extends AbstractIncrementingRenderer {

    public static final String NAME = "coverity";
    public static boolean isFirstIssue = true;

    protected static List<String> fileList = new ArrayList<>();

    // TODO 7.0.0 use PropertyDescriptor<String> or something more specialized

    public static final StringProperty COV_VERSION = new StringProperty("version",
            "JSON format version, defaults to 1.", "1", 0);

    public static final StringProperty COV_FORMAT = new StringProperty("format",
            "JSON format name, defaults to 'cov-import-results input'.", "cov-import-results input", 0);

    public CoverityRenderer() {
        super(NAME, "Coverity format for importing results.");

        definePropertyDescriptor(COV_VERSION);
        definePropertyDescriptor(COV_FORMAT);

    }

    @Override
    public String defaultFileExtension() {
        return "json";
    }

    @Override
    public void start() throws IOException {

        fileList.clear();

        Writer writer = getWriter();
        StringBuilder buf = new StringBuilder(500);

        buf.append("\n"
                + "{\n"
                + "\t\"header\" : {\n"
                + "\t\t\"version\" : " + getProperty(COV_VERSION) + "," + PMD.EOL
                + "\t\t\"format\" : \"" + getProperty(COV_FORMAT) + "\"" + PMD.EOL
                + "\t},\n"
                + "\t\n"
                + "\t\"issues\": [\n");

        buf.append(PMD.EOL);

        writer.write(buf.toString());

        isFirstIssue = true;

    }

    protected String cleanupDoc(String doc) {
        return doc.trim()
                .replaceAll("(^\n)|(\n$)", "")
                .replaceAll("\n", " ");
    }

    protected boolean renderViolation(RuleViolation violation, StringBuilder buffer) {
        boolean result = violation != null && buffer != null;
        if (result) {

            if (!isFirstIssue) {
                buffer.append("\n\t\t,");
            }

            isFirstIssue = false;

            buffer.append("\t\t{" + PMD.EOL);
            buffer.append("\t\t\t\"checker\": \"PMD." + violation.getRule().getName() + "\"," + PMD.EOL);
            buffer.append("\t\t\t\"extra\": \"PMD violations\"," + PMD.EOL);
            buffer.append("\t\t\t\"file\": \"" + violation.getFilename() + "\"," + PMD.EOL);
            buffer.append("\t\t\t\"function\": \"" + violation.getMethodName() + "\"," + PMD.EOL);
            buffer.append("\t\t\t\"subcategory\": \"" + "code_quality" + "\"," + PMD.EOL);
            buffer.append("\t\t\t\"properties\": {" + PMD.EOL);
            buffer.append("\t\t\t  \"type\": \"Code maintainability issues\"," + PMD.EOL);
            buffer.append("\t\t\t  \"impact\": \"low\"," + PMD.EOL);
            buffer.append("\t\t\t  \"longDescription\": \"" + cleanupDoc(violation.getDescription()) + "\"," + PMD.EOL);
            buffer.append("\t\t\t  \"localEffect\": \"Hard to maintain function\"," + PMD.EOL);
            buffer.append("\t\t\t  \"issueKind\": \"QUALITY\"," + PMD.EOL);
            buffer.append("\t\t\t  \"events\": [ " + PMD.EOL);
            buffer.append("\t\t\t    {" + PMD.EOL);
            buffer.append("\t\t\t      \"tag\": \"PMD quality violation\"," + PMD.EOL);
            buffer.append("\t\t\t      \"file\": \"" + violation.getFilename() + "\"," + PMD.EOL);
            buffer.append("\t\t\t      \"linkUrl\": \"" + violation.getRule().getExternalInfoUrl() + "\"," + PMD.EOL);
            buffer.append("\t\t\t      \"linkText\": \"PMD Doc\"," + PMD.EOL);
            buffer.append("\t\t\t      \"description\": \"" + cleanupDoc(violation.getRule().getDescription()) + "\"," + PMD.EOL);
            buffer.append("\t\t\t      \"line\": " + violation.getBeginLine() + "," + PMD.EOL);
            buffer.append("\t\t\t      \"main\": true" + PMD.EOL);
            buffer.append("\t\t\t    }" + PMD.EOL);
            buffer.append("\t\t\t  ]" + PMD.EOL);
            buffer.append("\t\t\t }" + PMD.EOL);
            buffer.append("\t\t}");
        }
        return result;
    }

    @Override
    public void renderFileViolations(Iterator<RuleViolation> violations) throws IOException {
        Writer writer = getWriter();
        StringBuilder buf = new StringBuilder(500);


        // rule violations
        while (violations.hasNext()) {
            buf.setLength(0);
            RuleViolation rv = violations.next();
            if (renderViolation(rv, buf)) {
                if (!((ArrayList) fileList).contains(rv.getFilename())) {
                    fileList.add(rv.getFilename());
                }
            }
            writer.write(buf.toString());
        }
    }

    @Override
    public void end() throws IOException {
        Writer writer = getWriter();

        // End of JSON Array of defect.
        writer.write("]," + PMD.EOL);

        // Beginning of JSON Array of file
        writer.write("\n\t\"sources\": [" + PMD.EOL);

        writer.write(fileList.stream()
                .map(f -> "\t\t\t\"" + f + "\"")
                .collect(joining("," + PMD.EOL)));

        writer.write("\n\t]\n}\n");
    }

}
