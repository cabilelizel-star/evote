package com.evote.model;

public class Candidate {
    private String candidateId;
    private String name;
    private String party;
    private int    voteCount;
    private String electionType; // e.g. "Local Government Election", "Student Council Election"
    private byte[] photo;

    public Candidate() {}
    public Candidate(String candidateId, String name, String party, int voteCount) {
        this.candidateId = candidateId;
        this.name        = name;
        this.party       = party;
        this.voteCount   = voteCount;
    }

    public String getCandidateId()  { return candidateId; }
    public String getName()         { return name; }
    public String getParty()        { return party; }
    public int    getVoteCount()    { return voteCount; }
    public String getElectionType() { return electionType; }

    public void setCandidateId(String v)  { this.candidateId  = v; }
    public void setName(String v)         { this.name         = v; }
    public void setParty(String v)        { this.party        = v; }
    public void setVoteCount(int v)       { this.voteCount    = v; }
    public void setElectionType(String v) { this.electionType = v; }
    public byte[] getPhoto() { return photo; }
    public void setPhoto(byte[] v) { this.photo = v; }
    public boolean hasPhoto() { return photo != null && photo.length > 0; }

    /** Returns a short badge label for the election type */
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
