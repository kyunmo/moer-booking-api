package io.moer.booking.domain.help.service;

import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.help.HelpArticle;
import io.moer.booking.domain.help.dto.HelpArticleCreateRequest;
import io.moer.booking.domain.help.dto.HelpArticleResponse;
import io.moer.booking.domain.help.dto.HelpListResponse;
import io.moer.booking.domain.help.repository.HelpArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 도움말 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HelpArticleService {

    private final HelpArticleRepository helpArticleRepository;

    /**
     * 도움말 목록 조회 (Public)
     *
     * @param category 카테고리 필터 (nullable)
     * @param keyword  키워드 검색 (nullable)
     * @param lang     언어 필터 (nullable, 기본 ko)
     * @return 도움말 목록 응답
     */
    public HelpListResponse getHelpList(String category, String keyword, String lang) {
        String effectiveLang = (lang == null || lang.isBlank()) ? "ko" : lang;

        List<HelpArticle> articles = helpArticleRepository.findByCondition(
                category, keyword, effectiveLang, true);

        int totalCount = articles.size();

        List<HelpArticleResponse> items = articles.stream()
                .map(HelpArticleResponse::from)
                .toList();

        return HelpListResponse.builder()
                .categories(HelpListResponse.allCategories())
                .items(items)
                .totalCount(totalCount)
                .build();
    }

    /**
     * 도움말 단건 조회 (Public)
     *
     * @param id 도움말 ID
     * @return 도움말 응답
     */
    public HelpArticleResponse getHelpArticle(Long id) {
        HelpArticle article = helpArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.HELP_ARTICLE_NOT_FOUND,
                        "도움말을 찾을 수 없습니다: " + id
                ));

        return HelpArticleResponse.from(article);
    }

    /**
     * 도움말 생성 (SuperAdmin)
     *
     * @param request 생성 요청
     * @return 생성된 도움말 응답
     */
    @Transactional
    public HelpArticleResponse createArticle(HelpArticleCreateRequest request) {
        log.info("Creating help article: category={}, title={}", request.getCategory(), request.getTitle());

        HelpArticle article = HelpArticle.builder()
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .relatedFeature(request.getRelatedFeature())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .lang(request.getLang() != null ? request.getLang() : "ko")
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : true)
                .build();

        helpArticleRepository.save(article);

        log.info("Help article created: id={}", article.getId());

        return HelpArticleResponse.from(article);
    }

    /**
     * 도움말 수정 (SuperAdmin)
     *
     * @param id      도움말 ID
     * @param request 수정 요청
     * @return 수정된 도움말 응답
     */
    @Transactional
    public HelpArticleResponse updateArticle(Long id, HelpArticleCreateRequest request) {
        log.info("Updating help article: id={}", id);

        HelpArticle existing = helpArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.HELP_ARTICLE_NOT_FOUND,
                        "도움말을 찾을 수 없습니다: " + id
                ));

        HelpArticle updated = HelpArticle.builder()
                .id(existing.getId())
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .relatedFeature(request.getRelatedFeature())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .lang(request.getLang() != null ? request.getLang() : "ko")
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : true)
                .build();

        helpArticleRepository.update(updated);

        log.info("Help article updated: id={}", id);

        return HelpArticleResponse.from(updated);
    }

    /**
     * 도움말 삭제 (SuperAdmin)
     *
     * @param id 도움말 ID
     */
    @Transactional
    public void deleteArticle(Long id) {
        log.info("Deleting help article: id={}", id);

        helpArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.HELP_ARTICLE_NOT_FOUND,
                        "도움말을 찾을 수 없습니다: " + id
                ));

        helpArticleRepository.deleteById(id);

        log.info("Help article deleted: id={}", id);
    }

    /**
     * 도움말 목록 조회 (SuperAdmin - 비공개 포함)
     *
     * @param category 카테고리 필터 (nullable)
     * @param keyword  키워드 검색 (nullable)
     * @param lang     언어 필터 (nullable)
     * @return 도움말 목록 응답
     */
    public HelpListResponse getHelpListForAdmin(String category, String keyword, String lang) {
        List<HelpArticle> articles = helpArticleRepository.findByCondition(
                category, keyword, lang, false);

        int totalCount = articles.size();

        List<HelpArticleResponse> items = articles.stream()
                .map(HelpArticleResponse::from)
                .toList();

        return HelpListResponse.builder()
                .categories(HelpListResponse.allCategories())
                .items(items)
                .totalCount(totalCount)
                .build();
    }
}
