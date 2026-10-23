package com.example.board.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.board.comment.domain.Comment;
import com.example.board.global.config.JpaAuditingConfig;
import com.example.board.post.domain.Post;
import com.example.board.post.repository.PostRepository;
import com.example.board.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class CommentRepositoryTest {

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private TestEntityManager em;

	private Post post;
	private Post otherPost;

	@BeforeEach
	void setUp() {
		post = postRepository.save(Post.create("제목", "본문", "작성자"));
		otherPost = postRepository.save(Post.create("다른 제목", "본문", "작성자"));
	}

	@Test
	@DisplayName("게시글별 댓글을 작성순으로 페이징 조회한다")
	void findByPostId() {
		commentRepository.saveAndFlush(Comment.create(post, "댓글1", "작성자"));
		commentRepository.saveAndFlush(Comment.create(post, "댓글2", "작성자"));
		commentRepository.saveAndFlush(Comment.create(otherPost, "다른 글 댓글", "작성자"));

		Page<Comment> page = commentRepository.findByPostId(
				post.getId(), PageRequest.of(0, 10, Sort.by("createdAt", "id")));

		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent()).extracting(Comment::getContent).containsExactly("댓글1", "댓글2");
	}

	@Test
	@DisplayName("다른 게시글의 댓글은 id가 맞아도 조회되지 않는다")
	void findByIdAndPostId() {
		Comment comment = commentRepository.saveAndFlush(Comment.create(post, "댓글", "작성자"));

		assertThat(commentRepository.findByIdAndPostId(comment.getId(), post.getId())).isPresent();
		assertThat(commentRepository.findByIdAndPostId(comment.getId(), otherPost.getId())).isEmpty();
	}

	@Test
	@DisplayName("게시글을 삭제하면 댓글도 함께 삭제된다")
	void cascadeDeleteWithPost() {
		commentRepository.saveAndFlush(Comment.create(post, "댓글", "작성자"));
		em.clear();

		postRepository.deleteById(post.getId());
		em.flush();
		em.clear();

		assertThat(commentRepository.count()).isZero();
	}
}
