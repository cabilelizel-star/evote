package com.evote.service;

import com.evote.model.Candidate;
import com.evote.model.Election;
import com.evote.model.Voter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ElectionService {

    private final JdbcTemplate db;
    private static final int ELECTION_ID = 1;

    public ElectionService(JdbcTemplate db) { this.db = db; }

    // ── Election ──────────────────────────────────────────────────────────────
    public Election getElection() {
        return db.queryForObject(
            "SELECT id, title, is_open FROM elections WHERE id = ?",
            electionMapper(), ELECTION_ID);
    }

    public void setElectionOpen(boolean open) {
        db.update("UPDATE elections SET is_open = ? WHERE id = ?", open, ELECTION_ID);
    }

    // ── Candidates ────────────────────────────────────────────────────────────
    public List<Candidate> getCandidates() {
        return db.query(
            "SELECT candidate_id, name, party, vote_count FROM candidates " +
            "WHERE election_id = ? ORDER BY name",
            candidateMapper(), ELECTION_ID);
    }

    public void addCandidate(String id, String name, String party) {
        db.update("INSERT IGNORE INTO candidates (candidate_id, election_id, name, party) VALUES (?,?,?,?)",
            id, ELECTION_ID, name, party);
    }

    public void removeCandidate(String id) {
        db.update("DELETE FROM candidates WHERE candidate_id = ? AND election_id = ?", id, ELECTION_ID);
    }

    // ── Voters ────────────────────────────────────────────────────────────────
    public List<Voter> getVoters() {
        return db.query(
            "SELECT voter_id, name, has_voted, first_name, middle_name, last_name, " +
            "date_of_birth, gender, street, barangay, city, province, zip_code, " +
            "mobile_number, email, voter_id_number, voting_district, affiliation, " +
            "id_type, id_number FROM voters WHERE election_id = ? ORDER BY name",
            voterMapper(), ELECTION_ID);
    }

    public Optional<Voter> findVoter(String voterId) {
        List<Voter> list = db.query(
            "SELECT voter_id, name, has_voted FROM voters WHERE voter_id = ? AND election_id = ?",
            voterMapper(), voterId, ELECTION_ID);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<Voter> findVoterByIdAndEmail(String voterId, String email) {
        List<Voter> list = db.query(
            "SELECT voter_id, name, has_voted, email FROM voters " +
            "WHERE voter_id = ? AND email = ? AND election_id = ?",
            voterMapper(), voterId, email, ELECTION_ID);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public boolean voterIdExists(String voterId) {
        Integer count = db.queryForObject(
            "SELECT COUNT(*) FROM voters WHERE voter_id = ? AND election_id = ?",
            Integer.class, voterId, ELECTION_ID);
        return count != null && count > 0;
    }

    public void addVoter(String id, String name, String password) {
        db.update("INSERT IGNORE INTO voters (voter_id, election_id, name, password) VALUES (?,?,?,?)",
            id, ELECTION_ID, name, password);
    }

    public void addVoterFull(String voterId, String firstName, String middleName, String lastName,
                              String dob, String gender,
                              String street, String barangay, String city, String province, String zip,
                              String mobile, String email,
                              String voterIdNumber, String votingDistrict, String affiliation,
                              String idType, String idNumber,
                              String password) {
        String name = (firstName + " " + (middleName != null && !middleName.isBlank() ? middleName + " " : "") + lastName).trim();
        db.update("INSERT IGNORE INTO voters " +
            "(voter_id, election_id, name, first_name, middle_name, last_name, date_of_birth, gender, " +
            "street, barangay, city, province, zip_code, mobile_number, email, " +
            "voter_id_number, voting_district, affiliation, id_type, id_number, password) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            voterId, ELECTION_ID, name, firstName, middleName, lastName, dob, gender,
            street, barangay, city, province, zip, mobile, email,
            voterIdNumber, votingDistrict, affiliation, idType, idNumber, password);
    }    public void removeVoter(String id) {
        db.update("DELETE FROM voters WHERE voter_id = ? AND election_id = ?", id, ELECTION_ID);
    }

    // ── Auth ──────────────────────────────────────────────────────────────────
    public Optional<Voter> authenticateVoter(String voterId, String password) {
        List<Voter> list = db.query(
            "SELECT voter_id, name, has_voted FROM voters " +
            "WHERE voter_id = ? AND password = ? AND election_id = ?",
            voterMapper(), voterId, password, ELECTION_ID);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    // ── Voting ────────────────────────────────────────────────────────────────
    @Transactional
    public String castVote(String voterId, String candidateId) {
        Election e = getElection();
        if (!e.isOpen()) return "Election is currently CLOSED.";

        Optional<Voter> v = findVoter(voterId);
        if (v.isEmpty())          return "Voter not found.";
        if (v.get().isHasVoted()) return "You have already voted.";

        int updated = db.update(
            "UPDATE candidates SET vote_count = vote_count + 1 " +
            "WHERE candidate_id = ? AND election_id = ?", candidateId, ELECTION_ID);
        if (updated == 0) return "Invalid candidate.";

        db.update("UPDATE voters SET has_voted = 1 WHERE voter_id = ? AND election_id = ?",
            voterId, ELECTION_ID);
        db.update("INSERT INTO votes (voter_id, candidate_id, election_id) VALUES (?,?,?)",
            voterId, candidateId, ELECTION_ID);
        return "ok";
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    public int getTotalVotes() {
        Integer n = db.queryForObject(
            "SELECT COUNT(*) FROM votes WHERE election_id = ?", Integer.class, ELECTION_ID);
        return n == null ? 0 : n;
    }

    // ── Security questions ────────────────────────────────────────────────────
    public void saveSecurityQuestion(String voterId, String question, String answer) {
        db.update("INSERT INTO security_questions (voter_id, election_id, question, answer) " +
                  "VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE question=VALUES(question), answer=VALUES(answer)",
            voterId, ELECTION_ID, question, answer.toLowerCase());
    }

    public Optional<String> getSecurityQuestion(String voterId) {
        List<String> list = db.query(
            "SELECT question FROM security_questions WHERE voter_id = ? AND election_id = ?",
            (rs, i) -> rs.getString("question"), voterId, ELECTION_ID);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public boolean verifySecurityAnswer(String voterId, String answer) {
        List<String> list = db.query(
            "SELECT answer FROM security_questions WHERE voter_id = ? AND election_id = ?",
            (rs, i) -> rs.getString("answer"), voterId, ELECTION_ID);
        return !list.isEmpty() && list.get(0).equalsIgnoreCase(answer);
    }

    public void updatePassword(String voterId, String newPassword) {
        db.update("UPDATE voters SET password = ? WHERE voter_id = ? AND election_id = ?",
            newPassword, voterId, ELECTION_ID);
    }

    // ── Row mappers ───────────────────────────────────────────────────────────
    private RowMapper<Election> electionMapper() {
        return (rs, i) -> new Election(rs.getInt("id"), rs.getString("title"), rs.getBoolean("is_open"));
    }

    private RowMapper<Candidate> candidateMapper() {
        return (rs, i) -> new Candidate(
            rs.getString("candidate_id"), rs.getString("name"),
            rs.getString("party"), rs.getInt("vote_count"));
    }

    private RowMapper<Voter> voterMapper() {
        return (rs, i) -> {
            Voter v = new Voter(rs.getString("voter_id"), rs.getString("name"), rs.getBoolean("has_voted"));
            try { v.setFirstName(rs.getString("first_name")); }      catch (Exception ignored) {}
            try { v.setMiddleName(rs.getString("middle_name")); }    catch (Exception ignored) {}
            try { v.setLastName(rs.getString("last_name")); }        catch (Exception ignored) {}
            try { v.setDateOfBirth(rs.getString("date_of_birth")); } catch (Exception ignored) {}
            try { v.setGender(rs.getString("gender")); }             catch (Exception ignored) {}
            try { v.setStreet(rs.getString("street")); }             catch (Exception ignored) {}
            try { v.setBarangay(rs.getString("barangay")); }         catch (Exception ignored) {}
            try { v.setCity(rs.getString("city")); }                 catch (Exception ignored) {}
            try { v.setProvince(rs.getString("province")); }         catch (Exception ignored) {}
            try { v.setZipCode(rs.getString("zip_code")); }          catch (Exception ignored) {}
            try { v.setMobileNumber(rs.getString("mobile_number")); }catch (Exception ignored) {}
            try { v.setEmail(rs.getString("email")); }               catch (Exception ignored) {}
            try { v.setVoterIdNumber(rs.getString("voter_id_number")); }  catch (Exception ignored) {}
            try { v.setVotingDistrict(rs.getString("voting_district")); } catch (Exception ignored) {}
            try { v.setAffiliation(rs.getString("affiliation")); }   catch (Exception ignored) {}
            try { v.setIdType(rs.getString("id_type")); }            catch (Exception ignored) {}
            try { v.setIdNumber(rs.getString("id_number")); }        catch (Exception ignored) {}
            return v;
        };
    }
}
