package eu.trentorise.opendata.odtdoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static eu.trentorise.opendata.commons.OdtUtils.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import jodd.jerry.Jerry;
import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.RepositoryTag;
import org.pegdown.Parser;
import org.pegdown.PegDownProcessor;

/**
 *
 * @author David Leoni
 */
public class OdtDoc {

    private static final Logger LOG = Logger.getLogger(OdtDoc.class.getName());

    private String repoName;
    private String repoTitle;
    private String repoOrganization;
    private String sourceDocsDirPath;
    private String pagesDirPath;
    private boolean local;
    private File sourceDocsDir;
    private File pagesDir;

    /**
     * Null means they were not fetched. Notice we may also have fetched tags
     * and discovered there where none, so there might also be an empty array.
     */
    @Nullable
    private List<RepositoryTag> repoTags;

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

    private static void deleteOutputVersionDir(File outputVersionDir, int major, int minor) {

        String versionName = "" + major + "." + minor;

        if (outputVersionDir.exists()) {
            // let's be strict before doing moronic things
            checkArgument(major >= 0);
            checkArgument(minor >= 0);
            if (outputVersionDir.getAbsolutePath().endsWith(versionName)) {
                LOG.info("Found already existing output dir, cleaning it...");
                try {
                    FileUtils.deleteDirectory(outputVersionDir);
                } catch (Exception ex) {
                    throw new RuntimeException("Error while deleting directory!", ex);
                }
            } else {
                throw new RuntimeException("output path " + outputVersionDir.getAbsolutePath() + " doesn't end with '" + versionName + "', avoiding cleaning it for safety reasons!");
            }
        }

    }

    public OdtDoc(String repoName, String repoTitle, String repoOrganization, String sourceDocsDirPath, String pagesDirPath, boolean local) {
        checkNotEmpty(repoName, "Invalid repository name!");
        checkNotEmpty(repoTitle, "Invalid repository title!");
        checkNotEmpty(repoOrganization, "Invalid repository organization!");
        checkNotEmpty(sourceDocsDirPath, "Invalid repository source docs dir path!");
        checkNotEmpty(pagesDirPath, "Invalid pages dir path!");

        this.repoName = repoName;
        this.repoTitle = repoTitle;
        this.repoOrganization = repoOrganization;
        this.sourceDocsDirPath = sourceDocsDirPath;
        this.pagesDirPath = pagesDirPath;
        this.local = local;

        sourceDocsDir = new File(sourceDocsDirPath);
        pagesDir = new File(pagesDirPath);

    }

