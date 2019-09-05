ALTER TABLE project_differential DROP COLUMN project_type;
ALTER TABLE project_commit DROP COLUMN project_type;
ALTER TABLE denormalized_view_recent_commit DROP COLUMN project_type;
ALTER TABLE denormalized_view_recent_project DROP COLUMN project_type;
