
CREATE TABLE project_differential (
  upstream_target_id SMALLINT NOT NULL,
  downstream_target_id SMALLINT NOT NULL,
  timestamp BIGINT NOT NULL,
  uuid BINARY(16) NOT NULL,
  row_index INT NOT NULL,
  downstream_project VARCHAR(255) NOT NULL,
  upstream_project VARCHAR(255) NOT NULL,
  /* Ideally the status field is represented as an int; leaving as string for now for simplified DataStudio usage */
  status TINYINT NOT NULL,
  files_changed INT NOT NULL,
  line_insertions INT NOT NULL,
  line_deletions INT NOT NULL,
  line_changes INT NOT NULL,
  commits_not_upstreamed INT NOT NULL,
  PRIMARY KEY (upstream_target_id, downstream_target_id, timestamp, uuid, row_index)
);
