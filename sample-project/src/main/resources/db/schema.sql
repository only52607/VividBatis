DROP TABLE IF EXISTS blog;
DROP TABLE IF EXISTS author;

CREATE TABLE author (
    id INT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    bio VARCHAR(255)
);

CREATE TABLE blog (
    id INT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author_id INT NOT NULL,
    state VARCHAR(20) NOT NULL,
    created_on TIMESTAMP NOT NULL,
    FOREIGN KEY (author_id) REFERENCES author(id)
);
