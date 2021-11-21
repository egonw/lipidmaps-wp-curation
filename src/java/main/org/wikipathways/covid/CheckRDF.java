package org.wikipathways.covid;

import java.io.File;
import java.io.FileInputStream;
import java.io.Writer;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import nl.unimaas.bigcat.wikipathways.curator.assertions.*;
import nl.unimaas.bigcat.wikipathways.curator.SPARQLHelper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class CheckRDF {

    public static String getHashcode(String string) { return Integer.toHexString(string.hashCode()); }

    public static void main(String[] args) throws Exception {
        String wpFile     = args[0];
        String reportFile = args[1];
        String gpmlFile = wpFile.replace("wp/Human", "wp/gpml/Human");
        String sbmlFile = wpFile.replace("wp/Human", "sbml").replace(".ttl",".sbml");
        String notesFile = sbmlFile.replace(".sbml",".txt");
        String svgFile  = sbmlFile.replace(".sbml",".svg");
        String wpid     = wpFile.substring(9,wpFile.indexOf(".ttl"));

        PrintWriter report = new PrintWriter(reportFile);
        PrintWriter reportStatus = new PrintWriter(reportFile.replace(".md",".txt"));
        PrintWriter reportJSON = new PrintWriter(reportFile.replace(".md",".json"));

        report.println("<img style=\"float: right; width: 200px\" src=\"../logo.png\" />");

        report.println("# WikiPathways " + wpid + "\n");
        report.println("* WikiPathways: [" + wpid + "](https://identifiers.org/wikipathways:" + wpid + ")");
        report.println("* Scholia: [" + wpid + "](https://scholia.toolforge.org/wikipathways/" + wpid + ")");
        report.println("* WPRDF file: [" + wpFile + "](../" + wpFile + ")");
        report.println("* GPMLRDF file: [" + gpmlFile + "](../" + gpmlFile + ")");
        report.println("* SBML file: [" + sbmlFile + "](../" + sbmlFile + ") ([SVG](../" + svgFile + ")) ([conversion notes](../" + notesFile + "))\n");
        List<IAssertion> assertions = new ArrayList<IAssertion>();
        Model loadedData = ModelFactory.createDefaultModel();
        loadedData.read(new FileInputStream(new File(wpFile)), "", "TURTLE");
        loadedData.read(new FileInputStream(new File(gpmlFile)), "", "TURTLE");

        SPARQLHelper helper = new SPARQLHelper(loadedData);

        Scanner testConfigFile = new Scanner(new FileInputStream("tests.txt"));
        while (testConfigFile.hasNextLine()) {
            String testConfig = testConfigFile.nextLine().trim();
            if (!testConfig.startsWith("#")) { // comment lines start with #
                String[] config = testConfig.split("\\.");
                Class testClass = Class.forName("nl.unimaas.bigcat.wikipathways.curator.tests." + config[0]);
                assertions.addAll((List<IAssertion>)testClass.getDeclaredMethod(config[1], SPARQLHelper.class).invoke(null, helper));
            }
        }

        report.println("## Tests");

        List<IAssertion> failedAssertions = new ArrayList<IAssertion>();
        int testClasses = 0;
        int testClassTests = 0;
        int tests = 0;
        String currentTestClass = "";
        String currentTest = "";
        boolean currentTestClassHasFails = false;
        boolean anyTestClassHasFails = false;
        String currentTestClassMessages = "";
        String message = "";
        String errors = "";
        int assertionsFailed = 0;
        for (IAssertion assertion : assertions) {
            if (assertion.getTestClass() != currentTestClass) {
                // is there report output to finish?
                if (testClasses > 0) {
                  // wrap up last test of the previous test class
                  if (assertionsFailed == 0) { message += " all OK!"; } else { message += " we found " + assertionsFailed + " problem(s):"; }
                  if (!errors.isEmpty()) message += "\n" + errors;

                  // only output results when there are fails
                  if (currentTestClassHasFails) {
                    report.println(currentTestClassMessages + message);
                    anyTestClassHasFails = true;
                  } else {
                    report.println(": all " + testClassTests + " tests OK!");
                  }
                }

                // reset properties for new test
                currentTestClass = assertion.getTestClass();
                currentTest = "";
                testClasses++;
                currentTestClassMessages = "";
                message = "";
                testClassTests = 0;
                report.print("* " + currentTestClass);
                currentTestClassHasFails = false;
                currentTestClassMessages = "";
            }

            // new test ?
            if (assertion.getTest() != currentTest) {
                currentTest = assertion.getTest();
                if (!message.isEmpty()) {
                  if (assertionsFailed == 0) { message += " all OK!"; } else { message += " we found " + assertionsFailed + " problem(s):"; }
                  if (!errors.isEmpty()) message += "\n" + errors;
                  currentTestClassMessages += message;
                }
                message = "\n    * " + currentTest + ": ";
                assertionsFailed = 0;
                errors = "";
                tests++;
                testClassTests++;
            }

            if (assertion instanceof AssertEquals) {
                AssertEquals typedAssertion = (AssertEquals)assertion;
                if (!typedAssertion.getExpectedValue().equals(typedAssertion.getValue())) {
                   message += "x";
                   assertionsFailed++;
                   errors += "        * [" + typedAssertion.getMessage() + "](#" + getHashcode(assertion.getTestClass() + assertion.getTest() + assertion.getMessage()) + ")";
                   failedAssertions.add(assertion);
                   currentTestClassHasFails = true;
                } else {
                    message += ".";
                }
            } else if (assertion instanceof AssertNotSame) {
                AssertNotSame typedAssertion = (AssertNotSame)assertion;
                if (typedAssertion.getExpectedValue().equals(typedAssertion.getValue())) {
                   message += "x";
                   assertionsFailed++;
                   errors += "        * [" + typedAssertion.getMessage() + "](#" + getHashcode(assertion.getTestClass() + assertion.getTest() + assertion.getMessage()) + ")";
                   failedAssertions.add(assertion);
                   currentTestClassHasFails = true;
                } else {
                    message += ".";
                }
            } else if (assertion instanceof AssertNotNull) {
                AssertNotNull typedAssertion = (AssertNotNull)assertion;
                if (typedAssertion.getValue() == null) {
                   message += "x";
                   assertionsFailed++;
                   errors += "            * Unexpected null found";
                   failedAssertions.add(assertion);
                   currentTestClassHasFails = true;
                } else {
                    message += ".";
                }
            } else if (assertion instanceof AssertTrue) {
                AssertTrue typedAssertion = (AssertTrue)assertion;
                if ((boolean)typedAssertion.getValue()) {
                   message += "x";
                   assertionsFailed++;
                   errors += "            * Expected true but found false";
                   failedAssertions.add(assertion);
                   currentTestClassHasFails = true;
                } else {
                    message += ".";
                }
            } else {
                message += "?";
                errors += "        * Unrecognized assertion type: " + assertion.getClass().getName();
                assertionsFailed++;
                failedAssertions.add(assertion);
                currentTestClassHasFails = true;
            }
        }
        // any test results remaining?
        if (!message.isEmpty()) {
          if (assertionsFailed == 0) { message += " all OK!"; } else { message += " we found " + assertionsFailed + " problem(s):"; }
          if (!errors.isEmpty()) message += "\n" + errors;
        }
        if (currentTestClassHasFails) {
          report.println(currentTestClassMessages + message);
          anyTestClassHasFails = true;
        } else {
          report.println(": all " + testClassTests + " tests OK!");
        }

        report.println();
        report.println("\n## Summary\n");
        report.println("* Number of test classes: " + testClasses);
        report.println("* Number of tests: " + tests);
        report.println("* Number of assertions: " + assertions.size());
        report.println("* Number of fails: " + failedAssertions.size());

        report.println("\n## Fails\n");
        for (IAssertion assertion : failedAssertions) {
            report.println("<a name=\"" + getHashcode(assertion.getTestClass() + assertion.getTest() + assertion.getMessage()) + "\" />\n");
            report.println("## " + assertion.getTestClass() + "." + assertion.getTest());
            report.println("\n" + assertion.getMessage());
            if (assertion.getDetails() != null && !assertion.getDetails().isEmpty()) {
                report.println("```\n" + assertion.getDetails() + "```\n");
                if (assertion.hasLinkToDocs()) {
                    report.println("More details at [" + assertion.getLinkToDocs() + "](" + assertion.getLinkToDocs() + ")\n");
                }
            }
        }
        reportJSON.println("{");
        reportJSON.println("  \"schemaVersion\": 1,");
        reportJSON.println("  \"label\": \"curation\",");
        if (anyTestClassHasFails) {
          reportStatus.println("status=⨯");
          reportJSON.println("  \"message\": \"" + failedAssertions.size() + " errors\",");
          reportJSON.println("  \"color\": \"red\"");
        } else {
          reportStatus.println("status=✓");
          reportJSON.println("  \"message\": \"success\",");
          reportJSON.println("  \"color\": \"green\"");
        }
        reportJSON.println("}");

        report.flush(); report.close();
        reportStatus.flush(); reportStatus.close();
        reportJSON.flush(); reportJSON.close();
    }

}
