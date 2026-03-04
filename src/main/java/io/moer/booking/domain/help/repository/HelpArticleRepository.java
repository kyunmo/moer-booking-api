package io.moer.booking.domain.help.repository;

import io.moer.booking.domain.help.HelpArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 도움말 Repository
 */
@Mapper
public interface HelpArticleRepository {

    /**
     * 도움말 저장
     */
    void save(HelpArticle article);

    /**
     * ID로 조회
     */
    Optional<HelpArticle> findById(Long id);

    /**
     * 조건 검색 (카테고리, 키워드, 언어)
     */
    List<HelpArticle> findByCondition(
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("lang") String lang,
            @Param("publishedOnly") boolean publishedOnly
    );

    /**
     * 조건별 개수
     */
    int countByCondition(
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("lang") String lang,
            @Param("publishedOnly") boolean publishedOnly
    );

    /**
     * 카테고리 목록 조회 (중복 제거)
     */
    List<String> findCategories();

    /**
     * 도움말 수정
     */
    void update(HelpArticle article);

    /**
     * 도움말 삭제
     */
    void deleteById(Long id);
}
