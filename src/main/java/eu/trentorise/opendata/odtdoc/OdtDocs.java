/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.trentorise.opendata.odtdoc;

import static eu.trentorise.opendata.commons.OdtUtils.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
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

/**
 * Utilities for OdtDoc
 * @author David Leoni
 */
public class OdtDocs {

    private static final Logger LOG = Logger.getLogger(OdtDocs.class.getName());

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
     * Returns major.minor as string
     */
    public static String majorMinor(SemVersion version){
        return version.getMajor() + "." + version.getMinor();
    };
    
    /**
     * Constructs a SemVersion out of a release tag, like i.e. odt-doc-1.2.3
     */
    public static SemVersion version(String repoName, String releaseTag) {
        String versionString = releaseTag.replace(repoName + "-", "");
        return SemVersion.of(versionString);
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
     * @param repoName the github repository name i.e. odt-doc
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

        /*
    private String version = "0.3.1";
    private String releaseTag = repoName + "-" + version;
    private String repoRelease = repoUrl + "/blob/" + releaseTag + "/";    
    */
    
    /**
     * Returns the release tag formed by inserting a minus between the repoName and the version
     * @param repoName i.e. odt-doc
     * @param version i.e. 1.2.3
     * @return i.e. odt-doc-1.2.3
     */
    static public String releaseTag(String repoName, String version){
        return repoName + "-" + version;
    }        
                    
    /**
     * Returns the github release code url, i.e. 
     * @param organization i.e. opendatatrentino
     * @param name i.e. odt-doc     
     * @return i.e. https://github.com/opendatatrentino/odt-doc
     */
    static public String repoUrl(String organization, String name){
        return "https://github.com/" + organization + "/" + name;
    }      

    /**
     * Returns the github release code url, i.e. 
     * @param repoName i.e. odt-doc
     * @param version i.e. 1.2.3     
     */
    static public String repoRelease(String organization, String repoName, String version){
        return repoUrl(organization, repoName)+ "-" + version;
    }    

    /**
     * Returns the github wiki url, i.e. 
     * @param organization i.e. opendatatrentino
     * @param repoName i.e. odt-doc     
     */
    static public String repoWiki(String organization, String repoName){
        return repoUrl(organization, repoName) + "/wiki";
    }    

    /**
     * Returns the github wiki url, i.e. 
     * @param organization i.e. opendatatrentino
     * @param repoName i.e. odt-doc     
     */
    static public String repoWebsite(String organization, String repoName){
        return "https://" + organization + ".github.io/" + repoName;
    }    
    
    static public SemVersion latestVersion(String repoName, List<RepositoryTag> tags){
        return OdtDocs.version(repoName, OdtDocs.filterTags(repoName, tags).lastKey());
    }
    
}
