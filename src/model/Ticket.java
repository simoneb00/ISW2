package model;

import java.util.ArrayList;

public class Ticket {

    public String id;
    public String key;
    public ArrayList<Release> affectedVersions = new ArrayList<>();
    public Release fixVersion;    // fix version
    public Release openingVersion;      // opening version
    public Release injectedVersion;
    public float proportion = 0;


}
