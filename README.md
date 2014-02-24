# About
I recently came across [scala-oauth-provider](https://github.com/nulab/scala-oauth2-provider) which is a nice OAuth 2.0 provider implemented in scala .

There wasn't an example database structure to save the tokens, codes etc, so I tried to come up with one.
I also needed to implement DataHandler.scala class to make it work with your own database tables.  

I've used Playframework 2.2.1, PostgreSQL, [play-slick](https://github.com/freekh/play-slick), and [scala-oauth-provider](https://github.com/nulab/scala-oauth2-provider)

There isn't any code for the authorization_code generation part in [scala-oauth-provider](https://github.com/nulab/scala-oauth2-provider).  Therefore, this only deals with the returning access_token.

I'm a bit new to Scala, OAuth and Playframework so there might be problems :)  Please feel free to fix them.

# Database Schema
I've used postgresql with this example.  Here is the database schema I've come up with:
```
CREATE TABLE "user"
(
  id serial NOT NULL,
  username character varying(100),
  email character varying(100),
  password character varying(100),
  CONSTRAINT pk_user PRIMARY KEY (id),
  CONSTRAINT ix_user_email_unique UNIQUE (email),
  CONSTRAINT ix_user_username_unique UNIQUE (username)
)


CREATE TABLE client
(
  id character varying(80) NOT NULL,
  secret character varying(80),
  redirect_uri character varying(2000),
  scope character varying(2000),
  CONSTRAINT pk_client PRIMARY KEY (id)
)

CREATE TABLE auth_code
(
  authorization_code character varying(40) NOT NULL,
  user_id integer NOT NULL,
  redirect_uri character varying(2000),
  created_at timestamp with time zone NOT NULL,
  scope character varying(1000),
  client_id character varying(80) NOT NULL,
  expires_in integer NOT NULL,
  CONSTRAINT pk_auth_code PRIMARY KEY (authorization_code),
  CONSTRAINT fk_auth_code_client_id FOREIGN KEY (client_id)
      REFERENCES client (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_auth_code_user_id_user_id FOREIGN KEY (user_id)
      REFERENCES "user" (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)

CREATE TABLE access_token
(
  access_token character varying(60) NOT NULL,
  refresh_token character varying(60),
  user_id integer NOT NULL,
  client_id character varying(80) NOT NULL,
  scope character varying(2000),
  expires_in integer NOT NULL,
  created_at timestamp with time zone NOT NULL,
  CONSTRAINT pk_access_token PRIMARY KEY (access_token),
  CONSTRAINT fk_access_token_client_id FOREIGN KEY (client_id)
      REFERENCES client (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_access_token_user_id FOREIGN KEY (user_id)
      REFERENCES "user" (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)

CREATE TABLE client_grant_type
(
  grant_type_id integer NOT NULL,
  client_id character varying(80) NOT NULL,
  CONSTRAINT pk_client_grant_type PRIMARY KEY (grant_type_id, client_id),
  CONSTRAINT fk_client_grant_type_client_id FOREIGN KEY (client_id)
      REFERENCES client (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_client_grant_type_grant_type_id FOREIGN KEY (grant_type_id)
      REFERENCES grant_type (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)


CREATE TABLE grant_type
(
  id integer NOT NULL,
  grant_type character varying NOT NULL,
  CONSTRAINT pk_grant_type PRIMARY KEY (id)
)


-- insert grant_types in grant_type configuration table
INSERT INTO grant_type (id, grant_type) VALUES (1, 'authorization_code')
INSERT INTO grant_type (id, grant_type) VALUES (2, 'client_credentials')
INSERT INTO grant_type (id, grant_type) VALUES (3, 'password')
INSERT INTO grant_type (id, grant_type) VALUES (4, 'refresh_token')


-- create sample user
INSERT INTO "user" (username, email, password) VALUES ('test_user', 'test_email', 'test_password')

-- create sample client
INSERT INTO client (id, secret, redirect_uri, scope) VALUES ('test_client_id', 'test_client_secret', 'http://xxx', 'read write update')

-- associate some grant_types to client
INSERT INTO client_grant_type (1, 'test_client_id')
INSERT INTO client_grant_type (2, 'test_client_id')
INSERT INTO client_grant_type (3, 'test_client_id')

```

# Run

 - Create the tables using above queries
 - Insert some sample data into user, client, grant_type, and client_grant_type tables
 - Change database configuration in conf/application.conf file.
```
db.default.driver=org.postgresql.Driver
db.default.url="jdbc:postgresql://localhost:5432/oauth"
db.default.user=
db.default.password=""
```
 - Run play application:
```
cd path-to-scala-oauth-provider-example
play run
```

----------

You should be able to make POST request like below and get the token result in json

Example OAuth access_token request using Password grant type:
http://localhost:9000/oauth2/access_token?grant_type=password&client_id=test_client_id&client_secret=test_client_secret&username=test_user&password=test_password

Result:
```json
{
    "token_type": "Bearer",
    "access_token": "MDEwNTBkNDgtNDhkNC00YmNhLWJiMjktMzVhMTJkMjMwNDBk",
    "expires_in": 3600,
    "refresh_token": "NzVmYjQ4ZDMtMjY3NS00NDA4LTkyZTgtNmNjOTNlNjRhNDZl"
}
```


# Notes

 - Authorization Code generation is not included.
 - client_credentials grant type is not implemented yet.
 - Old Access tokens are deleted from the table.  Instead you may want to keep them by adding a column to keep track of its status.