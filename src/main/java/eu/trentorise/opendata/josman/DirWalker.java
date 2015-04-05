package eu.trentorise.opendata.josman;

import static com.google.common.base.Preconditions.checkNotNull;
import eu.trentorise.opendata.commons.NotFoundException;
import eu.trentorise.opendata.commons.SemVersion;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

/**
 * Copies directory to destination and converts md files to html.
 * @author David Leoni
 */
@Immutable
public class DirWalker extends DirectoryWalker {
    private static final Logger LOG = Logger.getLogger(DirWalker.class.getName());

    
    
    private File sourceRoot;
    private File destinationRoot;
    private JosmanProject project;
    SemVersion version;

    /**
     * @throws NotFoundException if source root doesn't exists    
     */
    public DirWalker(
                    File sourceRoot, 
                    File destinationRoot, 
                    JosmanProject josman, 
                    SemVersion version) {
        super();
        checkNotNull(sourceRoot);
        if (!sourceRoot.exists()) {
            throw new NotFoundException("source root does not exists: " + sourceRoot.getAbsolutePath());
        }
        checkNotNull(destinationRoot);
        if (destinationRoot.exists()) {
            throw new NotFoundException("destination directory does already exists: " + destinationRoot.getAbsolutePath());
        }
        checkNotNull(josman);
        checkNotNull(version);
        this.sourceRoot = sourceRoot;
        this.destinationRoot = destinationRoot;
        this.project = josman;
        this.version = version;
    }

    public void process() {
        try {
            walk(sourceRoot, new ArrayList());
        } catch (IOException ex) {
            throw new RuntimeException("Error while copying root " + sourceRoot.getAbsolutePath(), ex);
        }
    }   
    
    /**
     * Copies directory content to destination 
     * @param depth
     * @param results ignored
     */
    @Override
    protected boolean handleDirectory(File directory, int depth, Collection results) {
        LOG.log(Level.INFO, "Processing directory {0}", directory.getAbsolutePath());
        File target = new File(destinationRoot, directory.getAbsolutePath().replace(sourceRoot.getAbsolutePath(), ""));
        if (target.exists()) {
            throw new RuntimeException("Target directory already exists!! " + target.getAbsolutePath());
        }
        LOG.log(Level.INFO, "Creating target    directory {0}", target.getAbsolutePath());
        if (!target.mkdirs()) {
            throw new RuntimeException("Couldn't create directory!! " + target.getAbsolutePath());
        }
        return true;
    }

    /**
     * Converts .md to .html and README.md to index.html . Other files are just
     * copied
     */
    @Override
    protected void handleFile(File file, int depth, Collection results) throws IOException {       
        String targetRelPath = file.getAbsolutePath().replace(sourceRoot.getAbsolutePath(), "");
        
        project.copyStream(
                            new FileInputStream(file), 
                            "docs" + File.separator  + targetRelPath, 
                            version);
    }

    public File getSourceRoot() {
        return sourceRoot;
    }

    public File getDestinationRoot() {
        return destinationRoot;
    }

    public JosmanProject getProject() {
        return project;
    }

    public SemVersion getVersion() {
        return version;
    }

}
