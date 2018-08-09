CREATE TABLE schooltoring.request (
  id         bigserial                   NOT NULL,
  student_id character varying(36)       NOT NULL,
  owner      character varying(36)       NOT NULL,
  state      character varying           NOT NULL,
  status     character varying,
  created    timestamp without time zone NOT NULL DEFAULT now(),
  CONSTRAINT request_pk PRIMARY KEY (id),
  CONSTRAINT fk_owner_id FOREIGN KEY (owner)
  REFERENCES schooltoring.student (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_student_id FOREIGN KEY (student_id)
  REFERENCES schooltoring.student (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT students_state_unicity UNIQUE (student_id, owner, state),
  CONSTRAINT state_values CHECK (state :: text = ANY
                                 (ARRAY ['STRENGTH' :: character varying :: text, 'WEAKNESS' :: character varying :: text])),
  CONSTRAINT status_values CHECK (status :: text = ANY
                                  (ARRAY ['WAITING' :: character varying :: text, 'ACCEPTED' :: character varying :: text, 'CANCELED' :: character varying :: text]))
);

CREATE TABLE schooltoring.message
(
  id         bigserial                   NOT NULL,
  owner      character varying(36),
  request_id bigint,
  date       timestamp without time zone NOT NULL DEFAULT now(),
  text       text,
  CONSTRAINT message_pk PRIMARY KEY (id),
  CONSTRAINT fk_request_id FOREIGN KEY (request_id)
  REFERENCES schooltoring.request (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_student_id FOREIGN KEY (owner)
  REFERENCES schooltoring.student (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION
);