CREATE TABLE denormalized_view_recent_commit (
  upstream_target_id SMALLINT NOT NULL,
  downstream_target_id SMALLINT NOT NULL,
  row_index INT NOT NULL,
  commit_ CHAR(40) NOT NULL,
  downstream_project VARCHAR(255) NOT NULL,
  author VARCHAR(255) NOT NULL,
  subject TEXT NOT NULL,
  upstream_url VARCHAR(255) NOT NULL,
  upstream_branch VARCHAR(255) NOT NULL,
  downstream_url VARCHAR(255) NOT NULL,
  downstream_branch VARCHAR(255) NOT NULL,
  PRIMARY KEY (upstream_target_id, downstream_target_id, row_index),
  INDEX idx_url_branch (upstream_url, upstream_branch, downstream_url, downstream_branch)
);
