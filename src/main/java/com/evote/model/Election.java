package com.evote.model;

public class Election {
    private int     id;
    private String  title;
    private boolean open;

    public Election() {}
    public Election(int id, String title, boolean open) {
        this.id    = id;
        this.title = title;
        this.open  = open;
    }

    public int     getId()    { return id; }
    public String  getTitle() { return title; }
    public boolean isOpen()   { return open; }
    public void    setId(int v)       { this.id    = v; }
    public void    setTitle(String v) { this.title = v; }
    public void    setOpen(boolean v) { this.open  = v; }
}
