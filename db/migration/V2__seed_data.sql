-- Sample data for the whole system, inserted parent-first so every foreign key
-- resolves. The fixed UUIDs are kept from the original per-service seed files:
-- ...-0001-... are students, ...-0002-... faculty, ...-0003-... classes and
-- ...-0004-... login accounts.

INSERT INTO faculty (id, first_name, last_name, email, department, title, hire_date) VALUES
('00000000-0000-0000-0002-000000000001', 'Susan',  'Miller',    'susan.miller@example.edu',    'Mathematics',       'Professor',            '2010-08-15'),
('00000000-0000-0000-0002-000000000002', 'James',  'Anderson',  'james.anderson@example.edu',  'Computer Science',  'Professor',            '2008-01-10'),
('00000000-0000-0000-0002-000000000003', 'Maria',  'Rodriguez', 'maria.rodriguez@example.edu', 'Physics',           'Associate Professor',  '2015-09-01'),
('00000000-0000-0000-0002-000000000004', 'Robert', 'Taylor',    'robert.taylor@example.edu',   'History',           'Professor',            '2005-07-20'),
('00000000-0000-0000-0002-000000000005', 'Linda',  'Martinez',  'linda.martinez@example.edu',  'English',           'Associate Professor',  '2012-03-12'),
('00000000-0000-0000-0002-000000000006', 'Kevin',  'White',     'kevin.white@example.edu',     'Chemistry',         'Assistant Professor',  '2019-11-04');

INSERT INTO student (id, first_name, last_name, email, date_of_birth, enrollment_status) VALUES
('00000000-0000-0000-0001-000000000001', 'Alice',    'Johnson', 'alice.johnson@example.edu',   '2005-03-14', 'ACTIVE'),
('00000000-0000-0000-0001-000000000002', 'Brian',    'Smith',   'brian.smith@example.edu',     '2004-11-02', 'ACTIVE'),
('00000000-0000-0000-0001-000000000003', 'Carla',    'Diaz',    'carla.diaz@example.edu',      '2005-07-21', 'ACTIVE'),
('00000000-0000-0000-0001-000000000004', 'David',    'Kim',     'david.kim@example.edu',       '2004-01-09', 'ACTIVE'),
('00000000-0000-0000-0001-000000000005', 'Emma',     'Wilson',  'emma.wilson@example.edu',     '2005-09-30', 'ACTIVE'),
('00000000-0000-0000-0001-000000000006', 'Frank',    'Lee',     'frank.lee@example.edu',       '2003-12-18', 'ACTIVE'),
('00000000-0000-0000-0001-000000000007', 'Grace',    'Chen',    'grace.chen@example.edu',      '2005-05-05', 'ACTIVE'),
('00000000-0000-0000-0001-000000000008', 'Henry',    'Patel',   'henry.patel@example.edu',     '2004-08-27', 'ACTIVE'),
('00000000-0000-0000-0001-000000000009', 'Isabella', 'Garcia',  'isabella.garcia@example.edu', '2005-02-14', 'ACTIVE'),
('00000000-0000-0000-0001-00000000000a', 'Jacob',    'Brown',   'jacob.brown@example.edu',     '2004-06-11', 'GRADUATED');

-- faculty_id references the faculty rows above; faculty_name is still stored
-- alongside it as a denormalised display value so the class list needs no join.
INSERT INTO course_class (id, course_code, title, description, semester, faculty_id, faculty_name, capacity, seats_taken, room, day_of_week, start_time, end_time) VALUES
('00000000-0000-0000-0003-000000000001', 'CS101',   'Intro to Computer Science', 'Fundamentals of programming and computational thinking.', 'FALL2026',   '00000000-0000-0000-0002-000000000002', 'James Anderson',  30, 2, 'Eng Hall 101',  'MWF', '09:00', '09:50'),
('00000000-0000-0000-0003-000000000002', 'CS201',   'Data Structures',           'Core data structures and algorithm analysis.',            'FALL2026',   '00000000-0000-0000-0002-000000000002', 'James Anderson',  25, 0, 'Eng Hall 205',  'TR',  '11:00', '12:15'),
('00000000-0000-0000-0003-000000000003', 'MATH101', 'Calculus I',                'Limits, derivatives, and integrals.',                     'FALL2026',   '00000000-0000-0000-0002-000000000001', 'Susan Miller',    35, 1, 'Sci Hall 210',  'MWF', '10:00', '10:50'),
('00000000-0000-0000-0003-000000000004', 'PHYS101', 'Physics I',                 'Classical mechanics.',                                    'FALL2026',   '00000000-0000-0000-0002-000000000003', 'Maria Rodriguez', 28, 1, 'Sci Hall 118',  'MWF', '13:00', '13:50'),
('00000000-0000-0000-0003-000000000005', 'HIST101', 'World History',             'Survey of world history from antiquity to present.',      'FALL2026',   '00000000-0000-0000-0002-000000000004', 'Robert Taylor',   40, 1, 'Humanities 100','TR',  '09:30', '10:45'),
('00000000-0000-0000-0003-000000000006', 'ENG101',  'English Composition',       'Academic writing and rhetoric.',                          'FALL2026',   '00000000-0000-0000-0002-000000000005', 'Linda Martinez',  32, 0, 'Humanities 220','MWF', '11:00', '11:50'),
('00000000-0000-0000-0003-000000000007', 'CS101',   'Intro to Computer Science', 'Fundamentals of programming and computational thinking.', 'SPRING2026', '00000000-0000-0000-0002-000000000002', 'James Anderson',  30, 1, 'Eng Hall 101',  'MWF', '09:00', '09:50'),
('00000000-0000-0000-0003-000000000008', 'MATH101', 'Calculus I',                'Limits, derivatives, and integrals.',                     'SPRING2026', '00000000-0000-0000-0002-000000000001', 'Susan Miller',    35, 2, 'Sci Hall 210',  'MWF', '10:00', '10:50'),
('00000000-0000-0000-0003-000000000009', 'CHEM101', 'General Chemistry',         'Atomic structure, bonding, and reactions.',               'SPRING2026', '00000000-0000-0000-0002-000000000006', 'Kevin White',     24, 1, 'Sci Hall 305',  'TR',  '14:00', '15:15'),
('00000000-0000-0000-0003-00000000000a', 'ENG101',  'English Composition',       'Academic writing and rhetoric.',                          'SPRING2026', '00000000-0000-0000-0002-000000000005', 'Linda Martinez',  32, 1, 'Humanities 220','MWF', '11:00', '11:50'),
('00000000-0000-0000-0003-00000000000b', 'HIST101', 'World History',             'Survey of world history from antiquity to present.',      'SPRING2026', '00000000-0000-0000-0002-000000000004', 'Robert Taylor',   40, 0, 'Humanities 100','TR',  '09:30', '10:45'),
('00000000-0000-0000-0003-00000000000c', 'PHYS101', 'Physics I',                 'Classical mechanics.',                                    'SPRING2026', '00000000-0000-0000-0002-000000000003', 'Maria Rodriguez', 28, 0, 'Sci Hall 118',  'MWF', '13:00', '13:50');

