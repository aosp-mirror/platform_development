CREATE TABLE project_commit (
  timestamp BIGINT NOT NULL,
  uuid BINARY(16) NOT NULL,
  row_index INT NOT NULL,
  commit_ CHAR(40) NOT NULL,
  downstream_project VARCHAR(255) NOT NULL,
  author VARCHAR(255) NOT NULL,
  subject TEXT NOT NULL,
  PRIMARY KEY (timestamp, uuid, row_index)
);
