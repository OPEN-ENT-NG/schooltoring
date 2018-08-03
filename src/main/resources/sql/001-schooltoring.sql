CREATE SCHEMA schooltoring;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE schooltoring.scripts (
  filename character varying(255) NOT NULL,
  passed timestamp without time zone NOT NULL DEFAULT now(),
  CONSTRAINT scripts_pkey PRIMARY KEY (filename)
);

CREATE TABLE schooltoring.student (
  id character varying(36) NOT NULL,
  structure_id character varying(36) NOT NULL,
  monday boolean,
  tuesday boolean,
  wednesday boolean,
  thursday boolean,
  friday boolean,
  saturday boolean,
  sunday boolean,
  CONSTRAINT student_pk PRIMARY KEY (id)
);

CREATE TABLE schooltoring.feature (
  id bigserial NOT NULL,
  student_id character varying(36),
  state character varying(255),
  subject_id character varying(36),
  CONSTRAINT feature_pk PRIMARY KEY (id),
  CONSTRAINT fk_student_id FOREIGN KEY (student_id)
      REFERENCES schooltoring.student (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT state_values CHECK (state::text = ANY (ARRAY['STRENGTH'::character varying, 'WEAKNESS'::character varying]::text[]))
);