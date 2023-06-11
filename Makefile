GPMLS := ${shell cat pathways.txt | sed -e 's/\(.*\)/gpml\/\1.gpml/' }
WPRDFS := ${shell cat pathways.txt | sed -e 's/\(.*\)/wp\/Human\/\1.ttl/' }
GPMLRDFS := ${shell cat pathways.txt | sed -e 's/\(.*\)/wp\/gpml\/Human\/\1.ttl/' }
REPORTS := ${shell cat pathways.txt | sed -e 's/\(.*\)/reports\/\1.md/' }
SBMLS := ${shell cat pathways.txt | sed -e 's/\(.*\)/sbml\/\1.sbml/' } ${shell cat pathways.txt | sed -e 's/\(.*\)/sbml\/\1.txt/' }
SVGS := ${shell cat pathways.txt | sed -e 's/\(.*\)/sbml\/\1.svg/' }

FRAMEWORKVERSION=release-6
JENAVERSION=4.8.0
GPMLRDFJAR=GPML2RDF-3.0.0-SNAPSHOT.jar
WPCURJAR=wikipathways.curator-1-SNAPSHOT.jar

WEBSITE := ${shell cat website.txt }

all: wikipathways-rdf-wp.zip wikipathways-rdf-gpml.zip

install:
	@wget -O libs/${GPMLRDFJAR} https://github.com/wikipathways/wikipathways-curation-template/releases/download/${FRAMEWORKVERSION}/${GPMLRDFJAR}
	@wget -O libs/${WPCURJAR} https://github.com/wikipathways/wikipathways-curation-template/releases/download/${FRAMEWORKVERSION}/${WPCURJAR}
	@wget -O libs/slf4j-simple-1.7.32.jar https://search.maven.org/remotecontent?filepath=org/slf4j/slf4j-simple/1.7.32/slf4j-simple-1.7.32.jar
	@wget -O libs/jena-arq-${JENAVERSION}.jar https://repo1.maven.org/maven2/org/apache/jena/jena-arq/${JENAVERSION}/jena-arq-${JENAVERSION}.jar

sbml: ${SBMLS}

svg: ${SVGS}

fetch: clean ${GPMLS}

clean:
	@rm -f ${GPMLS}

