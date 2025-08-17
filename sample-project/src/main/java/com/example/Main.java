package com.example;

import com.example.mappers.BlogMapper;
import com.example.model.Blog;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        // Load configuration
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        // Setup database schema and data
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Connection conn = session.getConnection();
            ScriptRunner runner = new ScriptRunner(conn);
            runner.runScript(Resources.getResourceAsReader("db/schema.sql"));
            runner.runScript(Resources.getResourceAsReader("db/data.sql"));
            System.out.println("Database initialized successfully.");
        }

        // Run demonstrations
        try (SqlSession session = sqlSessionFactory.openSession()) {
            BlogMapper mapper = session.getMapper(BlogMapper.class);

            System.out.println("\n--- 1. Test: <if> ---");
            System.out.println("Finding active blogs with title like '%Post%':");
            List<Blog> blogs1 = mapper.findActiveBlogWithTitleLike("%Post%");
            blogs1.forEach(System.out::println);

            System.out.println("\n--- 2. Test: <choose>, <when>, <otherwise> ---");
            System.out.println("Finding INACTIVE blogs:");
            List<Blog> blogs2 = mapper.findBlogByState("INACTIVE");
            blogs2.forEach(System.out::println);

            System.out.println("\n--- 3. Test: <where> ---");
            System.out.println("Finding blogs by author 101 and title like '%MyBatis%':");
            List<Blog> blogs3 = mapper.findBlogByAuthorAndTitle(101, "%MyBatis%");
            blogs3.forEach(System.out::println);

            System.out.println("\n--- 4. Test: <set> ---");
            System.out.println("Updating blog with id 1:");
            Blog blogToUpdate = mapper.selectBlog(1);
            blogToUpdate.setTitle("Updated First Post");
            blogToUpdate.setState("INACTIVE");
            int updateCount = mapper.updateBlog(blogToUpdate);
            System.out.println("Updated " + updateCount + " record(s). Fetching updated record:");
            System.out.println(mapper.selectBlog(1));
            session.commit(); // Commit the transaction for update

            System.out.println("\n--- 5. Test: <foreach> for IN clause ---");
            System.out.println("Selecting blogs with IDs 1, 3, 5:");
            List<Blog> blogs4 = mapper.selectBlogIn(Arrays.asList(1, 3, 5));
            blogs4.forEach(System.out::println);

            System.out.println("\n--- 6. Test: <foreach> for batch insert ---");
            System.out.println("Inserting 2 new blogs:");
            List<Blog> newBlogs = new ArrayList<>();
            Blog b1 = new Blog();
            b1.setId(10);
            b1.setTitle("Batch Insert 1");
            b1.setContent("Content for batch 1");
            b1.setAuthorId(103);
            b1.setState("DRAFT");
            b1.setCreatedOn(new Date());
            Blog b2 = new Blog();
            b2.setId(11);
            b2.setTitle("Batch Insert 2");
            b2.setContent("Content for batch 2");
            b2.setAuthorId(103);
            b2.setState("ACTIVE");
            b2.setCreatedOn(new Date());
            newBlogs.add(b1);
            newBlogs.add(b2);
            int insertCount = mapper.insertBlogs(newBlogs);
            System.out.println("Inserted " + insertCount + " new record(s). Fetching new records:");
            System.out.println(mapper.selectBlog(10));
            System.out.println(mapper.selectBlog(11));
            session.commit();

            System.out.println("\n--- 7. Test: <trim> ---");
            System.out.println("Updating blog with id 2 selectively (only title):");
            Blog blogToUpdateSelective = new Blog();
            blogToUpdateSelective.setId(2);
            blogToUpdateSelective.setTitle("MyBatis Intro (Trimmed)");
            mapper.updateBlogSelective(blogToUpdateSelective);
            System.out.println(mapper.selectBlog(2));
            session.commit();

            System.out.println("\n--- 8. Test: <bind> ---");
            System.out.println("Finding blogs with title containing 'Java':");
            List<Blog> blogs5 = mapper.findBlogWithTitleLikePattern("Java");
            blogs5.forEach(System.out::println);

            System.out.println("\n--- 9. Test: <sql> and <include> ---");
            System.out.println("Selecting blog details for id 5:");
            Blog blogDetails = mapper.selectBlogDetails(5);
            System.out.println(blogDetails);
            if (blogDetails != null && blogDetails.getAuthor() != null) {
                System.out.println("Author details: " + blogDetails.getAuthor());
            }

            System.out.println("\n--- All tests completed. ---");
        }
    }
}
