package org.wikipathways.curator;

import java.io.File;
import java.io.FileInputStream;
import java.io.Writer;
import java.io.PrintWriter;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import nl.unimaas.bigcat.wikipathways.curator.assertions.*;
import nl.unimaas.bigcat.wikipathways.curator.SPARQLHelper;
import nl.unimaas.bigcat.wikipathways.curator.StringMatrix;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class CheckRDF {

    public static String getHashcode(String string) { return Integer.toHexString(string.hashCode()); }

    public static void main(String[] args) throws Exception {
        String wpFile     = args[0];
        String reportFile = args[1];
        String sbmlFile = wpFile.replace("wp/Human", "sbml").replace(".ttl",".sbml");
        String notesFile = sbmlFile.replace(".sbml",".txt");
        String svgFile  = sbmlFile.replace(".sbml",".svg");
        String gpmlFile = wpFile.replace("wp/Human", "wp/gpml/Human");
        String wpid     = wpFile.substring(9,wpFile.indexOf(".ttl"));

        PrintWriter report = new PrintWriter(reportFile);
        PrintWriter reportStatus = new PrintWriter(reportFile.replace(".md",".txt"));
        PrintWriter reportJSON = new PrintWriter(reportFile.replace(".md",".json"));

        report.println("<img style=\"float: right; width: 200px\" src=\"https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Wplogo_with_text_500.png/640px-Wplogo_with_text_500.png\" />");

        List<IAssertion> assertions = new ArrayList<IAssertion>();
        Model loadedData = ModelFactory.createDefaultModel();
        loadedData.read(new FileInputStream(new File(wpFile)), "", "TURTLE");
        loadedData.read(new FileInputStream(new File(gpmlFile)), "", "TURTLE");

        SPARQLHelper helper = new SPARQLHelper(loadedData);

        // determind the species of the pathway
        StringMatrix results = helper.sparql("PREFIX wp: <http://vocabularies.wikipathways.org/wp#> SELECT DISTINCT ?species WHERE { ?pathway wp:organismName ?species }");
        String species = results.getColumn("species").get(0);
        boolean isHuman = "Homo sapiens".equals(species.trim());

        report.println("# WikiPathways " + wpid + "\n");
        report.println("* WikiPathways: [" + wpid + "](https://wikipathways.org/pathways/" + wpid + ") ([classic](https://classic.wikipathways.org/instance/" + wpid + "))");
        report.println("* Species: " + species);
        if (isHuman) report.println("* Scholia: [" + wpid + "](https://scholia.toolforge.org/wikipathways/" + wpid + ")");
        report.println("* WPRDF file: [" + wpFile + "](../" + wpFile + ")");
        report.println("* GPMLRDF file: [" + gpmlFile + "](../" + gpmlFile + ")");
        report.println("* SBML file: [" + sbmlFile + "](../" + sbmlFile + ") ([SVG](../" + svgFile + ")) ([conversion notes](../" + notesFile + "))\n");

        Scanner testConfigFile = new Scanner(new FileInputStream("tests.txt"));
        while (testConfigFile.hasNextLine()) {
            String testConfig = testConfigFile.nextLine().trim();
            if (!testConfig.startsWith("#")) { // comment lines start with #
                String[] config = testConfig.split("\\.");
                Class testClass = Class.forName("nl.unimaas.bigcat.wikipathways.curator.tests." + config[0]);
                Method declaredMethod = null;
                try {
                    declaredMethod = testClass.getDeclaredMethod(config[1], SPARQLHelper.class, String.class);
                    assertions.addAll((List<IAssertion>)declaredMethod.invoke(null, helper, "text/markdown"));
                } catch (NoSuchMethodException exception) {
                    // try the old method, without the format parameter
                    declaredMethod = testClass.getDeclaredMethod(config[1], SPARQLHelper.class);
                    assertions.addAll((List<IAssertion>)declaredMethod.invoke(null, helper));
                }
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
            if (assertion.getTest().getClassName() != currentTestClass) {
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
                currentTestClass = assertion.getTest().getClassName();
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
            if (assertion.getTest().getTestName() != currentTest) {
                currentTest = assertion.getTest().getTestName();
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
                   errors += "        * [" + typedAssertion.getMessage() + "](#" + getHashcode(assertion.getTest().getClassName() + assertion.getTest().getTestName() + assertion.getMessage()) + ")";
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
                   errors += "        * [" + typedAssertion.getMessage() + "](#" + getHashcode(assertion.getTest().getClassName() + assertion.getTest().getTestName() + assertion.getMessage()) + ")";
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
                if (!(boolean)typedAssertion.getValue()) {
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
            report.println("<a name=\"" + getHashcode(assertion.getTest().getClassName() + assertion.getTest().getTestName() + assertion.getMessage()) + "\" />\n");
            report.println("## " + assertion.getTest().getTitle());
            report.println("\n" + assertion.getMessage());
            if (assertion.getDetails() != null && !assertion.getDetails().isEmpty()) {
                if ("text/markdown".equals(assertion.getDetailsFormat())) {
                  report.println("\n" + assertion.getDetails() + "\n");
                } else {
                  report.println("```\n" + assertion.getDetails() + "```\n");
                }
                if (assertion.getTest().hasLinkToDocs()) {
                    report.println("More details at [" + assertion.getTest().getLinkToDocs() + "](" + assertion.getTest().getLinkToDocs() + ")\n");
                }
            }
        }
        reportJSON.println("{");
        reportJSON.println("  \"schemaVersion\": 1,");
        reportJSON.println("  \"label\": \"curation\",");
        double ratio = (double)failedAssertions.size() / (double)assertions.size();
        // reportJSON.println("  \"ratio\": \"" + ratio + "\",");
        if (anyTestClassHasFails) {
          String color = "yellow";
          if (ratio > 0.05) { color = "red"; }
          else if  (ratio > 0.01) { color = "orange"; };
          reportStatus.println("status=⨯");
          reportJSON.println("  \"message\": \"" +
            (failedAssertions.size() == 1
               ? "1 issue"
               : failedAssertions.size() + " issues")
            + "\",");
          reportJSON.println("  \"color\": \"" + color + "\"");
        } else {
          reportStatus.println("status=✓");
          reportJSON.println("  \"message\": \"success\",");
          reportJSON.println("  \"color\": \"brightgreen\"");
        }
        reportJSON.println("}");

        report.flush(); report.close();
        reportStatus.flush(); reportStatus.close();
        reportJSON.flush(); reportJSON.close();
    }

}
