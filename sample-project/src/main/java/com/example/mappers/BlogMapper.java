package com.example.mappers;

import com.example.model.Blog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BlogMapper {

    // Basic select
    Blog selectBlog(int id);

    // Dynamic SQL: <if>
    List<Blog> findActiveBlogWithTitleLike(String title);

    // Dynamic SQL: <choose>, <when>, <otherwise>
    List<Blog> findBlogByState(String state);

    // Dynamic SQL: <where>
    List<Blog> findBlogByAuthorAndTitle(@Param("authorId") Integer authorId, @Param("title") String title);

    // Dynamic SQL: <set>
    int updateBlog(Blog blog);

    // Dynamic SQL: <foreach> for IN clause
    List<Blog> selectBlogIn(List<Integer> ids);

    // Dynamic SQL: <foreach> for batch insert
    int insertBlogs(List<Blog> blogs);

    // Dynamic SQL: <trim> custom prefix/suffix
    int updateBlogSelective(Blog blog);

    // Dynamic SQL: <bind>
    List<Blog> findBlogWithTitleLikePattern(String pattern);

    // Reusable fragment: <sql> and <include>
    Blog selectBlogDetails(int id);
}
