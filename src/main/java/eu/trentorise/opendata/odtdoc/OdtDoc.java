package eu.trentorise.opendata.odtdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import jodd.jerry.Jerry;
import org.apache.commons.io.FileUtils;
import org.pegdown.Parser;
import org.pegdown.PegDownProcessor;

/**
 *
 * @author David Leoni
 */
public class OdtDoc {

    private static final Logger LOG = Logger.getLogger(OdtDoc.class.getName());

    private String repoName = "jackan";
    private String repoTitle = "Jackan";
    private String repoOrganization = "opendatatrentino";
    private String repoUrl = "https://github.com/" + repoOrganization + "/" + repoName;
    private String repoMaster = repoUrl + "/blob/master/";
    private String version = "0.3.1";
    private String releaseTag = repoName + "-0.3.1";
    private String repoRelease = repoUrl + "/blob/" + releaseTag + "/";

    private String wikiDirPath = "..\\..\\jackan\\wiki"; // todo review
    private String pagesDirPath = "..\\..\\jackan\\pages"; // todo review
    //String pagesDirPath = "..\\jackan\\pages"; // todo review

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

    private File wikiDir;
    private File pagesDir;
    private File outputVersionDir;

    public OdtDoc() {

        wikiDir = new File(wikiDirPath);
        pagesDir = new File(pagesDirPath);

        outputVersionDir = new File(pagesDir, version);

        if (outputVersionDir.exists()) {
            // let's be strict before doing moronic things
            checkArgument(version.length() > 0);
            if (outputVersionDir.getAbsolutePath().endsWith(version)) {
                LOG.info("Found already existing output dir, cleaning it...");
                try {
                    FileUtils.deleteDirectory(outputVersionDir);
                } catch (Exception ex) {
                    throw new RuntimeException("Error while deleting directory!", ex);
                }
            } else {
                throw new RuntimeException("output path " + outputVersionDir.getAbsolutePath() + " doesn't end with '" + version + "', avoiding cleaning it for safety reasons!");
            }
        }
    }

    /**
     * First searches in src/main/resources (so it works even when developing),
     * then in proper classpath resources
     */
    private static File findResource(String path) {
        File fileFromPath = new File("src/main/resources" + path);

        if (fileFromPath.exists()) {
            return fileFromPath;
        }

        LOG.log(Level.INFO, "Can''t find file {0}", fileFromPath.getAbsolutePath());

        URL resourceUrl = OdtDoc.class.getResource(path);
        if (resourceUrl == null) {
            throw new RuntimeException("Can't find path in resources! " + path);
        }
        String resourcePath = resourceUrl.getFile();
        LOG.log(Level.INFO, "Found file in {0}", resourcePath);
        return new File(resourcePath);
    }

    /**
     * Returns the path, eventually prepended with ../ if isIndex is false.
     */
    private static String relPath(String path, boolean isIndex) {
        if (isIndex) {
            return path;
        } else {
            return "../" + path;
        }
    }

    void buildMd(File sourceMdFile, File outputFile, String prependedPath) {
        checkNotNull(prependedPath);

        String sourceMdString;
        try {
            sourceMdString = FileUtils.readFileToString(sourceMdFile);
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't read source md file!", ex);
        }

        String filteredSourceMdString = sourceMdString
                .replaceAll("#\\{version}", version)
                .replaceAll("#\\{repoRelease}", repoRelease);
        
        //new LinkRenderer().;
        // https://github.com/opendatatrentino/jackan/blob/master/src/test/java/eu/trentorise/opendata/jackan/test/ckan/TestApp.java
        File skeletonFile = findResource("/skeleton.html");

        String skeletonString;
        try {
            skeletonString = FileUtils.readFileToString(skeletonFile);
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't read skeleton file!", ex);
        }

        String skeletonStringFixedPaths;
        if (prependedPath.length() > 0) {
            // fix paths
            skeletonStringFixedPaths = skeletonString.replaceAll("src=\"js/", "src=\"../js/")
                    .replaceAll("src=\"img/", "src=\"../img/");
        } else {
            skeletonStringFixedPaths = skeletonString;
        }

        Jerry skeleton = Jerry.jerry(skeletonStringFixedPaths);
        skeleton.$("title").text(repoTitle);
        skeleton.$("#odtdoc-internal-content").html(pdp.markdownToHtml(filteredSourceMdString));
        skeleton.$("#odtdoc-repo-title").html(repoTitle);
        skeleton.$("#odtdoc-program-logo").attr("src", prependedPath + "img/" + repoName + "-logo-200px.png");

        try {
            FileUtils.write(outputFile, skeleton.html());
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't write into " + outputFile.getAbsolutePath() + "!", ex);
        }

    }

    private void buildIndex() {
        File sourceMdFile = new File(wikiDir, "userdoc\\UserHome.md");

        File outputFile = new File(pagesDir, "index.html");

        buildMd(sourceMdFile, outputFile, "");

    }

    public void generateSite() throws IOException {

        buildIndex();
        
        DirWalker dirWalker = new DirWalker(new File(wikiDir + "\\userdoc\\x.y.z\\"), new File(pagesDir + "\\" + version), this);
        
        dirWalker.process();
        
        File sourceMdFile = new File(wikiDir, "userdoc\\x.y.z\\Usage.md");
        File outputFile = new File(pagesDir, version + "\\usage.html");
        buildMd(sourceMdFile, outputFile, "../");

        File websiteTemplateDir = findResource("/website-template");

        LOG.log(Level.INFO, "Copying website template dir: {0}", websiteTemplateDir.getAbsolutePath());
        LOG.log(Level.INFO, "        to directory: {0}", pagesDir.getAbsolutePath());

        FileUtils.copyDirectory(websiteTemplateDir, pagesDir);

        File userdocImgDir = new File(wikiDir, "userdoc\\img");
        LOG.log(Level.INFO, "Merging userdoc/img dir: {0}", userdocImgDir.getAbsolutePath());
        LOG.log(Level.INFO, "        in directory: {0}", pagesDir.getAbsolutePath());

        FileUtils.copyDirectory(userdocImgDir, pagesDir);

    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        OdtDoc odtDoc = new OdtDoc();

        odtDoc.generateSite();
    }
}
