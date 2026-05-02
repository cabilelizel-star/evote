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
            "SELECT voter_id, name, has_voted FROM voters WHERE election_id = ? ORDER BY name",
            voterMapper(), ELECTION_ID);
    }

    public Optional<Voter> findVoter(String voterId) {
        List<Voter> list = db.query(
            "SELECT voter_id, name, has_voted FROM voters WHERE voter_id = ? AND election_id = ?",
            voterMapper(), voterId, ELECTION_ID);
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

    public void addVoterFull(String id, String name, String email, String birthday,
                              Integer age, String placeOfBirth, String gender,
                              String contact, String password) {
        db.update("INSERT IGNORE INTO voters " +
                  "(voter_id, election_id, name, email, birthday, age, place_of_birth, gender, contact_number, password) " +
                  "VALUES (?,?,?,?,?,?,?,?,?,?)",
            id, ELECTION_ID, name, email, birthday, age, placeOfBirth, gender, contact, password);
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
            try { v.setEmail(rs.getString("email")); } catch (Exception ignored) {}
            try { v.setBirthday(rs.getString("birthday")); } catch (Exception ignored) {}
            try { v.setAge(rs.getInt("age")); } catch (Exception ignored) {}
            try { v.setPlaceOfBirth(rs.getString("place_of_birth")); } catch (Exception ignored) {}
            try { v.setGender(rs.getString("gender")); } catch (Exception ignored) {}
            try { v.setContactNumber(rs.getString("contact_number")); } catch (Exception ignored) {}
            return v;
        };
    }
}
