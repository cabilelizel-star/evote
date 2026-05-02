package com.evote.model;

public class Voter {
    private String voterId;
    private String name;
    private boolean hasVoted;

    public Voter() {}
    public Voter(String voterId, String name, boolean hasVoted) {
        this.voterId  = voterId;
        this.name     = name;
        this.hasVoted = hasVoted;
    }

    public String  getVoterId()  { return voterId; }
    public String  getName()     { return name; }
    public boolean isHasVoted()  { return hasVoted; }
    public void    setVoterId(String v)   { this.voterId  = v; }
    public void    setName(String n)      { this.name     = n; }
    public void    setHasVoted(boolean h) { this.hasVoted = h; }
}
