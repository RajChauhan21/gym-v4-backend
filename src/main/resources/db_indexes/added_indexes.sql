--Added indexes till now--
CREATE INDEX idx_member_expiry_status
ON member (expiry, is_active);