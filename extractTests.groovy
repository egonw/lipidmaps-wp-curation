/* Run the following before this class:

 jar tf libs/wikipathways.curator-1-SNAPSHOT.jar | grep '.class$' \
   | grep 'nl.unimaas.bigcat.wikipathways.curator.tests' | tr / . \
   | sed 's/\.class$//' | xargs javap -public -cp libs/wikipathways.curator-1-SNAPSHOT.jar \
   > tests.tmp 

*/

import java.util.*

// read the old data
Map<String, Map<String,Boolean>> oldClasses = new HashMap()
new File("tests.txt").eachLine() { line ->
  commentedOut = line.startsWith("#")
  if (commentedOut) line = line.substring(1)
  if (line.endsWith(".all")) {
    clazz = line.split("\\.")[0]
    if (!oldClasses.containsKey(clazz)) {
      methods = new HashMap()
      methods.put("all", commentedOut)
      oldClasses.put(clazz, methods)
    }
  } else {
    clazz = line.split("\\.")[0]
    method = line.split("\\.")[1]
    oldClasses.get(clazz).put(method, Boolean.valueOf(commentedOut))
  }
}

// read the new data
Map<String, List<String>> testClasses = new HashMap()
new File("tests.tmp").eachLine() { line ->
  if (line.startsWith("public class nl.unimaas.bigcat.wikipathways.curator.tests.")) {
    clazz = line.substring(58).split(" ")[0]
    if (!testClasses.containsKey(clazz)) {
      testClasses.put(clazz, new ArrayList())
    }
  } else if (line.startsWith("  public static java.util.List<nl.unimaas.bigcat.wikipathways.curator.assertions.IAssertion>")) {
    method = line.split(" ")[5].split("\\(")[0]
    if (clazz != null) {
      testClasses.get(clazz).add(method)
    }
  } else if (line.startsWith("Compiled from")) {
    clazz = null
  }
}

testClasses.sort().each { clazz, methods ->
  foo = ""
  if (oldClasses.get(clazz) != null && ((boolean)oldClasses.get(clazz).get("all"))) foo = "#"
  println "${foo}${clazz}.all"
  methods.sort().each { method ->
    if (method != "all") {
      foo = ""
      if (oldClasses.get(clazz) != null) {
        if (oldClasses.get(clazz).get(method) == null) {
          foo = "#"
        } else {
          if ((boolean)oldClasses.get(clazz).get(method)) foo = "#"
        }
      }
      println "${foo}${clazz}.${method}"
    }
  }
}
