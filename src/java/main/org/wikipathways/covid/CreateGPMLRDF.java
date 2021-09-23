package org.wikipathways.covid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
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

import com.hp.hpl.jena.rdf.model.Model;

public class CreateGPMLRDF {

    public static void main(String[] args) throws Exception {
        String gpmlFile = args[0];
        String wpid     = gpmlFile.substring(5,11);
        String rev      = args[1];

        InputStream input = new FileInputStream(gpmlFile);
        Pathway pathway = PathwayReader.readPathway(input);
        input.close();

        Model model = GpmlConverter.convertGpml(pathway, wpid, rev);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        model.write(output, "TURTLE");
        System.out.print(new String(output.toByteArray()));
    }

}