    private OdtDoc() {
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

    void buildMd(File sourceMdFile, File outputFile, String prependedPath, SemVersion version) {
        checkNotNull(prependedPath);

        String sourceMdString;
        try {
            sourceMdString = FileUtils.readFileToString(sourceMdFile);
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't read source md file!", ex);
        }

        String filteredSourceMdString = sourceMdString
                .replaceAll("#\\{version}", version.toString())
                .replaceAll("#\\{repoRelease}", OdtDocs.repoRelease(repoOrganization, repoName, version.toString()));

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
        String contentFromWikiHtml = pdp.markdownToHtml(filteredSourceMdString);
        skeleton.$("#odtdoc-internal-content").html(contentFromWikiHtml);
        skeleton.$("#odtdoc-repo-title").html(repoTitle);

        File programLogo = new File(sourceDocsDir, "img\\" + repoName + "-logo-200px.png");

        if (programLogo.exists()) {
            skeleton.$("#odtdoc-program-logo").attr("src", prependedPath + "img/" + repoName + "-logo-200px.png");
        } else {
            skeleton.$("#odtdoc-program-logo").css("display", "none");
        }

        skeleton.$("#odtdoc-program-logo-link").attr("href", OdtDocs.repoWebsite(repoOrganization, repoName));

        skeleton.$("#odtdoc-wiki").attr("href", OdtDocs.repoWiki(repoOrganization, repoName));
        skeleton.$("#odtdoc-home").attr("href", OdtDocs.repoWebsite(repoOrganization, repoName));

        // cleaning example versions
        skeleton.$(".odtdoc-version-tab-header").remove();

        List<RepositoryTag> tags = new ArrayList(OdtDocs.filterTags(repoName, repoTags).values());
        Collections.reverse(tags);
        for (RepositoryTag tag : tags) {
            SemVersion ver = OdtDocs.version(repoName, tag.getName());
            String verShortName = OdtDocs.majorMinor(ver);
            skeleton.$("#odtdoc-nav-header").append(
                    "<a class='odtdoc-version-tab-header' href='"
                    + prependedPath
                    + verShortName
                    + "'>" + verShortName + "</a>");
        }

        String sidebarString = makeSidebar(contentFromWikiHtml);
        if (sidebarString.length() > 0) {
            skeleton.$("#odtdoc-internal-sidebar").html(sidebarString);
        } else {
            skeleton.$("#odtdoc-internal-sidebar").text("");
        }

        skeleton.$(".odtdoc-to-strip").remove();
        
        try {
            FileUtils.write(outputFile, skeleton.html());
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't write into " + outputFile.getAbsolutePath() + "!", ex);
        }

    }

    private void buildIndex(SemVersion latestVersion) {
        File sourceMdFile = new File(sourceDocsDir, "index.md");

        File outputFile = new File(pagesDir, "index.html");

        buildMd(sourceMdFile, outputFile, "", latestVersion);
    }

    private void processDir(SemVersion semVersion) {
        checkNotNull(semVersion);
        
            // File sourceMdFile = new File(wikiDir, "userdoc\\" + ver.getMajor() + ver.getMinor() + "\\Usage.md");
        // File outputFile = new File(pagesDir, version + "\\usage.html");
        // buildMd(sourceMdFile, outputFile, "../");            

        File sourceVersionDir = new File(sourceDocsDir, "x.y");

        if (!sourceVersionDir.exists()) {
            throw new RuntimeException("Can't find source dir!" + sourceVersionDir.getAbsolutePath());
        }

        File targetVersionDir = new File(pagesDir, "" + semVersion.getMajor() + "." + semVersion.getMinor());

        deleteOutputVersionDir(targetVersionDir, semVersion.getMajor(), semVersion.getMinor());

        DirWalker dirWalker = new DirWalker(
                sourceVersionDir,
                targetVersionDir,
                this,
                semVersion
        );
        dirWalker.process();

    }

    public void generateSite() throws IOException {

        LOG.log(Level.INFO, "Fetching {0}/{1} tags.", new Object[]{repoOrganization, repoName});
        repoTags = OdtDocs.fetchTags(repoOrganization, repoName);

        SemVersion latestVersion;

        if (local) {
            latestVersion = SemVersion.of("0.0.0"); // todo take this from pom
        } else {
            latestVersion = OdtDocs.latestVersion(repoName, repoTags);
        }

        buildIndex(latestVersion);

        if (local) {
                LOG.log(Level.INFO, "Processing local source");
                processDir(latestVersion);

        } else {
            SortedMap<String, RepositoryTag> filteredTags = OdtDocs.filterTags(repoName, repoTags);

            for (RepositoryTag tag : filteredTags.values()) {

                LOG.log(Level.INFO, "Processing release tag {0}", tag.getName());
                processDir(OdtDocs.version(repoName, tag.getName()));

            }

        }

        File websiteTemplateDir = findResource("/website-template");

        LOG.log(Level.INFO, "Copying website template dir: {0}", websiteTemplateDir.getAbsolutePath());
        LOG.log(Level.INFO, "        to directory: {0}", pagesDir.getAbsolutePath());

        FileUtils.copyDirectory(websiteTemplateDir, pagesDir);

        File userdocImgDir = new File(sourceDocsDir, "img");
        File targetImgDir = new File(pagesDir, "img");
        LOG.log(Level.INFO, "Merging img dir: {0}", userdocImgDir.getAbsolutePath());
        LOG.log(Level.INFO, "        in directory: {0}", targetImgDir.getAbsolutePath());

        FileUtils.copyDirectory(userdocImgDir, targetImgDir);

    }

    public static void main(String[] args) throws IOException, URISyntaxException {

        String repoName = "odt-commons";
        String repoTitle = "Odt Commons";

        OdtDoc odtDoc = new OdtDoc(
                repoName,
                repoTitle,
                "opendatatrentino",
                "..\\..\\" + repoName + "\\prj\\docs",
                "..\\..\\" + repoName + "\\pages",
                true
        );

        odtDoc.generateSite();
    }

    private String makeSidebar(String contentFromWikiHtml) {
        Jerry html = Jerry.jerry(contentFromWikiHtml);
        String ret = "";
        for (Jerry sourceHeaderLink : html.$("h3 a")) {
            // <a href="#header1">Header 1</a><br/>

            ret += "<div> <a href='#" + sourceHeaderLink.attr("id") + "'>" + sourceHeaderLink.text() + "</div> \n";
            /*Jerry.jerry("<a>")
             .attr("href","#" + sourceHeaderLink.attr("id"))
             .text(sourceHeaderLink.text()); */
        }
        return ret;
    }
}
