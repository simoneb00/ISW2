package model;

import java.util.ArrayList;
import java.util.List;

public class Ticket {

    public String id;
    public String key;
    public ArrayList<Release> affectedVersions = new ArrayList<>();
    public Release fixVersion;    // fix version
    public Release openingVersion;      // opening version
    public Release injectedVersion = null;
    public List<String> affectedComponents = new ArrayList<>();
    public float proportion = 0;


}
