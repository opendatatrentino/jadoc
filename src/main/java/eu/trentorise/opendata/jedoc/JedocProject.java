package eu.trentorise.opendata.jedoc;

import com.google.common.base.Charsets;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.io.Resources;
import eu.trentorise.opendata.commons.NotFoundException;
import eu.trentorise.opendata.commons.OdtUtils;
import static eu.trentorise.opendata.commons.OdtUtils.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import eu.trentorise.opendata.jedoc.org.pegdown.Parser;
import eu.trentorise.opendata.jedoc.org.pegdown.PegDownProcessor;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author David Leoni
 */
public class JedocProject {

    private static final Logger LOG = Logger.getLogger(JedocProject.class.getName());

    public static final String DOCS_FOLDER = "docs";

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
                }
                catch (Exception ex) {
                    throw new RuntimeException("Error while deleting directory!", ex);
                }
            } else {
                throw new RuntimeException("output path " + outputVersionDir.getAbsolutePath() + " doesn't end with '" + versionName + "', avoiding cleaning it for safety reasons!");
            }
        }

    }

    public JedocProject(String repoName, String repoTitle, String repoOrganization, String sourceRepoDirPath, String pagesDirPath, boolean local) {
        checkNotEmpty(repoName, "Invalid repository name!");
        checkNotEmpty(repoTitle, "Invalid repository title!");
        checkNotEmpty(repoOrganization, "Invalid repository organization!");
        checkNotNull(sourceRepoDirPath, "Invalid repository source docs dir path!");
        checkNotNull(pagesDirPath, "Invalid pages dir path!");

        this.repoName = repoName;
        this.repoTitle = repoTitle;
        this.repoOrganization = repoOrganization;
        if (sourceRepoDirPath.isEmpty()) {
            this.sourceRepoDir = new File("." + File.separator);
        } else {
            this.sourceRepoDir = new File(sourceRepoDirPath);
        }
        this.local = local;
        this.pagesDir = new File(pagesDirPath);
        checkArgument(!sourceRepoDir.getAbsolutePath().equals(pagesDir.getAbsolutePath()),
                "Source folder and target folder coincide! They are " + sourceRepoDir.getAbsolutePath());
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

    private File targetJavadocDir(SemVersion version) {
        return new File(targetVersionDir(version), "javadoc");
    }

    private File sourceJavadocDir(SemVersion version) {
        if (local) {
            return new File(sourceRepoDir, "target/apidocs");
        } else {
            throw new UnsupportedOperationException("todo non-local javadoc not supported yet");
        }
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
     * the website tree
     * @param version The version the md page refers to.
     */
    void writeMdAsHtml(File sourceMdFile, File outputFile, final String prependedPath, final SemVersion version) {
        checkNotNull(prependedPath);

        String sourceMdString;
        try {
            sourceMdString = FileUtils.readFileToString(sourceMdFile);
        }
        catch (Exception ex) {
            throw new RuntimeException("Couldn't read source md file!", ex);
        }

        Jedocs.checkNotMeaningful(sourceMdString, "Invalid source md file!");

        String filteredSourceMdString = sourceMdString
                .replaceAll("#\\{version}", version.toString())
                .replaceAll("#\\{majorMinorVersion}", Jedocs.majorMinor(version))
                .replaceAll("#\\{repoRelease}", Jedocs.repoRelease(repoOrganization, repoName, version));

        
        
        String skeletonString;
        try {
            StringWriter writer = new StringWriter();
            InputStream stream = Jedocs.findResourceStream("/skeleton.html");
            IOUtils.copy(stream, writer, "UTF-8");
            skeletonString = writer.toString();            
        }
        catch (Exception ex) {
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
        String contentFromMdHtml = pegDownProcessor.markdownToHtml(filteredSourceMdString);
        Jerry contentFromMd = Jerry.jerry(contentFromMdHtml);

        contentFromMd.$("a")
                .each(new JerryFunction() {

                    @Override
                    public boolean onNode(Jerry arg0, int arg1) {
                        String href = arg0.attr("href");
                        if (href.startsWith(prependedPath + "src")) {
                            arg0.attr("href", href.replace(prependedPath + "src", Jedocs.repoRelease(repoOrganization, repoName, version) + "/src"));
                            return true;
                        }
                        if (href.endsWith(".md")) {
                            arg0.attr("href", Jedocs.htmlizePath(href));
                            return true;
                        }

                        if (href.equals(prependedPath + "../../wiki")) {
                            arg0.attr("href", href.replace(prependedPath + "../../wiki", Jedocs.repoWiki(repoOrganization, repoName)));
                            return true;
                        }

                        if (href.equals(prependedPath + "../../issues")) {
                            arg0.attr("href", href.replace(prependedPath + "../../issues", Jedocs.repoIssues(repoOrganization, repoName)));
                            return true;
                        }

                        if (href.equals(prependedPath + "../../milestones")) {
                            arg0.attr("href", href.replace(prependedPath + "../../milestones", Jedocs.repoMilestones(repoOrganization, repoName)));
                            return true;
                        }

                        if (OdtUtils.removeTrailingSlash(href).equals(DOCS_FOLDER)) {
                            arg0.attr("href", Jedocs.majorMinor(version) + "/index.html");
                            return true;
                        }

                        if (href.startsWith(DOCS_FOLDER + "/")) {
                            arg0.attr("href", Jedocs.majorMinor(version) + href.substring(DOCS_FOLDER.length()));
                            return true;
                        }

                        return true;
                    }
                }
                );

        contentFromMd.$("img")
                .each(new JerryFunction() {

                    @Override
                    public boolean onNode(Jerry arg0, int arg1) {
                        String src = arg0.attr("src");
                        if (src.startsWith(DOCS_FOLDER + "/")) {
                            arg0.attr("src", Jedocs.majorMinor(version) + src.substring(DOCS_FOLDER.length()));
                            return true;
                        }
                        return true;
                    }
                });

        skeleton.$("#jedoc-internal-content").html(contentFromMd.html());

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

        String sidebarString = makeSidebar(contentFromMdHtml);
        if (sidebarString.length() > 0) {
            skeleton.$("#jedoc-internal-sidebar").html(sidebarString);
        } else {
            skeleton.$("#jedoc-internal-sidebar").text("");
        }
        if (prependedPath.length() == 0) {
            skeleton.$("#jedoc-sidebar-managed-block").css("display", "none");
        }

        skeleton.$(".jedoc-to-strip").remove();

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
            Pattern p = Pattern.compile("todo", Pattern.CASE_INSENSITIVE);
            Matcher matcher = p.matcher(skeleton.html());
            if (matcher.find()) {
                throw new RuntimeException("Found '" + matcher.group() + "' string in file " + sourceMdFile.getAbsolutePath() + " (at position " + matcher.start() + ")");
            }
        }

        try {
            FileUtils.write(outputFile, skeleton.html());
        }
        catch (Exception ex) {
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

    /**
     * Processes a directory 'docs' that holds documentation for a given version
     * of the software
     */
    private void processDocsDir(SemVersion version) {
        checkNotNull(version);

        // File sourceMdFile = new File(wikiDir, "userdoc\\" + ver.getMajor() + ver.getMinor() + "\\Usage.md");
        // File outputFile = new File(pagesDir, version + "\\usage.html");
        // buildMd(sourceMdFile, outputFile, "../");            
        if (!sourceDocsDir().exists()) {
            throw new RuntimeException("Can't find source dir!" + sourceDocsDir().getAbsolutePath());
        }

        File targetVersionDir = targetVersionDir(version);

        deleteOutputVersionDir(targetVersionDir, version.getMajor(), version.getMinor());

        DirWalker dirWalker = new DirWalker(
                sourceDocsDir(),
                targetVersionDir,
                this,
                version
        );
        dirWalker.process();

        copyJavadoc(version);

    }

    /**
     * Copies target version to 'latest' directory, cleaning it before the copy
     *
     * @param version
     */
    private void createLatestDocsDirectory(SemVersion version) {

        File targetLatestDocsDir = targetLatestDocsDir();
        LOG.log(Level.INFO, "Creating latest docs directory {0}", targetLatestDocsDir.getAbsolutePath());

        if (!targetLatestDocsDir.getAbsolutePath().endsWith("latest")) {
            throw new RuntimeException("Trying to delete a latest docs dir which doesn't end with 'latest'!");
        }
        try {
            FileUtils.deleteDirectory(targetLatestDocsDir);
            FileUtils.copyDirectory(targetVersionDir(version), targetLatestDocsDir);
        }
        catch (Throwable tr) {
            throw new RuntimeException("Error while creating latest docs directory ", tr);
        }

    }

    public void generateSite() {

        LOG.log(Level.INFO, "Fetching {0}/{1} tags.", new Object[]{repoOrganization, repoName});
        repoTags = Jedocs.fetchTags(repoOrganization, repoName);
        MavenXpp3Reader reader = new MavenXpp3Reader();

        try {
            pom = reader.read(new FileInputStream(new File(sourceRepoDir, "pom.xml")));
        }
        catch (Exception ex) {
            throw new RuntimeException("Error while reading pom!", ex);
        }

        SemVersion snapshotVersion = SemVersion.of(pom.getVersion()).withPreReleaseVersion("");

        if (local) {
            LOG.log(Level.INFO, "Processing local version");
            buildIndex(snapshotVersion);
            processDocsDir(snapshotVersion);
            createLatestDocsDirectory(snapshotVersion);

        } else {
            if (repoTags.isEmpty()) {
                throw new NotFoundException("There are no tags at all in the repository!!");
            }
            SemVersion latestPublishedVersion = Jedocs.latestVersion(repoName, repoTags);
            LOG.log(Level.INFO, "Processing published version");
            buildIndex(latestPublishedVersion);
            String curBranch = Jedocs.readRepoCurrentBranch(sourceRepoDir);
            SemVersion curBranchVersion = Jedocs.versionFromBranchName(curBranch);
            RepositoryTag releaseTag;
            try {
                releaseTag = Jedocs.find(repoName, curBranchVersion.getMajor(), curBranchVersion.getMinor(), repoTags);
            }
            catch (NotFoundException ex) {
                throw new RuntimeException("Current branch " + curBranch + " does not correspond to any released version!", ex);
            }
            SemVersion tagVersion = Jedocs.version(repoName, releaseTag.getName());
            processDocsDir(tagVersion);
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
        
        Jedocs.copyDirFromResource(Jedocs.class, "/website-template", pagesDir);

        try {

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

        }
        catch (Exception ex) {
            throw new RuntimeException("Error while copying files!", ex);
        }

        LOG.log(Level.INFO, "\n\nSite is now browsable at {0}\n\n", pagesDir.getAbsolutePath());
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

    /**
     * Copies javadoc into target website according to the artifact version.
     */
    private void copyJavadoc(SemVersion version) {
        File targetJavadoc = targetJavadocDir(version);
        if (targetJavadoc.exists() && (targetJavadoc.isFile() || targetJavadoc.length() > 0)) {
            throw new RuntimeException("Target directory for Javadoc already exists!!! " + targetJavadoc.getAbsolutePath());
        }
        if (local) {
            File sourceJavadoc = sourceJavadocDir(version);
            if (sourceJavadoc.exists()) {

                try {
                    LOG.log(Level.INFO, "Now copying Javadoc from {0} to {1}", new Object[]{sourceJavadoc.getAbsolutePath(), targetJavadoc.getAbsolutePath()});
                    FileUtils.copyDirectory(sourceJavadoc, targetJavadoc);
                }
                catch (Throwable tr) {
                    throw new RuntimeException("Error while copying Javadoc from " + sourceJavadoc.getAbsolutePath() + " to " + targetJavadoc.getAbsolutePath());
                }
            }
        } else {
            File jardocs = Jedocs.fetchJavadoc(pom.getGroupId(), pom.getArtifactId(), version);
            Jedocs.copyDirFromJar(jardocs, targetJavadocDir(version), "");
        }

    }
}
