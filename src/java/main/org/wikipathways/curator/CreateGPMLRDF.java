package org.wikipathways.curator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.IDMapperStack;
import org.bridgedb.bio.DataSourceTxt;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.wikipathways.wp2rdf.io.PathwayReader;
import org.wikipathways.wp2rdf.GpmlConverter;
import org.wikipathways.wp2rdf.WPREST2RDF;

import org.apache.jena.rdf.model.Model;

public class CreateGPMLRDF {

    public static void main(String[] args) throws Exception {
        String gpmlFile = args[0];
        String wpid     = gpmlFile.substring(5,11);
        String rev      = args[2];
        String outFile  = args[1];

        InputStream input = new FileInputStream(gpmlFile);
        Pathway pathway = PathwayReader.readPathway(input);
        input.close();

        Model model = GpmlConverter.convertGpml(pathway, wpid, rev, Collections.<String>emptyList());
        FileOutputStream output = new FileOutputStream(outFile);
        model.write(output, "TURTLE");
        output.close();
    }

}
