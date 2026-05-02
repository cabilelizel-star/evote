package com.evote.model;

public class Voter {
    private String voterId;
    private String name;
    private String email;
    private String birthday;
    private Integer age;
    private String placeOfBirth;
    private String gender;
    private String contactNumber;
    private boolean hasVoted;

    public Voter() {}
    public Voter(String voterId, String name, boolean hasVoted) {
        this.voterId  = voterId;
        this.name     = name;
        this.hasVoted = hasVoted;
    }

    public String  getVoterId()       { return voterId; }
    public String  getName()          { return name; }
    public String  getEmail()         { return email; }
    public String  getBirthday()      { return birthday; }
    public Integer getAge()           { return age; }
    public String  getPlaceOfBirth()  { return placeOfBirth; }
    public String  getGender()        { return gender; }
    public String  getContactNumber() { return contactNumber; }
    public boolean isHasVoted()       { return hasVoted; }

    public void setVoterId(String v)       { this.voterId       = v; }
    public void setName(String v)          { this.name          = v; }
    public void setEmail(String v)         { this.email         = v; }
    public void setBirthday(String v)      { this.birthday      = v; }
    public void setAge(Integer v)          { this.age           = v; }
    public void setPlaceOfBirth(String v)  { this.placeOfBirth  = v; }
    public void setGender(String v)        { this.gender        = v; }
    public void setContactNumber(String v) { this.contactNumber = v; }
    public void setHasVoted(boolean v)     { this.hasVoted      = v; }
}