distclean: clean
	@rm libs/*.jar

gpml/%.gpml:
	@echo "Git fetching $@ ..."
	@echo '$@' | sed -e 's/gpml\/\(.*\)\.gpml/\1/' | xargs bash getPathway.sh

wikipathways-rdf-wp.zip: ${WPRDFS}
	@rm -f wikipathways-rdf-wp.zip
	@zip wikipathways-rdf-wp.zip wp/Human/*

wikipathways-rdf-gpml.zip: ${GPMLRDFS}
	@rm -f wikipathways-rdf-gpml.zip
	@zip wikipathways-rdf-gpml.zip wp/gpml/Human/*

sbml/%.sbml: gpml/%.gpml
	@echo "Fetching SBML for $< ..."
	@mkdir -p sbml
	@curl -H "Content-Type: application/octet-stream" -X POST --data-binary @$< https://minerva-service.lcsb.uni.lu/minerva/api/convert/GPML:SBML > $@

sbml/%.txt: sbml/%.sbml
	@echo "Extracting notes for $@ ..."
	@xpath -e "/sbml/model/notes/body/p/text()" $< > $@ || :

sbml/%.svg: sbml/%.sbml
	@echo "Fetching SVG for $@ ..."
	@curl -H "Content-Type: application/octet-stream" -X POST --data-binary @$< https://minerva-service.lcsb.uni.lu/minerva/api/convert/image/SBML:svg > $@

wp/Human/%.ttl: gpml/%.gpml src/java/main/org/wikipathways/curator/CreateRDF.class
	@mkdir -p wp/Human
	@cat "$<.rev" | xargs java -cp src/java/main/.:libs/${GPMLRDFJAR}:libs/derby-10.14.2.0.jar:libs/slf4j-simple-1.7.32.jar org.wikipathways.curator.CreateRDF $< $@

wp/gpml/Human/%.ttl: gpml/%.gpml src/java/main/org/wikipathways/curator/CreateGPMLRDF.class
	@mkdir -p wp/gpml/Human
	@cat "$<.rev" | xargs java -cp src/java/main/.:libs/${GPMLRDFJAR}:libs/derby-10.14.2.0.jar:libs/slf4j-simple-1.7.32.jar org.wikipathways.curator.CreateGPMLRDF $< $@

src/java/main/org/wikipathways/curator/CreateRDF.class: src/java/main/org/wikipathways/curator/CreateRDF.java
	@echo "Compiling $@ ..."
	@javac -cp libs/${GPMLRDFJAR} src/java/main/org/wikipathways/curator/CreateRDF.java

src/java/main/org/wikipathways/curator/CreateGPMLRDF.class: src/java/main/org/wikipathways/curator/CreateGPMLRDF.java
	@echo "Compiling $@ ..."
	@javac -cp libs/${GPMLRDFJAR} src/java/main/org/wikipathways/curator/CreateGPMLRDF.java

src/java/main/org/wikipathways/curator/CheckRDF.class: src/java/main/org/wikipathways/curator/CheckRDF.java libs/${WPCURJAR}
	@echo "Compiling $@ ..."
	@javac -cp libs/${WPCURJAR} src/java/main/org/wikipathways/curator/CheckRDF.java

check: ${REPORTS} index.md

reports/%.md: wp/Human/%.ttl wp/gpml/Human/%.ttl src/java/main/org/wikipathways/curator/CheckRDF.class tests.txt
	@echo "Detection curation events for $@ ..."
	@mkdir -p reports
	@java -cp libs/slf4j-simple-1.7.32.jar:libs/jena-arq-${JENAVERSION}.jar:src/java/main/:libs/${WPCURJAR} org.wikipathways.curator.CheckRDF $< $@

index.md: ${REPORTS}
	@echo "<img style=\"float: right; width: 200px\" src=\"logo.png\" />" > index.md
	@echo "# Validation Reports\n" >> index.md
	@for report in $(REPORTS) ; do \
		echo -n "* [$$report]($$report) " >> index.md ; \
		echo -n "<img alt=\"pathway status\" src=\"https://img.shields.io/endpoint?url=${WEBSITE}reports/" >> index.md ; \
		echo -n "`echo "$$report" | sed -e 's/.md//; s/reports\///'`" >> index.md ; \
		echo ".json\">" >> index.md ; \
	done

update: install
	@wget -O Makefile https://raw.githubusercontent.com/wikipathways/wikipathways-curation-template/main/Makefile
	@wget -O extractTests.groovy https://raw.githubusercontent.com/wikipathways/wikipathways-curation-template/main/extractTests.groovy
	@wget -O src/java/main/org/wikipathways/curator/CheckRDF.java https://raw.githubusercontent.com/wikipathways/wikipathways-curation-template/main/src/java/main/org/wikipathways/curator/CheckRDF.java
	@wget -O src/java/main/org/wikipathways/curator/CreateRDF.java https://raw.githubusercontent.com/wikipathways/wikipathways-curation-template/main/src/java/main/org/wikipathways/curator/CreateRDF.java
	@wget -O src/java/main/org/wikipathways/curator/CreateGPMLRDF.java https://raw.githubusercontent.com/wikipathways/wikipathways-curation-template/main/src/java/main/org/wikipathways/curator/CreateGPMLRDF.java

updateTests:
	@jar tf libs/wikipathways.curator-1-SNAPSHOT.jar | grep '.class' \
	  | grep 'nl.unimaas.bigcat.wikipathways.curator.tests' | tr / . \
	  | sed 's/\.class//' | xargs javap -public -cp libs/wikipathways.curator-1-SNAPSHOT.jar \
	  > tests.tmp
	@groovy extractTests.groovy > tests.tmp2
	@mv tests.tmp2 tests.txt
