package org.wikipathways.covid;

import java.io.File;
import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.List;

import nl.unimaas.bigcat.wikipathways.curator.assertions.*;
import nl.unimaas.bigcat.wikipathways.curator.tests.*;
import nl.unimaas.bigcat.wikipathways.curator.SPARQLHelper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class CheckRDF {

    public static String getHashcode(String string) { return Integer.toHexString(string.hashCode()); }

    public static void main(String[] args) throws Exception {
        String wpFile   = args[0];
        String gpmlFile = wpFile.replace("wp/Human", "wp/gpml/Human");
        String sbmlFile = wpFile.replace("wp/Human", "sbml").replace(".ttl",".sbml");
        String notesFile = sbmlFile.replace(".sbml",".txt");
        String svgFile  = sbmlFile.replace(".sbml",".svg");
        String wpid     = wpFile.substring(9,wpFile.indexOf(".ttl"));
        System.out.println("<img style=\"float: right; width: 200px\" src=\"logo.png\" />");

        System.out.println("# WikiPathways " + wpid + "\n");
        System.out.println("* WikiPathways: [" + wpid + "](https://identifiers.org/wikipathways:" + wpid + ")");
        System.out.println("* Scholia: [" + wpid + "](https://scholia.toolforge.org/wikipathways/" + wpid + ")");
        System.out.println("* WPRDF file: [" + wpFile + "](../" + wpFile + ")");
        System.out.println("* GPMLRDF file: [" + gpmlFile + "](../" + gpmlFile + ")");
        System.out.println("* SBML file: [" + sbmlFile + "](../" + sbmlFile + ") ([SVG](../" + svgFile + ")) ([conversion notes](../" + notesFile + "))\n");
        List<IAssertion> assertions = new ArrayList<IAssertion>();
        Model loadedData = ModelFactory.createDefaultModel();
        loadedData.read(new FileInputStream(new File(wpFile)), "", "TURTLE");
        loadedData.read(new FileInputStream(new File(gpmlFile)), "", "TURTLE");

        SPARQLHelper helper = new SPARQLHelper(loadedData);
        assertions.addAll(GeneralTests.titlesShortEnough(helper)); // not all GeneralTests
        assertions.addAll(GeneralTests.weirdCharacterTitles(helper));
        assertions.addAll(GeneralTests.duplicateTitles(helper));
        assertions.addAll(GeneralTests.noTags(helper));
        assertions.addAll(GeneralTests.nullDataSources(helper));
        assertions.addAll(GeneralTests.undefinedDataSources(helper));
        assertions.addAll(GeneralTests.undefinedIdentifier(helper));
        assertions.addAll(GeneralTests.dataNodeWithoutGraphId(helper));
        assertions.addAll(GeneralTests.groupsHaveDetail(helper));
        assertions.addAll(GeneralTests.emptyLabelOfNodeWithIdentifier(helper));
        assertions.addAll(DataNodesTests.all(helper));
        assertions.addAll(PathwayTests.all(helper));
        assertions.addAll(ReferencesTests.all(helper));
	assertions.addAll(WikidataTests.keggWithoutMapping(helper)); // not all
	assertions.addAll(WikidataTests.pubchemCIDWithoutMapping(helper));
	assertions.addAll(WikidataTests.hmdbWithoutMapping(helper));
	assertions.addAll(WikidataTests.casWithoutMapping(helper));
	assertions.addAll(WikidataTests.wikDataTypo(helper));
	assertions.addAll(WikidataTests.duplicateWikidataMappings(helper));
	assertions.addAll(WikidataTests.wikidataIdentifiersWrong(helper));
	assertions.addAll(WikidataTests.chemspiderCIDWithoutMapping(helper));
        assertions.addAll(OutdatedDataSourcesTests.outdatedUniprot(helper));
        assertions.addAll(OutdatedDataSourcesTests.outdatedUniprot2(helper));
        assertions.addAll(OutdatedDataSourcesTests.outdatedUniprot3(helper));
        assertions.addAll(OutdatedDataSourcesTests.outdatedUniprot4(helper));
        assertions.addAll(OutdatedDataSourcesTests.oldUniprotSwissProt(helper));

        assertions.addAll(CovidDiseaseMapsTests.all(helper));

        assertions.addAll(GeneTests.all(helper));
        assertions.addAll(EnsemblTests.wrongEnsemblIDForHumanSpecies(helper)); // exclude outdatedIdentifiers (not all pathways have many genes)
        assertions.addAll(EnsemblTests.wrongEnsemblIDForRatSpecies(helper));
        assertions.addAll(EnsemblTests.wrongEnsemblIDForCowSpecies(helper));
        assertions.addAll(EnsemblTests.wrongEnsemblIDForMouseSpecies(helper));
        assertions.addAll(ProteinsTests.all(helper));
        assertions.addAll(UniProtTests.all(helper));

        assertions.addAll(CASMetabolitesTests.all(helper));
        assertions.addAll(ChEBIMetabolitesTests.all(helper));
        assertions.addAll(ChemSpiderTests.all(helper));
        assertions.addAll(HMDBMetabolitesTests.outdatedIdentifiers(helper)); // not all
        assertions.addAll(HMDBSecMetabolitesTests.all(helper));
        assertions.addAll(KEGGMetaboliteTests.all(helper));
        assertions.addAll(LIPIDMAPSTests.all(helper));
        assertions.addAll(MetabolitesTests.all(helper));
        assertions.addAll(MetaboliteStructureTests.all(helper));
        assertions.addAll(PubChemMetabolitesTests.all(helper));

        assertions.addAll(InteractionTests.all(helper));

        System.out.println("## Tests");

        List<IAssertion> failedAssertions = new ArrayList<IAssertion>();
        int testClasses = 0;
        int testClassTests = 0;
        int tests = 0;
        String currentTestClass = "";
        String currentTest = "";
        boolean currentTestClassHasFails = false;
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
                    System.out.println(currentTestClassMessages + message);
                  } else {
                    System.out.println(": all " + testClassTests + " tests OK!");
                  }
                }

                // reset properties for new test
                currentTestClass = assertion.getTestClass();
                currentTest = "";
                testClasses++;
                currentTestClassMessages = "";
                message = "";
                testClassTests = 0;
                System.out.print("* " + currentTestClass);
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
          System.out.println(currentTestClassMessages + message);
        } else {
          System.out.println(": all " + testClassTests + " tests OK!");
        }

        System.out.println();
        System.out.println("\n## Summary\n");
        System.out.println("* Number of test classes: " + testClasses);
        System.out.println("* Number of tests: " + tests);
        System.out.println("* Number of assertions: " + assertions.size());
        System.out.println("* Number of fails: " + failedAssertions.size());

        System.out.println("\n## Fails\n");
        for (IAssertion assertion : failedAssertions) {
            System.out.println("<a name=\"" + getHashcode(assertion.getTestClass() + assertion.getTest() + assertion.getMessage()) + "\" />\n");
            System.out.println("## " + assertion.getTestClass() + "." + assertion.getTest());
            System.out.println("\n" + assertion.getMessage());
            if (assertion.getDetails() != null && !assertion.getDetails().isEmpty()) {
                System.out.println("```\n" + assertion.getDetails() + "```\n");
                if (assertion.hasLinkToDocs()) {
                    System.out.println("More details at [" + assertion.getLinkToDocs() + "](" + assertion.getLinkToDocs() + ")\n");
                }
            }
        }
    }

}
