/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.trentorise.opendata.jedoc;

import static com.google.common.base.Preconditions.checkNotNull;
import eu.trentorise.opendata.commons.NotFoundException;
import static eu.trentorise.opendata.commons.OdtUtils.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Utilities for Jedoc
 *
 * @author David Leoni
 */
public class Jedocs {

    private static final Logger LOG = Logger.getLogger(Jedocs.class.getName());

    /**
     * Reading file with Jgit:
     * https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/api/ReadFileFromCommit.java
     */
    /**
     * Fetches all tags from a github repository. Beware of API limits of 60
     * requests per hour
     */
    public static List<RepositoryTag> fetchTags(String organization, String repoName) {
        checkNotEmpty(organization, "Invalid organization!");
        checkNotEmpty(repoName, "Invalid repo name!");

        LOG.log(Level.FINE, "Fetching {0}/{1} tags.", new Object[]{organization, repoName});

        try {
            GitHubClient client = new GitHubClient();
            RepositoryService service = new RepositoryService(client);
            Repository repo = service.getRepository(organization, repoName);
            List<RepositoryTag> tags = service.getTags(repo);
            return tags;
        } catch (Exception ex) {
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
        } catch (IOException ex) {
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
     * Constructs a SemVersion out of a release tag, like i.e. jedoc-1.2.3
     */
    public static SemVersion version(String repoName, String releaseTag) {
        String versionString = releaseTag.replace(repoName + "-", "");
        return SemVersion.of(versionString);
    }
    
    /**
     * 
     * @param repoName i.e. "jedoc"
     * @param major
     * @param minor
     * @param tags
     * @throws NotFoundException if tag is not found
     * @return 
     */
    public static RepositoryTag find(String repoName, int major, int minor, Iterable<RepositoryTag> tags){
        for (RepositoryTag tag : tags){
            SemVersion tagVersion = version(repoName,tag.getName());
            if (tagVersion.getMajor() == major
                    && tagVersion.getMinor() == minor){
                return tag;
            }
        }
        throw new NotFoundException("Couldn't find any tag matching " + major + "."+minor + " pattern");
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
        } catch (Throwable tr) {
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
     * @param repoName the github repository name i.e. jedoc
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
     * @param repoName i.e. jedoc
     * @param version i.e. 1.2.3
     * @return i.e. jedoc-1.2.3
     */
    public static String releaseTag(String repoName, SemVersion version) {
        return repoName + "-" + version;
    }
    
    
    /**
     * Returns the github repo url, i.e.
     * https://github.com/opendatatrentino/jedoc
     *
     * @param organization i.e. opendatatrentino
     * @param name i.e. jedoc
     */
    public static String repoUrl(String organization, String name) {
        return "https://github.com/" + organization + "/" + name;
    }

    /**
     * Returns the github release code url, i.e.
     * https://github.com/opendatatrentino/jedoc/blob/todo-releaseTag
     *
     * @param repoName i.e. jedoc
     * @param version i.e. 1.2.3
     */
    public static String repoRelease(String organization, String repoName, SemVersion version) {
        return repoUrl(organization, repoName) + "/blob/" + releaseTag(repoName, version);
    }

    /**
     * Returns the github wiki url, i.e.
     * https://github.com/opendatatrentino/jedoc/wiki
     *
     * @param organization i.e. opendatatrentino
     * @param repoName i.e. jedoc
     */
    public static String repoWiki(String organization, String repoName) {
        return repoUrl(organization, repoName) + "/wiki";
    }

    /**
     * Returns the github issues url, i.e.
     * https://github.com/opendatatrentino/jedoc/issues
     *
     * @param organization i.e. opendatatrentino
     * @param repoName i.e. jedoc
     */
    public static String repoIssues(String organization, String repoName) {
        return repoUrl(organization, repoName) + "/issues";
    }

    /**
     * Returns the github milestones url, i.e.
     * https://github.com/opendatatrentino/jedoc/milestones
     *
     * @param organization i.e. opendatatrentino
     * @param repoName i.e. jedoc
     */
    public static String repoMilestones(String organization, String repoName) {
        return repoUrl(organization, repoName) + "/milestones";
    }

    /**
     * Returns the github wiki url, i.e.
     *
     * @param organization i.e. opendatatrentino
     * @param repoName i.e. jedoc
     */
    public static String repoWebsite(String organization, String repoName) {
        return "https://" + organization + ".github.io/" + repoName;
    }

    public static SemVersion latestVersion(String repoName, List<RepositoryTag> tags) {
        return Jedocs.version(repoName, Jedocs.filterTags(repoName, tags).lastKey());
    }

    /**
     * Returns a new path
     *
     * @path a path that may contain .md files
     */
    public static String htmlizePath(String path) {
        if (path.endsWith("README.md")) {
            return path.replace("README.md", "index.html");
        } else if (path.endsWith(".md")) {
            return path.substring(0, path.length() - 3) + ".html";
        }
        return path;
    }

}
