# Required libraries

Java libraries needed for running the tests. They can be downloaded from
https://github.com/wikipathways/wikipathways-curation-template/releases/

Get the libraries with:

```
cd ..
make install
```

## Making new releases

### WikiPathways RDF

```
git clone https://github.com/wikipathways/GPML2RDF.git
cd GPML2RDF
```

Then read the `README.md` and follow the instructions to install the dependencies,
followed by:

```
cd WPRDF
mvn -Dmaven.test.skip=true clean install
```

That creates `GPML2RDF-3.0.0-SNAPSHOT.jar` which
is released as `GPML2RDF-3.0.0-SNAPSHOT.jar`.

### WikiPathwaysCurator

```
git clone https://github.com/wikipathways/WikiPathwaysCurator.git
cd WikiPathwaysCurator
mvn -Dmaven.test.skip=true clean install assembly:single
```

This creates `target/wikipathways.curator-1-SNAPSHOT-jar-with-dependencies.jar` which
is released as `wikipathways.curator-1-SNAPSHOT.jar`.
