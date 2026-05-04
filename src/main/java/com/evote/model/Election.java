package com.evote.model;

public class Election {
    private int     id;
    private String  title;
    private boolean open;
    private String  startTime;
    private String  endTime;
    private String  electionType;
    private String  organization;

    // Election type constants
    public static final String TYPE_NATIONAL       = "National Election";
    public static final String TYPE_LOCAL          = "Local Government Election";
    public static final String TYPE_BARANGAY       = "Barangay-Level Voting";
    public static final String TYPE_SCHOOL         = "School Election";
    public static final String TYPE_STUDENT        = "Student Council Election";
    public static final String TYPE_ORGANIZATION   = "Organization Voting";

    public Election() {}
    public Election(int id, String title, boolean open) {
        this.id    = id;
        this.title = title;
        this.open  = open;
    }

    public int     getId()            { return id; }
    public String  getTitle()         { return title; }
    public boolean isOpen()           { return open; }
    public String  getStartTime()     { return startTime; }
    public String  getEndTime()       { return endTime; }
    public String  getElectionType()  { return electionType; }
    public String  getOrganization()  { return organization; }

    public void setId(int v)               { this.id           = v; }
    public void setTitle(String v)         { this.title        = v; }
    public void setOpen(boolean v)         { this.open         = v; }
    public void setStartTime(String v)     { this.startTime    = v; }
    public void setEndTime(String v)       { this.endTime      = v; }
    public void setElectionType(String v)  { this.electionType = v; }
    public void setOrganization(String v)  { this.organization = v; }

    /** Returns the icon emoji for the current election type */
    public String getTypeIcon() {
        if (electionType == null) return "🗳";
        switch (electionType) {
            case "National Election":         return "🇵🇭";
            case "Local Government Election": return "🏛";
            case "Barangay-Level Voting":     return "🏘";
            case "School Election":           return "🏫";
            case "Student Council Election":  return "🎓";
            case "Organization Voting":       return "🏢";
            default:                          return "🗳";
        }
    }
}
