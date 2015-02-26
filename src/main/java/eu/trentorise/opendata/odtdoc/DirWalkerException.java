package eu.trentorise.opendata.odtdoc;

import java.io.File;
import javax.annotation.Nullable;

/**
 *
 * @author David Leoni
 */
public class DirWalkerException extends RuntimeException {

    File source;
    File destination;
    
    public DirWalkerException(String string, @Nullable File source, @Nullable File destination, Throwable thrwbl) {                        
        super(string, thrwbl);
        
    }
        
    public DirWalkerException(String msg, @Nullable File source, @Nullable File destination) {
        super(msg);
        this.source = source;
        this.destination = destination;
    }

    @Override
    public String getMessage() {
        String m = "";
        if (source != null){
            m += " \n Source file was " + source.getAbsolutePath() + " .";
        }
        if (destination != null){
            m += " \n Destination file was " + destination.getAbsolutePath() + " .";
        }
        return super.getMessage() + m; 
    }
    
    
    
}
