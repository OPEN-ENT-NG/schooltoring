CREATE TABLE schooltoring.conversation (
  id      bigserial                   NOT NULL,
  created timestamp without time zone NOT NULL DEFAULT now(),
  CONSTRAINT conversation_pkey PRIMARY KEY (id)
);

CREATE TABLE schooltoring.conversation_users (
  id              character varying(36) NOT NULL,
  active          boolean               NOT NULL DEFAULT true,
  conversation_id bigint                NOT NULL,
  CONSTRAINT conversation_users_pkey PRIMARY KEY (id, conversation_id),
  CONSTRAINT fk_conversation_id FOREIGN KEY (conversation_id)
  REFERENCES schooltoring.conversation (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_student_id FOREIGN KEY (id)
  REFERENCES schooltoring.student (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION
);

ALTER TABLE schooltoring.message
  DROP CONSTRAINT fk_request_id;

ALTER TABLE schooltoring.message
  DROP COLUMN request_id RESTRICT;

ALTER TABLE schooltoring.message
  ADD COLUMN conversation_id bigint;

ALTER TABLE schooltoring.message
  ADD CONSTRAINT fk_conversation_id FOREIGN KEY (conversation_id) REFERENCES schooltoring.conversation (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION