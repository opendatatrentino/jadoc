package eu.trentorise.opendata.josman.test;

import eu.trentorise.opendata.commons.OdtConfig;
import eu.trentorise.opendata.josman.Josmans;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class JosmansTest {
    
        private static final Logger LOG = Logger.getLogger(JosmansTest.class.getName());

    @BeforeClass
    public static void beforeClass() {
        OdtConfig.init(JosmansTest.class);
    }   
    
    @Test
    public void testNotMeaningfulString(){
        
        Josmans.checkNotMeaningful(" a", "");
        Josmans.checkNotMeaningful(" a\n", "");
        Josmans.checkNotMeaningful(" a\t", "");
        
        try {
            Josmans.checkNotMeaningful("", "");
        } catch (IllegalArgumentException ex){
                
        }
        
        try {
            Josmans.checkNotMeaningful(" ", "");
        } catch (IllegalArgumentException ex){
                
        }
        try {
            Josmans.checkNotMeaningful("\\t", "");
        } catch (IllegalArgumentException ex){
                
        }
        
        try {
            Josmans.checkNotMeaningful("\\n", "");
        } catch (IllegalArgumentException ex){
                
        }
                        

    }
}
