CREATE TABLE denormalized_view_changes_over_time (
  upstream_target_id SMALLINT NOT NULL,
  downstream_target_id SMALLINT NOT NULL,
  datastudio_datetime CHAR(10) NOT NULL,
  modified_projects INT NOT NULL,
  line_changes INT NOT NULL,
  files_changed INT NOT NULL,
  upstream_url VARCHAR(255) NOT NULL,
  upstream_branch VARCHAR(255) NOT NULL,
  downstream_url VARCHAR(255) NOT NULL,
  downstream_branch VARCHAR(255) NOT NULL,
  PRIMARY KEY(upstream_target_id, downstream_target_id, datastudio_datetime),
  INDEX idx_url_branch (upstream_url, upstream_branch, downstream_url, downstream_branch)
);
