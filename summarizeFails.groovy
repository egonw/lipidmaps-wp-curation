import java.util.ArrayList
import java.util.List
import java.util.Map
import java.util.HashMap
import java.util.HashSet

import static groovy.io.FileType.FILES

Map testsWithDescription = new HashMap<>();

Map<String,List> failedTests = new HashMap<>();

def dir = new File("reports");
def files = [];
dir.traverse(type: FILES, maxDepth: 0) {
  if (it.name.endsWith(".md") && !it.name.endsWith(".ng.md")) {
    files.add(it)
  }
};

files.each { file ->
  wpid = file.name.split(".md")[0]
  boolean fails = false
  file.eachLine { line ->
     if (line.startsWith("## Fails")) {
       fails = true;
     } else if (fails == true) {
       if (line.startsWith("## ")) {
         failedTest = line.substring(3)
         pathways = failedTests.get(failedTest)
         if (pathways == null) pathways = new ArrayList()
         pathways.add(wpid)
         failedTests.put(failedTest, pathways)
       } else if (line.startsWith("More details at [https://www.wikipathways.org/WikiPathwaysCurator/")) {
         link = line.substring(line.indexOf('[') + 1, line.indexOf(']'))
         link = link.replace("https://www.wikipathways.org/WikiPathwaysCurator/", "")
         testsWithDescription.put(failedTest, link)
       }
     }
  }
}

println "<img style=\"float: right; width: 200px\" src=\"https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Wplogo_with_text_500.png/640px-Wplogo_with_text_500.png\" />"
println "# Validation Reports\n"

failedTests.keySet().sort().each { key ->
  testname = key
  println "## ${testname}\n"
  if (testsWithDescription.containsKey(key)) {
    descriptionURL = "https://www.wikipathways.org/WikiPathwaysCurator/" + testsWithDescription.get(key)
    println "\nRead more about why these fails happen and how to fix them in these [instructions](${descriptionURL}).\n"
  }

  failedTests.get(key).sort().each { pathway ->
    hashcode = key.replace(".", "").replace(" ","-").toLowerCase()
    print "[${pathway}](reports/${pathway}#${hashcode}) "
  }
  println "\n"
}
