package com.evote.model;

public class Candidate {
    private String candidateId;
    private String name;
    private String party;
    private int    voteCount;

    public Candidate() {}
    public Candidate(String candidateId, String name, String party, int voteCount) {
        this.candidateId = candidateId;
        this.name        = name;
        this.party       = party;
        this.voteCount   = voteCount;
    }

    public String getCandidateId() { return candidateId; }
    public String getName()        { return name; }
    public String getParty()       { return party; }
    public int    getVoteCount()   { return voteCount; }
    public void   setCandidateId(String v) { this.candidateId = v; }
    public void   setName(String v)        { this.name = v; }
    public void   setParty(String v)       { this.party = v; }
    public void   setVoteCount(int v)      { this.voteCount = v; }
}
