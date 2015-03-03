package eu.trentorise.opendata.jadoc;

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
public class Jadoc {

    private static final Logger LOG = Logger.getLogger(Jadoc.class.getName());

    private String repoName;
    private String repoTitle;
    private String repoOrganization;    
    private boolean local;
    private File sourceRepoDir;
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

    public Jadoc(String repoName, String repoTitle, String repoOrganization, String sourceRepoDirPath, String pagesDirPath, boolean local) {
        checkNotEmpty(repoName, "Invalid repository name!");
        checkNotEmpty(repoTitle, "Invalid repository title!");
        checkNotEmpty(repoOrganization, "Invalid repository organization!");
        checkNotNull(sourceRepoDirPath, "Invalid repository source docs dir path!");
        checkNotNull(pagesDirPath, "Invalid pages dir path!");

        this.repoName = repoName;
        this.repoTitle = repoTitle;
        this.repoOrganization = repoOrganization;
        this.sourceRepoDir = new File(sourceRepoDirPath);        
        this.local = local;        
        this.pagesDir = new File(pagesDirPath);

    }
    
    private File sourceDocsDir(){
        return new File(sourceRepoDir, "docs");
    }

    private Jadoc() {
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

        URL resourceUrl = Jadoc.class.getResource(path);
        if (resourceUrl == null) {
            throw new RuntimeException("Can't find path in resources! " + path);
        }
        String resourcePath = resourceUrl.getFile();
        LOG.log(Level.INFO, "Found file in {0}", resourcePath);
        return new File(resourcePath);
    }
    
    static String programLogoName(String repoName){
        return repoName + "-logo-200px.png";
    }
    
    static File programLogo(File sourceDocsDir, String repoName){
        return new File(sourceDocsDir, "img\\" + programLogoName(repoName));    
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
                .replaceAll("#\\{repoRelease}", Jadocs.repoRelease(repoOrganization, repoName, version.toString()));

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
        skeleton.$("#jadoc-internal-content").html(contentFromWikiHtml);
        skeleton.$("#jadoc-repo-title").html(repoTitle);

        File programLogo = programLogo(sourceDocsDir(), repoName);

        if (programLogo.exists()) {
            skeleton.$("#jadoc-program-logo").attr("src", prependedPath + "img/" + repoName + "-logo-200px.png");
        } else {
            skeleton.$("#jadoc-program-logo").css("display", "none");
        }

        skeleton.$("#jadoc-program-logo-link").attr("href", Jadocs.repoWebsite(repoOrganization, repoName));

        skeleton.$("#jadoc-wiki").attr("href", Jadocs.repoWiki(repoOrganization, repoName));
        skeleton.$("#jadoc-home").attr("href", Jadocs.repoWebsite(repoOrganization, repoName));

        // cleaning example versions
        skeleton.$(".jadoc-version-tab-header").remove();

        List<RepositoryTag> tags = new ArrayList(Jadocs.filterTags(repoName, repoTags).values());
        Collections.reverse(tags);
        for (RepositoryTag tag : tags) {
            SemVersion ver = Jadocs.version(repoName, tag.getName());
            String verShortName = Jadocs.majorMinor(ver);
            skeleton.$("#jadoc-nav-header").append(
                    "<a class='jadoc-version-tab-header' href='"
                    + prependedPath
                    + verShortName
                    + "'>" + verShortName + "</a>");
        }

        String sidebarString = makeSidebar(contentFromWikiHtml);
        if (sidebarString.length() > 0) {
            skeleton.$("#jadoc-internal-sidebar").html(sidebarString);
        } else {
            skeleton.$("#jadoc-internal-sidebar").text("");
        }

        skeleton.$(".jadoc-to-strip").remove();

        try {
            FileUtils.write(outputFile, skeleton.html());
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't write into " + outputFile.getAbsolutePath() + "!", ex);
        }

    }

    private void buildIndex(SemVersion latestVersion) {
        File sourceMdFile = new File(sourceRepoDir, "README.md");

        File outputFile = new File(pagesDir, "index.html");

        buildMd(sourceMdFile, outputFile, "", latestVersion);
    }

    private void processDir(SemVersion semVersion) {
        checkNotNull(semVersion);

            // File sourceMdFile = new File(wikiDir, "userdoc\\" + ver.getMajor() + ver.getMinor() + "\\Usage.md");
        // File outputFile = new File(pagesDir, version + "\\usage.html");
        // buildMd(sourceMdFile, outputFile, "../");            
        if (!sourceDocsDir().exists()) {
            throw new RuntimeException("Can't find source dir!" + sourceDocsDir().getAbsolutePath());
        }

        File targetVersionDir = new File(pagesDir, "" + semVersion.getMajor() + "." + semVersion.getMinor());

        deleteOutputVersionDir(targetVersionDir, semVersion.getMajor(), semVersion.getMinor());

        DirWalker dirWalker = new DirWalker(
                sourceDocsDir(),
                targetVersionDir,
                this,
                semVersion
        );
        dirWalker.process();

    }

    public void generateSite() throws IOException {

        LOG.log(Level.INFO, "Fetching {0}/{1} tags.", new Object[]{repoOrganization, repoName});
        repoTags = Jadocs.fetchTags(repoOrganization, repoName);

        SemVersion latestVersion;

        if (local) {
            latestVersion = SemVersion.of("0.0.0"); // todo take this from pom
        } else {
            latestVersion = Jadocs.latestVersion(repoName, repoTags);
        }

        buildIndex(latestVersion);

        if (local) {
            LOG.log(Level.INFO, "Processing local source");
            processDir(latestVersion);

        } else {
            throw new UnsupportedOperationException("Need to better review non local version case!");
            /*
             SortedMap<String, RepositoryTag> filteredTags = Jadocs.filterTags(repoName, repoTags);

             for (RepositoryTag tag : filteredTags.values()) {

             LOG.log(Level.INFO, "Processing release tag {0}", tag.getName());
             processDir(Jadocs.version(repoName, tag.getName()));

             }
             */
        }

        File websiteTemplateDir = findResource("/website-template");

        LOG.log(Level.INFO, "Copying website template dir: {0}", websiteTemplateDir.getAbsolutePath());
        LOG.log(Level.INFO, "        to directory: {0}", pagesDir.getAbsolutePath());

        FileUtils.copyDirectory(websiteTemplateDir, pagesDir);

        /*
         File userdocImgDir = new File(sourceDocsDir, "img");
         File targetImgDir = new File(pagesDir, "img");
         LOG.log(Level.INFO, "Merging img dir: {0}", userdocImgDir.getAbsolutePath());
         LOG.log(Level.INFO, "        in directory: {0}", targetImgDir.getAbsolutePath());

         FileUtils.copyDirectory(userdocImgDir, targetImgDir);
         */
        
        File targetImgDir = new File(pagesDir, "img");
        
        File programLogo = programLogo(sourceDocsDir(), repoName);
        if (programLogo.exists()){
            LOG.log(Level.INFO, "Found program logo: {0}", programLogo.getAbsolutePath());
            LOG.log(Level.INFO, "      copying it into dir {0}", targetImgDir.getAbsolutePath());

            FileUtils.copyFile(programLogo, new File(targetImgDir, programLogoName(repoName)));
        }
    }
    

    public static void main(String[] args) throws IOException, URISyntaxException {

        String repoName = "jadoc";
        String repoTitle = "Jadoc";

        Jadoc jadoc = new Jadoc(
                repoName,
                repoTitle,
                "opendatatrentino",
                ".\\", // todo fixed path!
                "..\\pages", // todo fixed path!
                true
        );

        jadoc.generateSite();
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
