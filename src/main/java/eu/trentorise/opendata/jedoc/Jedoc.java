package eu.trentorise.opendata.jedoc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import eu.trentorise.opendata.commons.NotFoundException;
import eu.trentorise.opendata.commons.OdtUtils;
import static eu.trentorise.opendata.commons.OdtUtils.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import jodd.jerry.Jerry;
import jodd.jerry.JerryFunction;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.egit.github.core.RepositoryTag;
import org.pegdown.Parser;
import org.pegdown.PegDownProcessor;

/**
 *
 * @author David Leoni
 */
public class Jedoc {

    private static final Logger LOG = Logger.getLogger(Jedoc.class.getName());

    private String repoName;
    private String repoTitle;
    private String repoOrganization;
    private boolean local;
    private File sourceRepoDir;
    private File pagesDir;

    private Model pom;

    /**
     * Null means they were not fetched. Notice we may also have fetched tags
     * and discovered there where none, so there might also be an empty array.
     */
    @Nullable
    private List<RepositoryTag> repoTags;

    PegDownProcessor pegDownProcessor;

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

    public Jedoc(String repoName, String repoTitle, String repoOrganization, String sourceRepoDirPath, String pagesDirPath, boolean local) {
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
        this.pegDownProcessor = new PegDownProcessor(
                Parser.QUOTES
                | Parser.HARDWRAPS
                | Parser.AUTOLINKS
                | Parser.TABLES
                | Parser.FENCED_CODE_BLOCKS
                | Parser.WIKILINKS
                | Parser.STRIKETHROUGH // not supported in netbeans flow 2.0 yet
                | Parser.ANCHORLINKS // not supported in netbeans flow 2.0 yet		
        );
    }

    private File sourceDocsDir() {
        return new File(sourceRepoDir, "docs");
    }

    /**
     * Searches file indicated by path first in src/main/resources (so it works
     * even when developing), then in proper classpath resources. If file is
     * found it is returned, otherwise an exception is thrown.
     *
     * @throws NotFoundException if path can't be found.
     */
    private static File findResource(String path) {
        File fileFromPath = new File("src/main/resources" + path);

        if (fileFromPath.exists()) {
            return fileFromPath;
        }

        LOG.log(Level.INFO, "Can''t find file {0}", fileFromPath.getAbsolutePath());

        URL resourceUrl = Jedoc.class.getResource(path);
        if (resourceUrl == null) {
            throw new NotFoundException("Can't find path in resources! " + path);
        }
        String resourcePath = resourceUrl.getFile();
        LOG.log(Level.INFO, "Found file in {0}", resourcePath);
        return new File(resourcePath);
    }

    static String programLogoName(String repoName) {
        return repoName + "-logo-200px.png";
    }

    static File programLogo(File sourceDocsDir, String repoName) {
        return new File(sourceDocsDir, "img\\" + programLogoName(repoName));
    }

    /**
     * Writes an md file as html to outputFile
     *
     * @param outputFile Must not exist.
     * @param prependedPath the path prepended according to the page position in
     * the weebsite tree
     * @param version The version the md page refers to.
     */
    void writeMdAsHtml(File sourceMdFile, File outputFile, String prependedPath, final SemVersion version) {
        checkNotNull(prependedPath);

        String sourceMdString;
        try {
            sourceMdString = FileUtils.readFileToString(sourceMdFile);
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't read source md file!", ex);
        }

        String filteredSourceMdString = sourceMdString
                .replaceAll("#\\{version}", version.toString())
                .replaceAll("#\\{majorMinorVersion}", Jedocs.majorMinor(version))
                .replaceAll("#\\{repoRelease}", Jedocs.repoRelease(repoOrganization, repoName, version.toString()));

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
                    .replaceAll("src=\"img/", "src=\"../img/")
                    .replaceAll("href=\"css/", "href=\"../css/");

        } else {
            skeletonStringFixedPaths = skeletonString;
        }

        Jerry skeleton = Jerry.jerry(skeletonStringFixedPaths);
        skeleton.$("title").text(repoTitle);
        String contentFromWikiHtml = pegDownProcessor.markdownToHtml(filteredSourceMdString);
        Jerry contentFromWiki = Jerry.jerry(contentFromWikiHtml);
        contentFromWiki.$("a")
                .each(new JerryFunction() {

                    @Override
                    public boolean onNode(Jerry arg0, int arg1) {
                        String href = arg0.attr("href");
                        if (href.startsWith("../../src")) {
                            arg0.attr("href", href.replace("../../src", Jedocs.repoRelease(repoOrganization, repoName, version.toString()) + "/src"));
                            return true;
                        }
                        if (href.endsWith(".md")) {
                            arg0.attr("href", Jedocs.htmlizePath(href));
                            return true;
                        }

                        if (href.equals("../../wiki")) {
                            arg0.attr("href", href.replace("../../wiki", Jedocs.repoWiki(repoOrganization, repoName)));
                            return true;
                        }

                        if (href.equals("../../issues")) {
                            arg0.attr("href", href.replace("../../issues", Jedocs.repoIssues(repoOrganization, repoName)));
                            return true;
                        }

                        if (href.equals("../../milestones")) {
                            arg0.attr("href", href.replace("../../milestones", Jedocs.repoMilestones(repoOrganization, repoName)));
                            return true;
                        }

                        if (OdtUtils.removeTrailingSlash(href).equals("docs")) {
                            arg0.attr("href", Jedocs.majorMinor(version) + "/index.html");
                            return true;
                        }

                        return true;
                    }
                }
                );
        skeleton.$("#jedoc-internal-content").html(contentFromWiki.html());

