CREATE TABLE denormalized_view_top_tech_area (
  upstream_target_id SMALLINT NOT NULL,
  downstream_target_id SMALLINT NOT NULL,
  surrogate_id INT NOT NULL,
  tech_area VARCHAR(255) NOT NULL,
  commits INT NOT NULL,
  line_changes INT NOT NULL,
  upstream_url VARCHAR(255) NOT NULL,
  upstream_branch VARCHAR(255) NOT NULL,
  downstream_url VARCHAR(255) NOT NULL,
  downstream_branch VARCHAR(255) NOT NULL,
  PRIMARY KEY(upstream_target_id, downstream_target_id, surrogate_id),
  INDEX idx_url_branch_commits (upstream_url, upstream_branch, downstream_url, downstream_branch, commits),
  INDEX idx_url_branch_lines (upstream_url, upstream_branch, downstream_url, downstream_branch, line_changes)
);
