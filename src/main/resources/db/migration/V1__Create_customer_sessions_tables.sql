-- Create customer_sessions table
CREATE TABLE IF NOT EXISTS customer_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(64) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ended_at TIMESTAMP WITH TIME ZONE,
    intent VARCHAR(64),
    resolution_status VARCHAR(32) DEFAULT 'unresolved',
    transcript TEXT,
    metadata JSONB,
    archived_at TIMESTAMP WITH TIME ZONE,
    is_archived BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_customer_id FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create session_turns table
CREATE TABLE IF NOT EXISTS session_turns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    turn_index INTEGER NOT NULL,
    speaker VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_session_id FOREIGN KEY (session_id) REFERENCES customer_sessions(id) ON DELETE CASCADE,
    CONSTRAINT chk_speaker CHECK (speaker IN ('customer', 'system'))
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_sessions_customer_id ON customer_sessions(customer_id);
CREATE INDEX IF NOT EXISTS idx_sessions_started_at ON customer_sessions(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_sessions_archived ON customer_sessions(is_archived) WHERE is_archived = FALSE;
CREATE INDEX IF NOT EXISTS idx_turns_session_id ON session_turns(session_id);
CREATE INDEX IF NOT EXISTS idx_turns_turn_index ON session_turns(session_id, turn_index);
