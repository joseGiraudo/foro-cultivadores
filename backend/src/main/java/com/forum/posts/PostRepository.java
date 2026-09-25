package com.forum.posts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
}
