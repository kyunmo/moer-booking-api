# 도메인 개발 패턴 상세

새로운 도메인을 추가하는 방법을 단계별로 설명합니다.

## 개요

새 도메인을 추가하는 표준 절차를 따르면 일관성 있는 코드베이스를 유지할 수 있습니다.

## 단계별 가이드

### Step 1: 요구사항 분석

먼저 다음 질문에 답하세요:

1. **엔티티 이름은?** (예: `Product`)
2. **주요 속성은?** (예: name, price, description)
3. **관계는?** (예: Business 1:N Product)
4. **주요 기능은?** (CRUD, 검색, 상태 관리 등)
5. **비즈니스 규칙은?** (예: 가격은 0보다 커야 함)

### Step 2: 데이터베이스 테이블 생성

**파일**: `src/main/resources/db/schema.sql`

```sql
-- products 테이블 생성
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price INTEGER NOT NULL CHECK (price >= 0),
    stock INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT products_business_id_fkey FOREIGN KEY (business_id)
        REFERENCES businesses(id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX idx_products_business_id ON products(business_id);
CREATE INDEX idx_products_is_active ON products(is_active);

-- 업데이트 트리거
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### Step 3: 패키지 생성

```
src/main/java/io/moer/booking/domain/product/
├── controller/
├── dto/
├── repository/
└── service/
```

### Step 4: Entity 작성

**파일**: `src/main/java/io/moer/booking/domain/product/Product.java`

```java
package io.moer.booking.domain.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    private Long id;
    private Long businessId;
    private String name;
    private String description;
    private Integer price;
    private Integer stock;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // 헬퍼 메서드 (비즈니스 로직)
    // ========================================

    /**
     * 재고 감소
     */
    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new BusinessException(
                ErrorCode.INVALID_INPUT_VALUE,
                "재고가 부족합니다"
            );
        }
        this.stock -= quantity;
    }

    /**
     * 재고 증가
     */
    public void increaseStock(int quantity) {
        this.stock += quantity;
    }

    /**
     * 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
}
```

### Step 5: DTO 작성

#### ProductCreateRequest.java

```java
package io.moer.booking.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {

    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 100, message = "상품명은 100자 이하여야 합니다")
    private String name;

    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다")
    private String description;

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private Integer price;

    @Min(value = 0, message = "재고는 0 이상이어야 합니다")
    private Integer stock;
}
```

#### ProductResponse.java

```java
package io.moer.booking.domain.product.dto;

import io.moer.booking.domain.product.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private Long businessId;
    private String name;
    private String description;
    private Integer price;
    private Integer stock;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .businessId(product.getBusinessId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
```

#### ProductSearchCondition.java

```java
package io.moer.booking.domain.product.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchCondition {
    private Long businessId;
    private String keyword;      // 이름/설명 검색
    private Boolean isActive;    // 활성/비활성 필터
    private Integer minPrice;
    private Integer maxPrice;
}
```

### Step 6: Repository 작성

**파일**: `src/main/java/io/moer/booking/domain/product/repository/ProductRepository.java`

```java
package io.moer.booking.domain.product.repository;

import io.moer.booking.domain.product.Product;
import io.moer.booking.domain.product.dto.ProductSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductRepository {

    // 기본 CRUD
    void save(Product product);
    Optional<Product> findById(Long id);
    void update(Product product);
    void deleteById(Long id);

    // 커스텀 조회
    List<Product> findByBusinessId(Long businessId);

    // 동적 검색
    List<Product> findByCondition(
            @Param("condition") ProductSearchCondition condition,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
    int countByCondition(@Param("condition") ProductSearchCondition condition);
}
```

### Step 7: MyBatis XML 작성

**파일**: `src/main/resources/mapper/product/ProductMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.moer.booking.domain.product.repository.ProductRepository">

    <!-- ResultMap -->
    <resultMap id="productResultMap" type="io.moer.booking.domain.product.Product">
        <id property="id" column="id"/>
        <result property="businessId" column="business_id"/>
        <result property="name" column="name"/>
        <result property="description" column="description"/>
        <result property="price" column="price"/>
        <result property="stock" column="stock"/>
        <result property="isActive" column="is_active"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <!-- 저장 -->
    <insert id="save" parameterType="io.moer.booking.domain.product.Product"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO products (
            business_id, name, description, price, stock, is_active
        ) VALUES (
            #{businessId}, #{name}, #{description}, #{price}, #{stock}, #{isActive}
        )
    </insert>

    <!-- 조회 -->
    <select id="findById" resultMap="productResultMap">
        SELECT * FROM products WHERE id = #{id}
    </select>

    <!-- 수정 -->
    <update id="update">
        UPDATE products
        SET name = #{name},
            description = #{description},
            price = #{price},
            stock = #{stock},
            is_active = #{isActive}
        WHERE id = #{id}
    </update>

    <!-- 삭제 -->
    <delete id="deleteById">
        DELETE FROM products WHERE id = #{id}
    </delete>

    <!-- Business별 조회 -->
    <select id="findByBusinessId" resultMap="productResultMap">
        SELECT * FROM products
        WHERE business_id = #{businessId}
        ORDER BY created_at DESC
    </select>

    <!-- 동적 검색 -->
    <select id="findByCondition" resultMap="productResultMap">
        SELECT * FROM products
        <where>
            <if test="condition.businessId != null">
                AND business_id = #{condition.businessId}
            </if>
            <if test="condition.keyword != null and condition.keyword != ''">
                AND (
                    name ILIKE '%' || #{condition.keyword} || '%'
                    OR description ILIKE '%' || #{condition.keyword} || '%'
                )
            </if>
            <if test="condition.isActive != null">
                AND is_active = #{condition.isActive}
            </if>
            <if test="condition.minPrice != null">
                AND price >= #{condition.minPrice}
            </if>
            <if test="condition.maxPrice != null">
                AND price <= #{condition.maxPrice}
            </if>
        </where>
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <!-- 개수 조회 -->
    <select id="countByCondition" resultType="int">
        SELECT COUNT(*) FROM products
        <where>
            <if test="condition.businessId != null">
                AND business_id = #{condition.businessId}
            </if>
            <if test="condition.keyword != null and condition.keyword != ''">
                AND (
                    name ILIKE '%' || #{condition.keyword} || '%'
                    OR description ILIKE '%' || #{condition.keyword} || '%'
                )
            </if>
            <if test="condition.isActive != null">
                AND is_active = #{condition.isActive}
            </if>
            <if test="condition.minPrice != null">
                AND price >= #{condition.minPrice}
            </if>
            <if test="condition.maxPrice != null">
                AND price <= #{condition.maxPrice}
            </if>
        </where>
    </select>

</mapper>
```

### Step 8: Service 작성

**파일**: `src/main/java/io/moer/booking/domain/product/service/ProductService.java`

```java
package io.moer.booking.domain.product.service;

import io.moer.booking.common.dto.PageInfo;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.product.Product;
import io.moer.booking.domain.product.dto.*;
import io.moer.booking.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 상품 생성
     */
    @Transactional
    public ProductResponse createProduct(Long businessId, ProductCreateRequest request) {
        log.info("Creating product: businessId={}, name={}", businessId, request.getName());

        // Entity 생성
        Product product = Product.builder()
                .businessId(businessId)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .isActive(true)
                .build();

        // 저장
        productRepository.save(product);

        log.info("Product created: id={}", product.getId());

        return ProductResponse.from(product);
    }

    /**
     * 상품 조회
     */
    public ProductResponse getProduct(Long businessId, Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "상품을 찾을 수 없습니다"
                ));

        // Business 일치 확인
        if (!product.getBusinessId().equals(businessId)) {
            throw new BusinessException(
                ErrorCode.ACCESS_DENIED,
                "해당 상품에 접근 권한이 없습니다"
            );
        }

        return ProductResponse.from(product);
    }

    /**
     * 상품 목록 조회 (검색/페이징)
     */
    public PageResponse<ProductResponse> getProducts(
            ProductSearchCondition condition, int page, int size) {

        int offset = (page - 1) * size;

        List<Product> products = productRepository.findByCondition(condition, offset, size);
        int totalElements = productRepository.countByCondition(condition);

        List<ProductResponse> content = products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());

        PageInfo pageInfo = PageInfo.of(page, size, totalElements);

        return new PageResponse<>(content, pageInfo);
    }

    /**
     * 상품 수정
     */
    @Transactional
    public ProductResponse updateProduct(
            Long businessId, Long id, ProductUpdateRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "상품을 찾을 수 없습니다"
                ));

        // Business 일치 확인
        if (!product.getBusinessId().equals(businessId)) {
            throw new BusinessException(
                ErrorCode.ACCESS_DENIED,
                "해당 상품에 접근 권한이 없습니다"
            );
        }

        // 수정 (Builder 패턴으로 새 객체 생성)
        Product updatedProduct = Product.builder()
                .id(product.getId())
                .businessId(product.getBusinessId())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .build();

        productRepository.update(updatedProduct);

        log.info("Product updated: id={}", id);

        return ProductResponse.from(updatedProduct);
    }

    /**
     * 상품 삭제
     */
    @Transactional
    public void deleteProduct(Long businessId, Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "상품을 찾을 수 없습니다"
                ));

        // Business 일치 확인
        if (!product.getBusinessId().equals(businessId)) {
            throw new BusinessException(
                ErrorCode.ACCESS_DENIED,
                "해당 상품에 접근 권한이 없습니다"
            );
        }

        productRepository.deleteById(id);

        log.info("Product deleted: id={}", id);
    }
}
```

### Step 9: Controller 작성

**파일**: `src/main/java/io/moer/booking/domain/product/controller/ProductController.java`

```java
package io.moer.booking.domain.product.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.domain.product.dto.*;
import io.moer.booking.domain.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/businesses/{businessId}/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "상품 관리 API")
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 생성
     */
    @PostMapping
    @Operation(summary = "상품 생성")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @PathVariable Long businessId,
            @Valid @RequestBody ProductCreateRequest request) {

        ProductResponse response = productService.createProduct(businessId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 상품 목록 조회
     */
    @GetMapping
    @Operation(summary = "상품 목록 조회")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @PathVariable Long businessId,
            @ModelAttribute ProductSearchCondition condition,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        condition.setBusinessId(businessId);  // businessId 설정

        PageResponse<ProductResponse> response = productService.getProducts(condition, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 상품 상세 조회
     */
    @GetMapping("/{id}")
    @Operation(summary = "상품 상세 조회")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable Long businessId,
            @PathVariable Long id) {

        ProductResponse response = productService.getProduct(businessId, id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 상품 수정
     */
    @PutMapping("/{id}")
    @Operation(summary = "상품 수정")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long businessId,
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {

        ProductResponse response = productService.updateProduct(businessId, id, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 상품 삭제
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "상품 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long businessId,
            @PathVariable Long id) {

        productService.deleteProduct(businessId, id);

        return ResponseEntity.ok(ApiResponse.success());
    }
}
```

### Step 10: ErrorCode 추가

**파일**: `src/main/java/io/moer/booking/common/exception/ErrorCode.java`

```java
// Product 관련 에러 (PR001~PR099)
PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PR001", "상품을 찾을 수 없습니다"),
PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PR002", "재고가 부족합니다"),
```

### Step 11: 테스트

#### 수동 테스트 (Swagger UI)

1. http://localhost:8080/swagger-ui.html 접속
2. Product API 섹션에서 테스트
3. 각 엔드포인트 실행 및 응답 확인

#### 통합 테스트 작성 (선택)

```java
@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Test
    void 상품_생성_성공() {
        // given
        ProductCreateRequest request = new ProductCreateRequest(
            "테스트 상품", "설명", 10000, 100
        );

        // when
        ProductResponse response = productService.createProduct(1L, request);

        // then
        assertThat(response.getName()).isEqualTo("테스트 상품");
        assertThat(response.getPrice()).isEqualTo(10000);
    }
}
```

## 체크리스트

새 도메인 추가 시 확인할 체크리스트:

- [ ] 데이터베이스 테이블 생성
- [ ] Entity 클래스 작성
- [ ] DTO 클래스 작성 (Request, Response, SearchCondition)
- [ ] Repository 인터페이스 작성
- [ ] MyBatis XML 매퍼 작성
- [ ] Service 클래스 작성
- [ ] Controller 클래스 작성
- [ ] ErrorCode 추가
- [ ] Swagger 문서 확인
- [ ] 테스트 작성 (선택)
- [ ] Git 커밋

## 다음 문서

- [Reservation 도메인](./reservation.md) - 복잡한 비즈니스 로직 예시
- [User 도메인](./user.md) - 인증/권한 예시