-- Current semester enrollments. seats_taken above was seeded to match these.
INSERT INTO schedule_entry (student_id, class_id, course_code, course_title, semester, status) VALUES
('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0003-000000000001', 'CS101',   'Intro to Computer Science', 'FALL2026', 'ENROLLED'),
('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0003-000000000003', 'MATH101', 'Calculus I',                'FALL2026', 'ENROLLED'),
('00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0003-000000000001', 'CS101',   'Intro to Computer Science', 'FALL2026', 'ENROLLED'),
('00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0003-000000000004', 'PHYS101', 'Physics I',                 'FALL2026', 'ENROLLED'),
('00000000-0000-0000-0001-000000000004', '00000000-0000-0000-0003-000000000005', 'HIST101', 'World History',             'FALL2026', 'ENROLLED');

-- Past semester (completed) enrollments.
INSERT INTO schedule_entry (student_id, class_id, course_code, course_title, semester, status) VALUES
('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0003-000000000007', 'CS101',    'Intro to Computer Science', 'SPRING2026', 'COMPLETED'),
('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0003-000000000008', 'MATH101',  'Calculus I',                'SPRING2026', 'COMPLETED'),
('00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0003-000000000008', 'MATH101',  'Calculus I',                'SPRING2026', 'COMPLETED'),
('00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0003-000000000009', 'CHEM101',  'General Chemistry',         'SPRING2026', 'COMPLETED'),
('00000000-0000-0000-0001-000000000005', '00000000-0000-0000-0003-00000000000a', 'ENG101',   'English Composition',       'SPRING2026', 'COMPLETED');

-- All demo accounts share the password `password123` for simplicity (sample/demo system only).
-- student_id / faculty_id reference the rows seeded above; admin references neither.
INSERT INTO app_user (id, username, password_hash, role, student_id, faculty_id) VALUES
('00000000-0000-0000-0004-000000000001', 'admin',           '$2a$10$LkX.psHDj.mo3RTc6/Mus.qK5SSXJ3xn16L98EeFMhifc5Up/9ziK', 'ADMIN',   NULL, NULL),
('00000000-0000-0000-0004-000000000002', 'susan.miller',    '$2a$10$LkX.psHDj.mo3RTc6/Mus.qK5SSXJ3xn16L98EeFMhifc5Up/9ziK', 'FACULTY', NULL, '00000000-0000-0000-0002-000000000001'),
('00000000-0000-0000-0004-000000000003', 'james.anderson',  '$2a$10$LkX.psHDj.mo3RTc6/Mus.qK5SSXJ3xn16L98EeFMhifc5Up/9ziK', 'FACULTY', NULL, '00000000-0000-0000-0002-000000000002'),
('00000000-0000-0000-0004-000000000004', 'alice.johnson',   '$2a$10$LkX.psHDj.mo3RTc6/Mus.qK5SSXJ3xn16L98EeFMhifc5Up/9ziK', 'STUDENT', '00000000-0000-0000-0001-000000000001', NULL),
('00000000-0000-0000-0004-000000000005', 'brian.smith',     '$2a$10$LkX.psHDj.mo3RTc6/Mus.qK5SSXJ3xn16L98EeFMhifc5Up/9ziK', 'STUDENT', '00000000-0000-0000-0001-000000000002', NULL),
('00000000-0000-0000-0004-000000000006', 'carla.diaz',      '$2a$10$LkX.psHDj.mo3RTc6/Mus.qK5SSXJ3xn16L98EeFMhifc5Up/9ziK', 'STUDENT', '00000000-0000-0000-0001-000000000003', NULL),
('00000000-0000-0000-0004-000000000007', 'david.kim',       '$2a$10$LkX.psHDj.mo3RTc6/Mus.qK5SSXJ3xn16L98EeFMhifc5Up/9ziK', 'STUDENT', '00000000-0000-0000-0001-000000000004', NULL),
('00000000-0000-0000-0004-000000000008', 'emma.wilson',     '$2a$10$LkX.psHDj.mo3RTc6/Mus.qK5SSXJ3xn16L98EeFMhifc5Up/9ziK', 'STUDENT', '00000000-0000-0000-0001-000000000005', NULL);
