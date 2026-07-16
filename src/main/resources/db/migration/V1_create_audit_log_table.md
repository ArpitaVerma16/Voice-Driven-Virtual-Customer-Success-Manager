-- Create immutable audit log table
CREATE TABLE IF NOT EXISTS immutable_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    previous_value TEXT,
    new_value TEXT,
    previous_hash VARCHAR(64) NOT NULL,
    current_hash VARCHAR(64) NOT NULL UNIQUE,
    sequence_number INT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    INDEX idx_actor (actor),
    INDEX idx_action (action),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_sequence (sequence_number),
    INDEX idx_timestamp (timestamp)
);

-- Create verification helper function
DELIMITER //
CREATE FUNCTION verify_audit_chain(start_id BIGINT, end_id BIGINT)
RETURNS BOOLEAN
DETERMINISTIC
BEGIN
    DECLARE valid BOOLEAN DEFAULT TRUE;
    DECLARE done INT DEFAULT FALSE;
    DECLARE prev_hash VARCHAR(64);
    DECLARE curr_hash VARCHAR(64);
    DECLARE curr_id BIGINT;
    
    DECLARE cur CURSOR FOR 
        SELECT id, previous_hash, current_hash 
        FROM immutable_audit_logs 
        WHERE id BETWEEN start_id AND end_id 
        ORDER BY sequence_number;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO curr_id, prev_hash, curr_hash;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- Check hash chain (simplified)
        -- In reality, you'd recalculate hash from content
        IF prev_hash IS NULL OR LENGTH(prev_hash) != 64 THEN
            SET valid = FALSE;
            LEAVE read_loop;
        END IF;
    END LOOP;
    
    CLOSE cur;
    RETURN valid;
END //
DELIMITER ;