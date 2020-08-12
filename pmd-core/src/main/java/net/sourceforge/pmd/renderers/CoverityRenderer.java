/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.renderers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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

    public static final StringProperty COV_STRIP_PATH = new StringProperty("strip-path",
            "File pathname prefix to be stripped.", "", 0);

    public static final StringProperty COV_VERSION = new StringProperty("version",
            "JSON format version, defaults to 1.", "1", 0);

    public static final StringProperty COV_FORMAT = new StringProperty("format",
            "JSON format name, defaults to 'cov-import-results input'.", "cov-import-results input", 0);

    private String stripPathPrefic = "";

    public CoverityRenderer() {
        super(NAME, "Coverity format for importing results.");

        definePropertyDescriptor(COV_VERSION);
        definePropertyDescriptor(COV_FORMAT);
        definePropertyDescriptor(COV_STRIP_PATH);

    }

    @Override
    public String defaultFileExtension() {
        return "json";
    }

    @Override
    public void start() throws IOException {

        // Read the the strip path prefix once for each run.
        stripPathPrefic = getProperty(COV_STRIP_PATH);

        // Clear the list of files with exported defects
        fileList.clear();

        //Writer writer = getWriter();
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
                .replaceAll("\n", " ")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    protected boolean renderViolation(RuleViolation violation, StringBuilder buffer) {
        boolean result = violation != null && buffer != null;
        if (result) {

            String pathName = violation.getFilename();
            String ruleSet = violation.getRule().getRuleSetName();
            String priority = Integer.toString(violation.getRule().getPriority().getPriority());
            String pCategory;
            String pSubCategory;
            String pImpact;
            String pIssueKind;
            String pLocalEffect;
            String pSec;


            if (!stripPathPrefic.isEmpty() && pathName.startsWith(stripPathPrefic)) {
                pathName = pathName.substring(stripPathPrefic.length());
            }

            if (!isFirstIssue) {
                buffer.append(",\n\t\t");
            }

            // Map PMD Rule priority to Coverity issue impact
            // Priority 1-2 : High
            // Priority 3   : Medium
            // Priority 4-5 : Low
            if (priority.matches("1|2")) {
                pImpact = "high";
                pSec = "High";
            } else if (priority.matches("3")) {
                pImpact = "medium";
                pSec = "Medium";
            } else {
                pImpact = "low";
                pSec = "Low";
            }

            // Map PMD RuleSet to Coverity Quality or Security defects
            if (ruleSet.contains("Security")) {
                pCategory = pSec + " impact security";
                pSubCategory = "none";
                pIssueKind = "SECURITY";
                pLocalEffect = "A potential security flaw that could be exploited to compromise the confidentiality, integrity or availability of the application";
            } else if (ruleSet.contains("Best Practices")) {
                pCategory = "Best pratices";
                pSubCategory = "none";
                pIssueKind = "QUALITY";
                pLocalEffect = "Issue with generally accepted coding best practices";
            } else if (ruleSet.contains("Error Prone")) {
                pCategory = "Error prone";
                pSubCategory = "none";
                pIssueKind = "QUALITY";
                pLocalEffect = "Construct that is either broken, extremely confusing or prone to runtime errors";
            } else if (ruleSet.contains("Design")) {
                pCategory = "Design";
                pSubCategory = "none";
                pIssueKind = "QUALITY";
                pLocalEffect = "Design flaw";
            } else if (ruleSet.contains("Documentation")) {
                pCategory = "Documentation";
                pSubCategory = "none";
                pIssueKind = "QUALITY";
                pLocalEffect = "Documentation issue";
            } else if (ruleSet.contains("Performance")) {
                pCategory = "Performance";
                pSubCategory = "none";
                pIssueKind = "QUALITY";
                pLocalEffect = "Suboptimal code which may have a negative impact on the performance of the application";
            } else if (ruleSet.contains("Multithreading")) {
                pCategory = "Multithreading";
                pSubCategory = "none";
                pIssueKind = "QUALITY";
                pLocalEffect = "Issue dealing with multiple threads of execution";
            } else if (ruleSet.contains("Code Style")) {
                pCategory = "Code style";
                pSubCategory = "none";
                pIssueKind = "QUALITY";
                pLocalEffect = "Issue in coding style";
            } else {
                pCategory = "Code quality";
                pSubCategory = "none";
                pIssueKind = "QUALITY";
                pLocalEffect = "Potential issue";
            }

            // (APEX) Elevate SOSQL, XSS and OpenRedirect rules to High Impact Security
            if (violation.getRule().getName().matches("ApexSOQLInjection|ApexXSSFromEscapeFalse|ApexXSSFromURLParam|ApexOpenRedirect")) {
                pCategory = "High impact security";
                pSec = "High";
                pImpact = "high";
                pIssueKind = "SECURITY";
                pSubCategory = "none";
                pLocalEffect = "A potential serious security flaw that could be exploited to compromise the confidentiality, integrity and availability of the application";
            }

            isFirstIssue = false;

            buffer.append("{" + PMD.EOL);
            buffer.append("\t\t\t\"checker\": \"PMD." + violation.getRule().getName() + "\"," + PMD.EOL);
            buffer.append("\t\t\t\"extra\": \"PMD violations\"," + PMD.EOL);
            buffer.append("\t\t\t\"file\": \"" + violation.getFilename() + "\"," + PMD.EOL);
            buffer.append("\t\t\t\"function\": \"" + violation.getMethodName() + "\"," + PMD.EOL);
            buffer.append("\t\t\t\"subcategory\": \"" + pSubCategory + "\"," + PMD.EOL);
            buffer.append("\t\t\t\"properties\": {" + PMD.EOL);
            buffer.append("\t\t\t  \"type\": \"" + violation.getRule().getName() + "\"," + PMD.EOL);
            buffer.append("\t\t\t  \"category\": \"" + pCategory + "\"," + PMD.EOL);
            buffer.append("\t\t\t  \"impact\": \"" + pImpact + "\"," + PMD.EOL);
            buffer.append("\t\t\t  \"longDescription\": \"" + cleanupDoc(violation.getDescription()) + "\"," + PMD.EOL);
            buffer.append("\t\t\t  \"localEffect\": \"" + pLocalEffect + "\"," + PMD.EOL);
            buffer.append("\t\t\t  \"issueKind\": \"" + pIssueKind + "\"" + PMD.EOL);
            buffer.append("\t\t\t }," + PMD.EOL);
            buffer.append("\t\t\t\"events\": [ " + PMD.EOL);
            buffer.append("\t\t\t    {" + PMD.EOL);
            buffer.append("\t\t\t      \"tag\": \"PMD violation\"," + PMD.EOL);
            buffer.append("\t\t\t      \"file\": \"" + pathName + "\"," + PMD.EOL);
            buffer.append("\t\t\t      \"linkUrl\": \"" + violation.getRule().getExternalInfoUrl() + "\"," + PMD.EOL);
            buffer.append("\t\t\t      \"linkText\": \"PMD Doc\"," + PMD.EOL);
            buffer.append("\t\t\t      \"description\": \"" + cleanupDoc(violation.getRule().getDescription()) + "\"," + PMD.EOL);
            buffer.append("\t\t\t      \"line\": " + violation.getBeginLine() + "," + PMD.EOL);
            buffer.append("\t\t\t      \"main\":  true" + PMD.EOL);
            buffer.append("\t\t\t    }" + PMD.EOL);
            buffer.append("\t\t\t ]" + PMD.EOL);
            buffer.append("\t\t}");
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
                if (!((ArrayList) fileList).contains(rv.getFilename())) {
                    fileList.add(rv.getFilename());
                }
            }
            writer.write(buf.toString());
        }
    }

    @Override
    public void end() throws IOException {
        //Writer writer = getWriter();

        // End of JSON Array of defect.
        writer.write("]," + PMD.EOL);

        // Beginning of JSON Array of file
        writer.write("\n\t\"sources\": [" + PMD.EOL);

        writer.write(fileList.stream()
                .map(f -> "\t\t\t{ \"file\": \"" + f + "\", \"encoding\": \"ASCII\" }")
                .collect(Collectors.joining("," + PMD.EOL)));

        writer.write("\n\t]\n}\n");
    }

}