        skeleton.$("#jedoc-repo-link").html(repoTitle).attr("href", prependedPath + "index.html");

        File programLogo = programLogo(sourceDocsDir(), repoName);

        if (programLogo.exists()) {
            skeleton.$("#jedoc-program-logo").attr("src", prependedPath + "img/" + repoName + "-logo-200px.png");
            skeleton.$("#jedoc-program-logo-link").attr("href", prependedPath + "index.html");
        } else {
            skeleton.$("#jedoc-program-logo-link").css("display", "none");
        }

        skeleton.$("#jedoc-wiki").attr("href", Jedocs.repoWiki(repoOrganization, repoName));
        skeleton.$("#jedoc-project").attr("href", Jedocs.repoUrl(repoOrganization, repoName));
        
        skeleton.$("#jedoc-home").attr("href", prependedPath + "index.html");
        if (prependedPath.length() == 0) {
            skeleton.$("#jedoc-home").addClass("jedoc-tag-selected");
        }

        // cleaning example versions
        skeleton.$(".jedoc-version-tab-header").remove();

        List<RepositoryTag> tags = new ArrayList(Jedocs.filterTags(repoName, repoTags).values());
        Collections.reverse(tags);

        if (local) {

            if (tags.size() > 0) {
                SemVersion ver = Jedocs.version(repoName, tags.get(0).getName());
                if (version.getMajor() >= ver.getMajor()
                        && version.getMinor() >= ver.getMinor()) {
                    addVersionHeaderTag(skeleton, prependedPath, version, prependedPath.length() != 0);
                }

            } else {
                addVersionHeaderTag(skeleton, prependedPath, version, prependedPath.length() != 0);
            }
        } else {
            LOG.warning("TODO - ADDING ONLY ONE TAG AND IGNORING THE OTHER ONES");
            addVersionHeaderTag(skeleton, prependedPath, version, prependedPath.length() != 0);
            /*
            for (RepositoryTag tag : tags) {
                SemVersion ver = Jedocs.version(repoName, tag.getName());
                addVersionHeaderTag(skeleton, prependedPath, ver, true);
            }
            throw new UnsupportedOperationException("repo tags are not supported yet!");
            */
        }

        String sidebarString = makeSidebar(contentFromWikiHtml);
        if (sidebarString.length() > 0) {
            skeleton.$("#jedoc-internal-sidebar").html(sidebarString);
        } else {
            skeleton.$("#jedoc-internal-sidebar").text("");
        }

        skeleton.$(".jedoc-to-strip").remove();

