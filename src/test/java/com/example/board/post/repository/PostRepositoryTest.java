package com.example.board.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.board.global.config.JpaAuditingConfig;
import com.example.board.post.domain.Post;
import com.example.board.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class PostRepositoryTest {

	@Autowired
	private PostRepository postRepository;

	@Test
	@DisplayName("게시글을 저장하면 id와 생성/수정 시각이 채워진다")
	void save() {
		Post saved = postRepository.saveAndFlush(Post.create("제목", "본문", "작성자"));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("게시글 목록을 생성일 내림차순으로 페이징 조회한다")
	void findAllWithPaging() {
		for (int i = 1; i <= 3; i++) {
			postRepository.saveAndFlush(Post.create("제목" + i, "본문", "작성자"));
		}

		Page<Post> page = postRepository.findAll(
				PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt", "id")));

		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getContent()).extracting(Post::getTitle).containsExactly("제목3", "제목2");
	}
}
