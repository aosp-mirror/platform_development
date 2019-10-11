CREATE INDEX idx_url_branch_commit_ts ON project_commit (upstream_target_id, downstream_target_id, commit_, timestamp ASC);
