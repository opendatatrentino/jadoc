package eu.trentorise.opendata.josman.org.pegdown;

import eu.trentorise.opendata.josman.org.pegdown.ast.VerbatimNode;

public interface VerbatimSerializer {
    static final String DEFAULT = "DEFAULT";

    void serialize(VerbatimNode node, Printer printer);
}
