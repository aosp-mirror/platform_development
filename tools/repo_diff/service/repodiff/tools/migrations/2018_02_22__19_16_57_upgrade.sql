CREATE TABLE denormalized_view_recent_project (
  row_index INT NOT NULL,
  date DATE NOT NULL,
  downstream_project VARCHAR(255) NOT NULL,
  upstream_project VARCHAR(255) NOT NULL,
  status VARCHAR(255) NOT NULL,
  files_changed INT NOT NULL,
  line_insertions INT NOT NULL,
  line_deletions INT NOT NULL,
  line_changes INT NOT NULL,
  commits_not_upstreamed INT NOT NULL,
  PRIMARY KEY (row_index)
);
