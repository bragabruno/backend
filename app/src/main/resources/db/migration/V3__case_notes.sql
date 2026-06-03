-- V3: Add case_notes table for analyst investigation notes on fraud cases

CREATE TABLE case_notes (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES fraud_cases(id),
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_case_notes_case_id ON case_notes(case_id);
CREATE INDEX idx_case_notes_created_at ON case_notes(created_at);
