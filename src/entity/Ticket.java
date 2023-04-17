package entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

public class Ticket {

    public String id;
    public String key;
    public HashMap<String, LocalDateTime> affectedVersions = new HashMap<>();
    public LocalDateTime resolutionDate;    // fix version
    public LocalDateTime creationDate;      // opening version
    public HashMap<String, LocalDateTime> injectedVersion = new HashMap<>();
    public float proportion = 0;


}
