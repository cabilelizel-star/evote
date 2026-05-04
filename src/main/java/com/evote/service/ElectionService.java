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

    // â”€â”€ Election â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public Election getElection() {
        return db.queryForObject(
            "SELECT id, title, is_open, start_time, end_time FROM elections WHERE id = ?",
            electionMapper(), ELECTION_ID);
    }

    public void setElectionOpen(boolean open) {
        db.update("UPDATE elections SET is_open = ? WHERE id = ?", open, ELECTION_ID);
    }

    public void updateElectionSettings(String title, String startTime, String endTime) {
        db.update("UPDATE elections SET title = ?, start_time = ?, end_time = ? WHERE id = ?",
            title, startTime, endTime, ELECTION_ID);
    }

    // â”€â”€ Candidates â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

    // â”€â”€ Voters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

    public Optional<Voter> findVoterFull(String voterId) {
        List<Voter> list = db.query(
            "SELECT voter_id, name, has_voted, first_name, middle_name, last_name, " +
            "date_of_birth, gender, street, barangay, city, province, zip_code, " +
            "mobile_number, email, voter_id_number, voting_district, affiliation, " +
            "id_type, id_number FROM voters WHERE voter_id = ? AND election_id = ?",
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
            "voter_id_number, voting_district, affiliation, id_type, id_number, password, status) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'approved')",
            voterId, ELECTION_ID, name, firstName, middleName, lastName, dob, gender,
            street, barangay, city, province, zip, mobile, email,
            voterIdNumber, votingDistrict, affiliation, idType, idNumber, password);
    }

    public void removeVoter(String id) {
        db.update("DELETE FROM voters WHERE voter_id = ? AND election_id = ?", id, ELECTION_ID);
    }

    // â”€â”€ Auth â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public Optional<Voter> authenticateVoter(String voterId, String password,
                                              org.springframework.security.crypto.password.PasswordEncoder encoder) {
        List<Voter> list = db.query(
            "SELECT voter_id, name, has_voted, password, status, rejection_reason FROM voters " +
            "WHERE voter_id = ? AND election_id = ?",
            (rs, i) -> {
                Voter v = new Voter(rs.getString("voter_id"), rs.getString("name"), rs.getBoolean("has_voted"));
                v.setStatus(rs.getString("status"));
                v.setRejectionReason(rs.getString("rejection_reason"));
                // Store raw password hash temporarily for verification
                v.setPassword(rs.getString("password"));
                return v;
            }, voterId, ELECTION_ID);
        if (list.isEmpty()) return Optional.empty();
        Voter v = list.get(0);
        String stored = v.getPassword();
        // Support both BCrypt hashes and legacy plain text
        boolean match = (stored != null && stored.startsWith("$2")) 
            ? encoder.matches(password, stored)
            : password.equals(stored);
        return match ? Optional.of(v) : Optional.empty();
    }

    public Optional<Voter> authenticateVoter(String voterId, String password) {
        return authenticateVoter(voterId, password,
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
    }

    // â”€â”€ Voting â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Transactional
    public String castVotes(String voterId, List<String> candidateIds) {
        Election e = getElection();
        if (!e.isOpen()) return "Election is currently CLOSED.";

        Optional<Voter> v = findVoter(voterId);
        if (v.isEmpty())          return "Voter not found.";
        if (v.get().isHasVoted()) return "You have already voted.";

        for (String candidateId : candidateIds) {
            int updated = db.update(
                "UPDATE candidates SET vote_count = vote_count + 1 " +
                "WHERE candidate_id = ? AND election_id = ?", candidateId, ELECTION_ID);
            if (updated > 0) {
                db.update("INSERT INTO votes (voter_id, candidate_id, election_id) VALUES (?,?,?)",
                    voterId, candidateId, ELECTION_ID);
            }
        }

        db.update("UPDATE voters SET has_voted = 1 WHERE voter_id = ? AND election_id = ?",
            voterId, ELECTION_ID);
        return "ok";
    }

    @Transactional
    public String castVote(String voterId, String candidateId) {
        return castVotes(voterId, java.util.List.of(candidateId));
    }

    // â”€â”€ Vote receipt â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public String getVoteTimestamp(String voterId) {
        try {
            List<String> rows = db.query(
                "SELECT MAX(voted_at) FROM votes WHERE voter_id = ? AND election_id = ?",
                (rs, i) -> rs.getString(1), voterId, ELECTION_ID);
            return (rows.isEmpty() || rows.get(0) == null) ? "" : rows.get(0);
        } catch (Exception e) { return ""; }
    }

    public String generateTransactionId(String voterId) {
        // Format: EVT-YYYYMMDD-VOTERPREFIX-HASH
        String date = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String prefix = voterId.replaceAll("[^A-Z0-9]", "").toUpperCase();
        if (prefix.length() > 6) prefix = prefix.substring(0, 6);
        int hash = Math.abs((voterId + date).hashCode()) % 100000;
        return "EVT-" + date + "-" + prefix + "-" + String.format("%05d", hash);
    }

    // â”€â”€ Stats â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public int getTotalVotes() {
        Integer n = db.queryForObject(
            "SELECT COUNT(*) FROM votes WHERE election_id = ?", Integer.class, ELECTION_ID);
        return n == null ? 0 : n;
    }

    public int getTurnoutPercent() {
        try {
            // Count unique voters who have voted
            Integer voted = db.queryForObject(
                "SELECT COUNT(*) FROM voters WHERE election_id = ? AND has_voted = 1",
                Integer.class, ELECTION_ID);
            int total = getVoters().size();
            if (total == 0) return 0;
            return (int) Math.round((voted == null ? 0 : voted) * 100.0 / total);
        } catch (Exception e) { return 0; }
    }

    public void updateVoterContact(String voterId, String mobile, String email) {
        db.update("UPDATE voters SET mobile_number=?, email=? WHERE voter_id=? AND election_id=?",
            mobile, email, voterId, ELECTION_ID);
    }

    public boolean verifyVoterPassword(String voterId, String password) {
        try {
            List<String> rows = db.query(
                "SELECT password FROM voters WHERE voter_id=? AND election_id=?",
                (rs, i) -> rs.getString("password"), voterId, ELECTION_ID);
            if (rows.isEmpty() || rows.get(0) == null) return false;
            String stored = rows.get(0);
            if (stored.startsWith("$2")) {
                return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(password, stored);
            }
            return password.equals(stored);
        } catch (Exception e) { return false; }
    }

    // -- Security questions --
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
        String hashed = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(newPassword);
        db.update("UPDATE voters SET password = ? WHERE voter_id = ? AND election_id = ?", hashed, voterId, ELECTION_ID);
    }

    // â”€â”€ Voter blocking â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void setVoterBlocked(String voterId, boolean blocked) {
        try {
            db.update("UPDATE voters SET is_blocked = ? WHERE voter_id = ? AND election_id = ?",
                blocked, voterId, ELECTION_ID);
        } catch (Exception ignored) {}
    }

    // â”€â”€ Image storage â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void saveVoterImages(String voterId, byte[] idPhotoBytes, byte[] selfieBytes) {
        try {
            db.update("UPDATE voters SET id_photo = ?, selfie_photo = ? WHERE voter_id = ? AND election_id = ?",
                idPhotoBytes, selfieBytes, voterId, ELECTION_ID);
        } catch (Exception e) {
            System.err.println("saveVoterImages failed: " + e.getMessage());
        }
    }

    public byte[] getVoterIdPhoto(String voterId) {
        try {
            List<byte[]> rows = db.query(
                "SELECT id_photo FROM voters WHERE voter_id = ? AND election_id = ?",
                (rs, i) -> rs.getBytes("id_photo"), voterId, ELECTION_ID);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) { return null; }
    }

    public byte[] getVoterSelfie(String voterId) {
        try {
            List<byte[]> rows = db.query(
                "SELECT selfie_photo FROM voters WHERE voter_id = ? AND election_id = ?",
                (rs, i) -> rs.getBytes("selfie_photo"), voterId, ELECTION_ID);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) { return null; }
    }

    // â”€â”€ Approval â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void approveVoter(String voterId) {
        db.update("UPDATE voters SET status='approved' WHERE voter_id=? AND election_id=?",
            voterId, ELECTION_ID);
    }

    public void rejectVoter(String voterId, String reason) {
        db.update("UPDATE voters SET status='rejected', rejection_reason=? WHERE voter_id=? AND election_id=?",
            reason, voterId, ELECTION_ID);
    }

    public List<Voter> getPendingVoters() {
        return db.query(
            "SELECT voter_id, name, has_voted, first_name, last_name, email, mobile_number, " +
            "id_type, id_number, status, rejection_reason FROM voters " +
            "WHERE election_id=? AND (status='pending' OR status IS NULL) ORDER BY created_at DESC",
            voterMapper(), ELECTION_ID);
    }

    // â”€â”€ Notifications â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void sendNotificationToAll(String title, String message) {
        try {
            List<Voter> voters = getVoters();
            for (Voter v : voters) {
                db.update("INSERT INTO notifications (voter_id, title, message) VALUES (?,?,?)",
                    v.getVoterId(), title, message);
            }
        } catch (Exception e) {
            System.err.println("sendNotificationToAll failed: " + e.getMessage());
        }
    }

    public List<java.util.Map<String, Object>> getNotifications(String voterId) {
        try {
            return db.queryForList(
                "SELECT id, title, message, is_read, created_at FROM notifications " +
                "WHERE voter_id = ? ORDER BY created_at DESC LIMIT 20", voterId);
        } catch (Exception e) { return java.util.Collections.emptyList(); }
    }

    public int getUnreadCount(String voterId) {
        try {
            Integer n = db.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE voter_id = ? AND is_read = 0",
                Integer.class, voterId);
            return n == null ? 0 : n;
        } catch (Exception e) { return 0; }
    }

    public void markAllRead(String voterId) {
        try {
            db.update("UPDATE notifications SET is_read = 1 WHERE voter_id = ?", voterId);
        } catch (Exception e) { System.err.println("markAllRead failed: " + e.getMessage()); }
    }

    // â”€â”€ Audit log â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void logActivity(String actor, String action) {
        try {
            db.update("INSERT INTO audit_log (actor, action, logged_at) VALUES (?, ?, NOW())",
                actor, action);
        } catch (Exception ignored) {}
    }

    public List<java.util.Map<String, Object>> getAuditLogs() {
        try {
            return db.queryForList(
                "SELECT actor, action, logged_at FROM audit_log ORDER BY logged_at DESC LIMIT 50");
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    // â”€â”€ Row mappers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private RowMapper<Election> electionMapper() {
        return (rs, i) -> {
            Election e = new Election(rs.getInt("id"), rs.getString("title"), rs.getBoolean("is_open"));
            try { e.setStartTime(rs.getString("start_time")); } catch (Exception ignored) {}
            try { e.setEndTime(rs.getString("end_time")); }   catch (Exception ignored) {}
            return e;
        };
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
            try { v.setStatus(rs.getString("status")); }             catch (Exception ignored) {}
            try { v.setRejectionReason(rs.getString("rejection_reason")); } catch (Exception ignored) {}
            return v;
        };
    }
}



