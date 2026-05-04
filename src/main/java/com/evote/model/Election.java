package com.evote.model;

public class Election {
    private int     id;
    private String  title;
    private boolean open;
    private String  startTime;
    private String  endTime;

    public Election() {}
    public Election(int id, String title, boolean open) {
        this.id    = id;
        this.title = title;
        this.open  = open;
    }

    public int     getId()        { return id; }
    public String  getTitle()     { return title; }
    public boolean isOpen()       { return open; }
    public String  getStartTime() { return startTime; }
    public String  getEndTime()   { return endTime; }
    public void    setId(int v)           { this.id        = v; }
    public void    setTitle(String v)     { this.title     = v; }
    public void    setOpen(boolean v)     { this.open      = v; }
    public void    setStartTime(String v) { this.startTime = v; }
    public void    setEndTime(String v)   { this.endTime   = v; }
}
