/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.trentorise.opendata.jadoc.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class TestReadPom {
    private static final Logger LOG = Logger.getLogger(TestReadPom.class.getName());
    
    
    @Test
    public void testReadPom() throws FileNotFoundException, IOException, XmlPullParserException{
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileInputStream("pom.xml"));
        LOG.log(Level.INFO, "artifactId = {0}", model.getArtifactId());
        LOG.log(Level.INFO, "version = {0}", model.getVersion());
    }
}
