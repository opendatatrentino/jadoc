package eu.trentorise.opendata.odtdoc;

import static com.google.common.base.Preconditions.checkArgument;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import jodd.jerry.Jerry;
import org.apache.commons.io.FileUtils;
import org.pegdown.Extensions;
import org.pegdown.LinkRenderer;
import org.pegdown.Parser;
import org.pegdown.PegDownProcessor;
import org.pegdown.Printer;
import org.pegdown.VerbatimSerializer;
import org.pegdown.ast.VerbatimNode;

/**
 *
 * @author David Leoni
 */
public class OdtDoc {

    private static final Logger LOG = Logger.getLogger(OdtDoc.class.getName());

    /**
     * First searches in src/main/resources (so it works even when developing), then in proper classpath
     * resources
     */
    private static File findResource(String path) {
        File fileFromPath = new File("src/main/resources" + path);
        
        if (fileFromPath.exists()){
            return fileFromPath;
        }
        
        LOG.log(Level.INFO, "Can''t find file {0}", fileFromPath.getAbsolutePath()) ;
                                
        URL resourceUrl = OdtDoc.class.getResource(path);
        if (resourceUrl == null) {
            throw new RuntimeException("Can't find path in resources! " + path);
        }
        String resourcePath = resourceUrl.getFile();
        LOG.log(Level.INFO, "Found file in {0}", resourcePath) ;        
        return new File(resourcePath);
    }

    public static void main(String[] args) throws IOException, URISyntaxException {

        PegDownProcessor pdp = new PegDownProcessor(
                Parser.QUOTES
                | Parser.HARDWRAPS
                | Parser.AUTOLINKS
                | Parser.TABLES
                | Parser.FENCED_CODE_BLOCKS
                | Parser.WIKILINKS
                | Parser.STRIKETHROUGH // not supported in netbeans flow 2.0 yet
                | Parser.ANCHORLINKS // not supported in netbeans flow 2.0 yet		
        );

        String repoName = "jackan";
        String repoTitle = "Jackan";
        String repoOrganization = "opendatatrentino";
        String repoUrl = "https://github.com/" + repoOrganization + "/" + repoName;
        String repoMaster = repoUrl + "/blob/master/";
        String version = "0.3.1";
        String releaseTag = repoName + "-0.3.1";
        String repoRelease = repoUrl + "/blob/" + releaseTag + "/";

        String wikiDirPath = "..\\..\\jackan\\wiki"; // todo review
        String pagesDirPath = "..\\..\\jackan\\pages"; // todo review
        //String pagesDirPath = "..\\jackan\\pages"; // todo review

        File wikiDir = new File(wikiDirPath);
        File pagesDir = new File(pagesDirPath);

        File outputDir = new File(pagesDir, version);

        if (outputDir.exists()) {
            // let's be strict before doing moronic things
            checkArgument(version.length() > 0);
            if (outputDir.getAbsolutePath().endsWith(version)) {
                LOG.info("Found already existing output dir, cleaning it...");
                FileUtils.deleteDirectory(outputDir);
            } else {
                throw new RuntimeException("output path " + outputDir.getAbsolutePath() + " doesn't end with '" + version + "', avoiding cleaning it for safety reasons!");
            }
        }

        File prova = new File(wikiDir, "Home.md");

        File outputHtml = new File(outputDir, "index.html");

        String provaMdString = FileUtils.readFileToString(prova);
        VerbatimSerializer vs = new VerbatimSerializer() {

            @Override
            public void serialize(VerbatimNode node, Printer printer) {

            }
        };

	//new LinkRenderer().;
        // https://github.com/opendatatrentino/jackan/blob/master/src/test/java/eu/trentorise/opendata/jackan/test/ckan/TestApp.java
        URL skeletonFileUrl = OdtDoc.class.getResource("/skeleton.html");
        if (skeletonFileUrl == null) {
            throw new RuntimeException("Can't find skeleton html!");
        }
        LOG.info("Loading skeleton html: " + skeletonFileUrl.getFile());
        File skeletonFile = new File(skeletonFileUrl.getFile());

        String skeletonString = FileUtils.readFileToString(skeletonFile);

        Jerry skeleton = Jerry.jerry(skeletonString);
        skeleton.$("title").text(repoTitle);
        skeleton.$("#odtdoc-content").html(pdp.markdownToHtml(provaMdString));
        skeleton.$("#odtdoc-repo-title").html(repoTitle);

        FileUtils.write(outputHtml, skeleton.html());
        
      //  findResource();
        URL websiteTemplate = OdtDoc.class.getResource("/website-template");
        if (websiteTemplate == null) {
            throw new RuntimeException("Can't find website-template dir!");
        }
        LOG.log(Level.INFO, "Copying website template dir: {0}", websiteTemplate.getFile());
        LOG.log(Level.INFO, "        to directory: {0}", pagesDir.getAbsolutePath());
        File websiteTemplateDir = new File(websiteTemplate.getFile());

        FileUtils.copyDirectory(websiteTemplateDir, pagesDir);
    }
}
