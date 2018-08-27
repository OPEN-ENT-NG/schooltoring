CREATE TABLE schooltoring.favorite (
  student_id character varying(36) NOT NULL,
  owner      character varying(36) NOT NULL,
  CONSTRAINT favorite_pkey PRIMARY KEY (student_id, owner),
  CONSTRAINT fk_student_id FOREIGN KEY (student_id) REFERENCES schooltoring.student (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_student_owner_id FOREIGN KEY (owner) REFERENCES schooltoring.student (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);