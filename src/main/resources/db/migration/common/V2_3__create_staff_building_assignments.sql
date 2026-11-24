-- ============================================================================
-- V2_3__create_staff_building_assignments.sql
-- Tạo bảng phân công nhân viên quản lý tòa nhà (staff_building_assignments)
-- Cú pháp ANSI SQL tương thích cả SQL Server và H2
-- ============================================================================

CREATE TABLE staff_building_assignments (
    assignment_id INT IDENTITY(1,1) NOT NULL,
    staff_user_id INT NOT NULL,
    building_id INT NOT NULL,
    CONSTRAINT pk_staff_building_assignments PRIMARY KEY (assignment_id),
    CONSTRAINT fk_sba_staff_user FOREIGN KEY (staff_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_sba_building FOREIGN KEY (building_id) REFERENCES buildings (building_id),
    CONSTRAINT uk_staff_building_assignment UNIQUE (staff_user_id, building_id)
);
