CREATE TABLE organizations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_organizations_name UNIQUE (name)
);

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    name VARCHAR(140) NOT NULL,
    email VARCHAR(190) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'MANAGER', 'MEMBER'))
);

CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    name VARCHAR(180) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_projects_org_name UNIQUE (organization_id, name),
    CONSTRAINT fk_projects_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    assignee_id BIGINT NOT NULL,
    created_by_id BIGINT NOT NULL,
    title VARCHAR(220) NOT NULL,
    description TEXT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    due_date DATE NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users(id),
    CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'BLOCKED'))
);

CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    replaced_by_hash CHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_users_org_role ON users (organization_id, role);
CREATE INDEX idx_projects_org ON projects (organization_id);
CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_assignee ON tasks (assignee_id);
CREATE INDEX idx_tasks_due_date ON tasks (due_date);
CREATE INDEX idx_tasks_org_filters ON tasks (organization_id, status, priority, assignee_id, due_date);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
