ALTER TABLE project_commit ADD project_type INT NOT NULL DEFAULT 0;
ALTER TABLE project_differential ADD project_type INT NOT NULL DEFAULT 0;
ALTER TABLE denormalized_view_recent_commit ADD project_type VARCHAR(255) NOT NULL DEFAULT "Empty";
ALTER TABLE denormalized_view_recent_project ADD project_type VARCHAR(255) NOT NULL DEFAULT "Empty";
