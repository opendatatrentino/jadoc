package eu.trentorise.opendata.jedoc.org.pegdown;

import eu.trentorise.opendata.jedoc.org.pegdown.ast.VerbatimNode;

public interface VerbatimSerializer {
    static final String DEFAULT = "DEFAULT";

    void serialize(VerbatimNode node, Printer printer);
}
