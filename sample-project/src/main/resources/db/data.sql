INSERT INTO author (id, username, password, email, bio) VALUES
(101, 'john.doe', 'password123', 'john.doe@example.com', 'A passionate blogger about technology.'),
(102, 'jane.smith', 'securepass', 'jane.smith@example.com', 'Exploring the world of culinary arts.'),
(103, 'peter.jones', 'pass123', 'peter.jones@example.com', NULL);

INSERT INTO blog (id, title, content, author_id, state, created_on) VALUES
(1, 'First Post', 'This is the first blog post.', 101, 'ACTIVE', '2024-01-15 10:30:00'),
(2, 'MyBatis Introduction', 'A deep dive into MyBatis dynamic SQL.', 101, 'ACTIVE', '2024-02-20 14:00:00'),
(3, 'Cooking Adventures', 'Exploring delicious recipes.', 102, 'INACTIVE', '2024-03-10 18:45:00'),
(4, 'Advanced Java', 'Topics for experienced Java developers.', 101, 'DRAFT', '2024-05-05 09:00:00'),
(5, 'Travel Diary', 'My trip to the mountains.', 102, 'ACTIVE', '2024-06-22 20:00:00');