        try {
            FileUtils.write(outputFile, skeleton.html());
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't write into " + outputFile.getAbsolutePath() + "!", ex);
        }

    }

    private static void addVersionHeaderTag(Jerry skeleton, String prependedPath, SemVersion version, boolean selected) {
        String verShortName = Jedocs.majorMinor(version);
        String classSelected = selected ? "jedoc-tag-selected" : "";
        skeleton.$("#jedoc-usage").append(
                "<a class='jedoc-version-tab-header " + classSelected + "' href='"
                + prependedPath
                + verShortName
                + "/index.html'>" + verShortName + "</a>");
    }

    private void buildIndex(SemVersion latestVersion) {
        File sourceMdFile = new File(sourceRepoDir, "README.md");

        File outputFile = new File(pagesDir, "index.html");

        writeMdAsHtml(sourceMdFile, outputFile, "", latestVersion);
    }

    private File targetVersionDir(SemVersion semVersion) {
        checkNotNull(semVersion);
        return new File(pagesDir, "" + semVersion.getMajor() + "." + semVersion.getMinor());
    }

    /**
     * Returns the directory where docs about the latest version will end up.
     */
    private File targetLatestDocsDir() {
        return new File(pagesDir, "latest");
    }

    private void processDir(SemVersion semVersion) {
        checkNotNull(semVersion);

        // File sourceMdFile = new File(wikiDir, "userdoc\\" + ver.getMajor() + ver.getMinor() + "\\Usage.md");
        // File outputFile = new File(pagesDir, version + "\\usage.html");
        // buildMd(sourceMdFile, outputFile, "../");            
        if (!sourceDocsDir().exists()) {
            throw new RuntimeException("Can't find source dir!" + sourceDocsDir().getAbsolutePath());
        }

        File targetVersionDir = targetVersionDir(semVersion);

        deleteOutputVersionDir(targetVersionDir, semVersion.getMajor(), semVersion.getMinor());

        DirWalker dirWalker = new DirWalker(
                sourceDocsDir(),
                targetVersionDir,
                this,
                semVersion
        );
        dirWalker.process();

    }

    /**
     * Copies target version to 'latest' directory, cleaning it before the copy
     * @param version 
     */
    private void createLatestDocsDirectory(SemVersion version){
        
        File targetLatestDocsDir = targetLatestDocsDir();
        LOG.log(Level.INFO, "Creating latest docs directory {0}", targetLatestDocsDir.getAbsolutePath());
        
        if (!targetLatestDocsDir.getAbsolutePath().endsWith("latest")){
            throw new RuntimeException("Trying to delete a latest docs dir which doesn't end with 'latest'!");
        }
        try {                        
            FileUtils.deleteDirectory(targetLatestDocsDir);
            FileUtils.copyDirectory(targetVersionDir(version), targetLatestDocsDir);    
        } catch (Throwable tr){
            throw new RuntimeException("Error while creating latest docs directory ", tr);
        }
        
    }
    
    public void generateSite() throws IOException {

        LOG.log(Level.INFO, "Fetching {0}/{1} tags.", new Object[]{repoOrganization, repoName});
        repoTags = Jedocs.fetchTags(repoOrganization, repoName);
        MavenXpp3Reader reader = new MavenXpp3Reader();

        try {
            pom = reader.read(new FileInputStream(new File(sourceRepoDir, "pom.xml")));
        } catch (Throwable tr) {
            throw new RuntimeException("Error while reading pom!", tr);
        }

        SemVersion snapshotVersion = SemVersion.of(pom.getVersion()).withPreReleaseVersion("");
        SemVersion latestPublishedVersion = Jedocs.latestVersion(repoName, repoTags);

        if (local) {
            LOG.log(Level.INFO, "Processing local version");
            buildIndex(snapshotVersion);
            processDir(snapshotVersion);
            createLatestDocsDirectory(snapshotVersion);

        } else {
            LOG.log(Level.INFO, "Processing published version");
            buildIndex(latestPublishedVersion);
            String curBranch = Jedocs.readRepoCurrentBranch(sourceRepoDir);
            SemVersion curBranchVersion = Jedocs.versionFromBranchName(curBranch);
            RepositoryTag releaseTag;
            try {
                releaseTag = Jedocs.find(repoName, curBranchVersion.getMajor(), curBranchVersion.getMinor(), repoTags);
            } catch (NotFoundException ex) {
                throw new RuntimeException("Current branch " + curBranch + " does not correspond to any released version!", ex);
            }
            SemVersion tagVersion = Jedocs.version(repoName, releaseTag.getName());
            processDir(tagVersion);
            createLatestDocsDirectory(tagVersion);
            LOG.warning("TODO - PROCESSING ONLY CURRENT BRANCH, NEED TO PROCESS ALL BRANCHES INSTEAD!");
            /*
             SortedMap<String, RepositoryTag> filteredTags = Jedocs.filterTags(repoName, repoTags);

             for (RepositoryTag tag : filteredTags.values()) {

             LOG.log(Level.INFO, "Processing release tag {0}", tag.getName());
             processDir(Jedocs.version(repoName, tag.getName()));

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
        if (programLogo.exists()) {
            LOG.log(Level.INFO, "Found program logo: {0}", programLogo.getAbsolutePath());
            LOG.log(Level.INFO, "      copying it into dir {0}", targetImgDir.getAbsolutePath());

            FileUtils.copyFile(programLogo, new File(targetImgDir, programLogoName(repoName)));
        }

        FileUtils.copyFile(new File(sourceRepoDir, "LICENSE.txt"), new File(pagesDir, "LICENSE.txt"));

        LOG.info("\n\nSite is now browsable at " + pagesDir.getAbsolutePath() + "\n\n");
    }

    public static void main(String[] args) throws IOException, URISyntaxException {

        String repoName = "traceprov";
        String repoTitle = "TraceProv";

        Jedoc jedoc = new Jedoc(
                repoName,
                repoTitle,
                "opendatatrentino",
                "..\\..\\traceprov\\prj", // todo fixed path!
                "..\\..\\traceprov\\pages", // todo fixed path!
                false
        );

        jedoc.generateSite();
    }

    private String makeSidebar(String contentFromWikiHtml) {
        Jerry html = Jerry.jerry(contentFromWikiHtml);
        String ret = "";
        for (Jerry sourceHeaderLink : html.$("h3 a")) {
            // <a href="#header1">Header 1</a><br/>

            ret += "<div> <a href='" + sourceHeaderLink.first().first().attr("href") + "'>" + sourceHeaderLink.first().text() + "</a></div> \n";
            /*Jerry.jerry("<a>")
             .attr("href","#" + sourceHeaderLink.attr("id"))
             .text(sourceHeaderLink.text()); */
        }
        return ret;
    }
}
