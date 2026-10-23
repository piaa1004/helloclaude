package com.example.board.post.domain;

import com.example.board.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	// 회원 기능 도입 전까지는 작성자 이름을 문자열로 저장한다.
	@Column(nullable = false, length = 50)
	private String author;

	private Post(String title, String content, String author) {
		this.title = title;
		this.content = content;
		this.author = author;
	}

	public static Post create(String title, String content, String author) {
		return new Post(title, content, author);
	}

	public void update(String title, String content) {
		this.title = title;
		this.content = content;
	}
}
