package com.evote.model;

public class Voter {
    private String  voterId;
    private String  name;
    private String  firstName;
    private String  middleName;
    private String  lastName;
    private String  dateOfBirth;
    private String  gender;
    private String  street;
    private String  barangay;
    private String  city;
    private String  province;
    private String  zipCode;
    private String  mobileNumber;
    private String  email;
    private String  voterIdNumber;
    private String  votingDistrict;
    private String  affiliation;
    private String  idType;
    private String  idNumber;
    private boolean hasVoted;
    private String  status; // pending, approved, rejected
    private String  rejectionReason;

    public Voter() {}
    public Voter(String voterId, String name, boolean hasVoted) {
        this.voterId  = voterId;
        this.name     = name;
        this.hasVoted = hasVoted;
    }

    // Getters
    public String  getVoterId()        { return voterId; }
    public String  getName()           { return name; }
    public String  getFirstName()      { return firstName; }
    public String  getMiddleName()     { return middleName; }
    public String  getLastName()       { return lastName; }
    public String  getDateOfBirth()    { return dateOfBirth; }
    public String  getGender()         { return gender; }
    public String  getStreet()         { return street; }
    public String  getBarangay()       { return barangay; }
    public String  getCity()           { return city; }
    public String  getProvince()       { return province; }
    public String  getZipCode()        { return zipCode; }
    public String  getMobileNumber()   { return mobileNumber; }
    public String  getEmail()          { return email; }
    public String  getVoterIdNumber()  { return voterIdNumber; }
    public String  getVotingDistrict() { return votingDistrict; }
    public String  getAffiliation()    { return affiliation; }
    public String  getIdType()         { return idType; }
    public String  getIdNumber()       { return idNumber; }
    public boolean isHasVoted()        { return hasVoted; }

    // Setters
    public void setVoterId(String v)        { this.voterId        = v; }
    public void setName(String v)           { this.name           = v; }
    public void setFirstName(String v)      { this.firstName      = v; }
    public void setMiddleName(String v)     { this.middleName     = v; }
    public void setLastName(String v)       { this.lastName       = v; }
    public void setDateOfBirth(String v)    { this.dateOfBirth    = v; }
    public void setGender(String v)         { this.gender         = v; }
    public void setStreet(String v)         { this.street         = v; }
    public void setBarangay(String v)       { this.barangay       = v; }
    public void setCity(String v)           { this.city           = v; }
    public void setProvince(String v)       { this.province       = v; }
    public void setZipCode(String v)        { this.zipCode        = v; }
    public void setMobileNumber(String v)   { this.mobileNumber   = v; }
    public void setEmail(String v)          { this.email          = v; }
    public void setVoterIdNumber(String v)  { this.voterIdNumber  = v; }
    public void setVotingDistrict(String v) { this.votingDistrict = v; }
    public void setAffiliation(String v)    { this.affiliation    = v; }
    public void setIdType(String v)         { this.idType         = v; }
    public void setIdNumber(String v)       { this.idNumber       = v; }
    public void setHasVoted(boolean v)      { this.hasVoted       = v; }
    public void setPassword(String v)       { /* not stored in model */ }
    public String  getStatus()              { return status; }
    public String  getRejectionReason()     { return rejectionReason; }
    public void    setStatus(String v)      { this.status           = v; }
    public void    setRejectionReason(String v) { this.rejectionReason = v; }
    public boolean isApproved()             { return "approved".equals(status); }
    public boolean isPending()              { return status == null || "pending".equals(status); }
}
