/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.trentorise.opendata.odtdoc;

import static com.google.common.base.Preconditions.checkNotNull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author David Leoni
 */
@Immutable
public class DirWalker extends DirectoryWalker {

    private static final Logger LOG = Logger.getLogger(DirWalker.class.getName());

    private File sourceRoot;
    private File destinationRoot;
    private OdtDoc odtDoc;

    public DirWalker(File sourceRoot, File destinationRoot, OdtDoc odtDoc) {
        super();
        checkNotNull(sourceRoot);
        checkNotNull(destinationRoot);
        checkNotNull(odtDoc);
        this.sourceRoot = sourceRoot;
        this.destinationRoot = destinationRoot;
        this.odtDoc = odtDoc;
    }

    public void process() {
        try {
            walk(sourceRoot, new ArrayList());
        } catch (IOException ex) {
            throw new RuntimeException("Error while copying root " + sourceRoot.getAbsolutePath(), ex);
        }
    }

    ;
    
    
    @Override
    protected boolean handleDirectory(File directory, int depth, Collection results) {
        LOG.info("Processing directory " + directory.getAbsolutePath());
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

    @Override
    protected void handleFile(File file, int depth, Collection results) throws IOException {

        String targetRelPath = file.getAbsolutePath().replace(sourceRoot.getAbsolutePath(), "");
                
        if (file.getName().endsWith(".md")) {            
            File target = new File(destinationRoot, targetRelPath.substring(0, targetRelPath.length()-3) + ".html");
            if (target.exists()) {
                throw new DirWalkerException("Target file already exists!", file, target);
            }
            LOG.log(Level.INFO, "Creating file {0}", target.getAbsolutePath());
            odtDoc.buildMd(file, target, "../");
        } else {
            File target = new File(destinationRoot, targetRelPath);
            LOG.log(Level.INFO, "Copying file into {0}", target.getAbsolutePath());
            FileUtils.copyFile(file, target);
        }

    }

    public File getSourceRoot() {
        return sourceRoot;
    }

    public File getDestinationRoot() {
        return destinationRoot;
    }

    public OdtDoc getOdtDoc() {
        return odtDoc;
    }

}
