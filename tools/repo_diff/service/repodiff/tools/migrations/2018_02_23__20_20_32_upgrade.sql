CREATE TABLE denormalized_view_changes_over_time (
  datastudio_datetime CHAR(10) NOT NULL,
  modified_projects INT NOT NULL,
  line_changes INT NOT NULL,
  files_changed INT NOT NULL,
  PRIMARY KEY(datastudio_datetime)
);
