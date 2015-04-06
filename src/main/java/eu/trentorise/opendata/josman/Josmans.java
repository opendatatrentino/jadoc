/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.trentorise.opendata.josman;

import static com.google.common.base.Preconditions.checkNotNull;
import eu.trentorise.opendata.commons.NotFoundException;
import eu.trentorise.opendata.commons.OdtUtils;
import static eu.trentorise.opendata.commons.OdtUtils.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Utilities for Josman
 *
 * @author David Leoni
 */
public final class Josmans {

    private static final Logger LOG = Logger.getLogger(Josmans.class.getName());

    public static final int CONNECTION_TIMEOUT = 1000;

    private Josmans() {
    }

    /**
     * Reading file with Jgit:
     * https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/api/ReadFileFromCommit.java
     */
    /**
     * Fetches all tags from a github repository. Beware of API limits of 60
     * requests per hour
     */
    public static List<RepositoryTag> fetchTags(String organization, String repoName) {
        OdtUtils.checkNotEmpty(organization, "Invalid organization!");
        OdtUtils.checkNotEmpty(repoName, "Invalid repo name!");

        LOG.log(Level.FINE, "Fetching {0}/{1} tags.", new Object[]{organization, repoName});

        try {
            GitHubClient client = new GitHubClient();
            RepositoryService service = new RepositoryService(client);
            Repository repo = service.getRepository(organization, repoName);
            List<RepositoryTag> tags = service.getTags(repo);
            return tags;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    /**
     * Reads a local project current branch.
     *
     * @param projectPath path to project root folder (the one that _contains_
     * the .git folder)
     * @return the current branch of provided repo
     */
    public static String readRepoCurrentBranch(File projectPath) {
        checkNotNull(projectPath);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            org.eclipse.jgit.lib.Repository repo = builder.setGitDir(new File(projectPath, ".git"))
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();
            return repo.getBranch();
        }
        catch (IOException ex) {
            throw new RuntimeException("Couldn't read current branch from " + projectPath.getAbsolutePath());
        }
    }

    /**
     * Returns major.minor as string
     */
    public static String majorMinor(SemVersion version) {
        return version.getMajor() + "." + version.getMinor();
    }

    ;
    
    /**
     * Constructs a SemVersion out of a release tag, like i.e. josman-1.2.3
     */
    public static SemVersion version(String repoName, String releaseTag) {
        String versionString = releaseTag.replace(repoName + "-", "");
        return SemVersion.of(versionString);
    }

    /**
     *
     * @param repoName i.e. "josman"
     * @param major
     * @param minor
     * @param tags
     * @throws NotFoundException if tag is not found
     * @return
     */
    public static RepositoryTag find(String repoName, int major, int minor, Iterable<RepositoryTag> tags) {
        for (RepositoryTag tag : tags) {
            SemVersion tagVersion = version(repoName, tag.getName());
            if (tagVersion.getMajor() == major
                    && tagVersion.getMinor() == minor) {
                return tag;
            }
        }
        throw new NotFoundException("Couldn't find any tag matching " + major + "." + minor + " pattern");
    }

    /**
     * Returns a semantic version from branch name in the format "branch-x.y".
     * Result patch number will be 0.
     *
     * @param branchName Must be in format "branch-x.y"
     * @throws IllegalArguemntException if branchname is not in the expected
     * format.
     */
    public static SemVersion versionFromBranchName(String branchName) {
        checkNotNull(branchName);

        if (!branchName.startsWith("branch-")) {
            throw new IllegalArgumentException("Tried to extract version from branch name '" + branchName + "', but it does not start with 'branch-'  !!!");
        }

        try {
            SemVersion ret = SemVersion.of(branchName.replace("branch-", "").concat(".0"));
            return ret;
        }
        catch (Throwable tr) {
            throw new IllegalArgumentException("Error while extracting version from branch name " + branchName, tr);
        }

    }

    /**
     * Adds the candidate tag to the provided tags if no other tag has same
     * major and minor or if its major.minor-patch are greater than provided
     * tags.
     */
    @Nullable
    private static void updateTags(String repoName, RepositoryTag candidateTag, List<RepositoryTag> tags) {

        SemVersion candidateSemVarsion = version(repoName, candidateTag.getName());

        for (int i = 0; i < tags.size(); i++) {
            RepositoryTag tag = tags.get(i);
            SemVersion semVersion = version(repoName, tag.getName());
            if (candidateSemVarsion.getMajor() == semVersion.getMajor()
                    && candidateSemVarsion.getMinor() == semVersion.getMinor()
                    && candidateSemVarsion.getPatch() > semVersion.getPatch()) {
                tags.set(i, candidateTag);
                return;
            }
        }
        tags.add(candidateTag);
    }

    /**
     * Returns new sorted map of only version tags of the format repoName-x.y.z
     * filtered tags, the latter having the highest version.
     *
     * @param repoName the github repository name i.e. josman
     * @param tags a list of tags from the repository
     */
    public static SortedMap<String, RepositoryTag> filterTags(String repoName, List<RepositoryTag> tags) {

        List<RepositoryTag> ret = new ArrayList();
        for (RepositoryTag candidateTag : tags) {
            if (candidateTag.getName().startsWith(repoName + "-")) {
                updateTags(repoName, candidateTag, ret);
            }
        }

        TreeMap map = new TreeMap();
        for (RepositoryTag tag : ret) {
            map.put(tag.getName(), tag);
        }
        return map;
    }

    /**
     * Returns the release tag formed by inserting a minus between the repoName
     * and the version
     *
     * @param repoName i.e. josman
     * @param version i.e. 1.2.3
     * @return i.e. josman-1.2.3
     */
    public static String releaseTag(String repoName, SemVersion version) {
        return repoName + "-" + version;
    }

    /**
     * Returns the github repo url, i.e.
     * https://github.com/opendatatrentino/josman
     *
     * @param organization i.e. opendatatrentino
     * @param name i.e. josman
     */
    public static String repoUrl(String organization, String name) {
        return "https://github.com/" + organization + "/" + name;
    }

    /**
     * Returns the github release code url, i.e.
     * https://github.com/opendatatrentino/josman/blob/todo-releaseTag
     *
     * @param repoName i.e. josman
     * @param version i.e. 1.2.3
     */
    public static String repoRelease(String organization, String repoName, SemVersion version) {
        return repoUrl(organization, repoName) + "/blob/" + releaseTag(repoName, version);
    }

    /**
     * Returns the github wiki url, i.e.
     * https://github.com/opendatatrentino/josman/wiki
     *
     * @param organization i.e. opendatatrentino
     * @param repoName i.e. josman
     */
    public static String repoWiki(String organization, String repoName) {
        return repoUrl(organization, repoName) + "/wiki";
    }

    /**
     * Returns the github issues url, i.e.
     * https://github.com/opendatatrentino/josman/issues
     *
     * @param organization i.e. opendatatrentino
     * @param repoName i.e. josman
     */
    public static String repoIssues(String organization, String repoName) {
        return repoUrl(organization, repoName) + "/issues";
    }

    /**
     * Returns the github milestones url, i.e.
     * https://github.com/opendatatrentino/josman/milestones
     *
     * @param organization i.e. opendatatrentino
     * @param repoName i.e. josman
     */
    public static String repoMilestones(String organization, String repoName) {
        return repoUrl(organization, repoName) + "/milestones";
    }

    /**
     * Returns the github wiki url, i.e.
     *
     * @param organization i.e. opendatatrentino
     * @param repoName i.e. josman
     */
    public static String repoWebsite(String organization, String repoName) {
        return "https://" + organization + ".github.io/" + repoName;
    }

    public static SemVersion latestVersion(String repoName, List<RepositoryTag> tags) {
        OdtUtils.checkNotEmpty(tags, "Invalid repository tags!");
        SortedMap<String, RepositoryTag> filteredTags = Josmans.filterTags(repoName, tags);
        if (filteredTags.isEmpty()) {
            throw new NotFoundException("Couldn't find any released version!");
        }
        return Josmans.version(repoName, filteredTags.lastKey());
    }

    /**
     * Returns a new url friendly path.
     *
     * @param path a path that may contain .md files
     */
    public static String htmlizePath(String path) {
        if (path.endsWith("README.md")) {
            return path.replace("README.md", "index.html");
        } else if (path.endsWith(".md")) {
            return path.substring(0, path.length() - 3) + ".html";
        }
        return path;
    }

    /**
     *
     * @return non-null input string
     * @param string
     * @throws IllegalArgumentException on empty string
     */
    public static String checkNotMeaningful(@Nullable String string, @Nullable Object prependedErrorMessage) {
        OdtUtils.checkNotEmpty(string, prependedErrorMessage);
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) != '\n' && string.charAt(i) != '\t' && string.charAt(i) != ' ') {
                return string;
            }
        }
        throw new IllegalArgumentException(String.valueOf(prependedErrorMessage) + " -- Reason: String contains only empty spaces/tabs/carriage returns!");
    }

    /**
     * Returns the maven style javadoc file name (i.e.
     * my-prog-1.2.3-javadoc.jar)
     */
    public static String javadocJarName(String artifactId, SemVersion version) {
        checkNotEmpty(artifactId, "Invalid artifactId!");
        checkNotNull(version);
        return artifactId + "-" + version + "-javadoc.jar";
    }

    /**
     * Fetches Javadoc of released artifact and writes it into {@code destFile}
     *
     * @param destFile must exists.
     */
    public static File fetchJavadoc(String groupId, String artifactId, SemVersion version) {
        checkNotEmpty(groupId, "Invalid groupId!");
        checkNotEmpty(artifactId, "Invalid artifactId!");
        checkNotNull(version);

        File destFile;

        try {
            destFile = File.createTempFile(groupId + "-" + artifactId + "-javadoc", ".jar");
            destFile.deleteOnExit();
        }
        catch (IOException ex) {
            throw new RuntimeException("Couldn't create target javadoc file!", ex);
        }

        URL url;
        try {
            url = new URL("http://repo1.maven.org/maven2/" + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + javadocJarName(artifactId, version));
        }
        catch (MalformedURLException ex) {
            throw new RuntimeException("Error while forming javadoc URL!", ex);
        }
        LOG.log(Level.INFO, "Fetching javadoc from {0} into {1} ...", new Object[]{url, destFile.getAbsolutePath()});
        try {
            FileUtils.copyURLToFile(url, destFile, CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        }
        catch (IOException ex) {
            throw new RuntimeException("Error while fetch-and-write javadoc for " + groupId + "/" + artifactId + "-" + version + " into file " + destFile.getAbsoluteFile(), ex);
        }
        return destFile;
    }

    /**
     * Extracts the directory at resource path to target directory. First
     * directory is searched in local "src/main/resources" so the thing also
     * works when developing in the IDE. If not found then searches in jar file.
     */
    public static void copyDirFromResource(Class clazz, String dirPath, File destDir) {
        String sep = File.separator;
        File sourceDir = new File("src" + sep + "main" + sep + "resources", dirPath);

        if (sourceDir.exists()) {
            LOG.log(Level.INFO, "Copying directory from {0} to {1}", new Object[]{sourceDir.getAbsolutePath(), destDir.getAbsolutePath()});
            try {
                FileUtils.copyDirectory(sourceDir, destDir);
            }
            catch (IOException ex) {
                throw new RuntimeException("Couldn't copy the directory!", ex);
            }
        } else {
            final File jarFile = new File(clazz.getProtectionDomain().getCodeSource().getLocation().getPath());

            copyDirFromJar(jarFile, destDir, dirPath);

        }
    }

    /**
     *
     * Extracts the files starting with dirPath from {@code file} to
     * {@code destDir}
     *
     * @param dirPath the prefix used for filtering. If empty the whole jar
     * content is extracted.
     */
    public static void copyDirFromJar(File file, File destDir, String dirPath) {
        checkNotNull(file);
        checkNotNull(destDir);
        checkNotNull(dirPath);

        String normalizedDirPath;
        if (dirPath.startsWith("/")) {
            normalizedDirPath = dirPath.substring(1);
        } else {
            normalizedDirPath = dirPath;
        }

        try {
            JarFile jar = new JarFile(file);
            java.util.Enumeration enumEntries = jar.entries();
            while (enumEntries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumEntries.nextElement();
                if (jarEntry.getName().startsWith(normalizedDirPath)) {
                    File f = new File(
                            destDir
                            + File.separator
                            + jarEntry
                            .getName()
                            .substring(normalizedDirPath.length()));

                    if (jarEntry.isDirectory()) { // if its a directory, create it
                        f.mkdirs();
                        continue;
                    } else {
                        f.getParentFile().mkdirs();
                    }

                    InputStream is = jar.getInputStream(jarEntry); // get the input stream
                    FileOutputStream fos = new FileOutputStream(f);
                    while (is.available() > 0) {  // write contents of 'is' to 'fos'
                        fos.write(is.read());
                    }
                    fos.close();
                    is.close();
                }

            }
        }
        catch (Exception ex) {
            throw new RuntimeException("Error while extracting jar file! Jar source: " + file.getAbsolutePath() + " destDir = " + destDir.getAbsolutePath(), ex);
        }
    }

    /**
     * Searches resource indicated by path first in src/main/resources (so it
     * works even when developing), then in proper classpath resources. If
     * resource is found it is returned as input stream, otherwise an exception
     * is thrown.
     *
     * @throws NotFoundException if path can't be found.
     */
    public static InputStream findResourceStream(String path) {

        checkNotNull(path, "invalid path!");

        String localPath = "src/main/resources" + path;

        try {
            return new FileInputStream(localPath);
        }
        catch (FileNotFoundException ex) {
        }

        LOG.log(Level.INFO, "Can''t find file {0}", new File(localPath).getAbsolutePath());

        try {
            URL url = JosmanProject.class.getResource(path);
            LOG.log(Level.INFO, "Found file in {0}", url);
            InputStream ret = JosmanProject.class.getResourceAsStream(path);
            return ret;
        }
        catch (Exception ex) {
            throw new NotFoundException("Can't load file in resources! " + path);
        }

    }

    /**
     * Returns the target file where a source path should be transfered into.
     *
     * @param relPath path relative to the {@link #sourceRepoDir} (i.e.
     * LICENSE.txt or docs/README.md)
     */
    static File targetFile(File pagesDir, String relPath, final SemVersion version) {

        if (Josmans.isRootpath(relPath)) {
            return new File(
                    pagesDir,
                    Josmans.htmlizePath(relPath));
        } else {
            return new File(
                    pagesDir,
                    Josmans.majorMinor(version)
                    + File.separator
                    + Josmans.htmlizePath(relPath.substring("docs/".length())));
        }

    }

    /**
     * Returns either "../" or "" according to {@code relPath}
     *
     * @param relPath may start with "docs"
     */
    static String prependedPath(String relPath) {
        checkNotNull(relPath);
        // todo it handles only one level....
        if (isRootpath(relPath)) {
            return "";
        } else {
            return "../";

        }
    }

    /**
     * Returns true if provided {@code relPath} is a website root path
     *
     * @param relPath the website path, i.e. README.md or docs/CHANGES.md or
     * docs\CHANGES.md
     */
    static boolean isRootpath(String relPath) {
        checkNotNull(relPath);
        return !(relPath.equals("docs")
                || relPath.startsWith("docs/")
                || relPath.startsWith("docs\\"));
    }

    /**
     * Returns a string representation of the provided git file mode
     */
    static String gitFileModeToString(FileMode fileMode) {
        if (fileMode.equals(FileMode.EXECUTABLE_FILE)) {
            return "Executable File";
        } else if (fileMode.equals(FileMode.REGULAR_FILE)) {
            return "Normal File";
        } else if (fileMode.equals(FileMode.TREE)) {
            return "Directory";
        } else if (fileMode.equals(FileMode.SYMLINK)) {
            return "Symlink";
        } else if (fileMode.equals(FileMode.GITLINK)) {
            return "submodule link";
        } else {
            return fileMode.toString();
        }

    }
}
