# 03. 데이터베이스

PostgreSQL 데이터베이스 설계 및 MyBatis 매핑 가이드입니다.

## 목차

1. [데이터베이스 스키마](#데이터베이스-스키마)
2. [MyBatis 매핑 규칙](#mybatis-매핑-규칙)
3. [JSONB 타입 핸들러](#jsonb-타입-핸들러)
4. [쿼리 패턴](#쿼리-패턴)

## 데이터베이스 스키마

### 테이블 목록

| 테이블명 | 설명 | 주요 컬럼 |
|---------|------|----------|
| **users** | 사용자 (ADMIN/OWNER/STAFF) | email, password, role, status, business_id, staff_id |
| **refresh_tokens** | JWT 리프레시 토큰 | user_id, token, expires_at |
| **businesses** | 매장 정보 | owner_id, name, business_type, business_hours(JSONB), status |
| **business_settings** | 매장 예약 설정 | booking_interval, auto_confirm, kakao_api_key |
| **staffs** | 직원/디자이너/강사 | business_id, name, specialty, career_years, is_active |
| **portfolios** | 직원 포트폴리오 | staff_id, image_url, tags |
| **services** | 서비스 메뉴 | business_id, name, duration, price, staff_ids(TEXT) |
| **customers** | 고객 | business_id, name, phone, visit_count, total_spent, tags(TEXT) |
| **customer_histories** | 고객 시술 이력 | customer_id, staff_id, services(JSONB), details(JSONB) |
| **reservations** | 예약 | customer_id, staff_id, reservation_date, start_time, end_time, services(JSONB), status |
| **special_holidays** | 특별 휴무일 | business_id, holiday_date, reason |

### ERD (Entity Relationship Diagram)

```
users (1) ─────── (1) businesses
                     │
                     ├── (N) staffs
                     ├── (N) services
                     ├── (N) customers
                     │      │
                     │      ├── (N) customer_histories
                     │      └── (N) reservations
                     │
                     └── (N) special_holidays

staffs (1) ───── (N) portfolios
```

### 공통 컬럼 규칙

모든 테이블은 다음 컬럼을 포함합니다:

```sql
id BIGSERIAL PRIMARY KEY,           -- 기본 키 (자동 증가)
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 생성 시각
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP   -- 수정 시각
```

### Enum 타입 정의

#### user_role
```sql
CREATE TYPE user_role AS ENUM ('ADMIN', 'OWNER', 'STAFF');
```

#### user_status
```sql
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');
```

#### business_type
```sql
CREATE TYPE business_type AS ENUM ('BEAUTY_SHOP', 'PILATES', 'CAFE', 'OTHER');
```

#### business_status
```sql
CREATE TYPE business_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');
```

#### reservation_status
```sql
CREATE TYPE reservation_status AS ENUM ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW');
```

### JSONB 활용 예시

#### 1. businesses.business_hours (영업시간)

```json
{
  "MONDAY": {"open": "09:00", "close": "18:00", "isHoliday": false},
  "TUESDAY": {"open": "09:00", "close": "18:00", "isHoliday": false},
  "WEDNESDAY": {"open": "09:00", "close": "18:00", "isHoliday": false},
  "THURSDAY": {"open": "09:00", "close": "18:00", "isHoliday": false},
  "FRIDAY": {"open": "09:00", "close": "18:00", "isHoliday": false},
  "SATURDAY": {"open": "10:00", "close": "17:00", "isHoliday": false},
  "SUNDAY": {"open": null, "close": null, "isHoliday": true}
}
```

#### 2. reservations.services (예약 서비스 목록)

```json
[
  {"id": 1, "name": "컷", "price": 30000, "duration": 60},
  {"id": 2, "name": "펌", "price": 80000, "duration": 120}
]
```

#### 3. customer_histories.details (시술 상세 정보)

```json
{
  "hairLength": "short",
  "hairType": "straight",
  "dyeColor": "brown",
  "notes": "앞머리 짧게"
}
```

### 인덱스 전략

#### 단일 인덱스
```sql
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_business_id ON users(business_id);
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_reservations_business_id ON reservations(business_id);
```

#### 복합 인덱스
```sql
-- 예약 조회 성능 향상
CREATE INDEX idx_reservations_business_date ON reservations(business_id, reservation_date);
CREATE INDEX idx_reservations_staff_date ON reservations(staff_id, reservation_date);

-- 고객 검색 성능 향상
CREATE INDEX idx_customers_business_phone ON customers(business_id, phone);
```

#### 부분 인덱스
```sql
-- 활성 예약만 인덱싱
CREATE INDEX idx_reservations_active ON reservations(staff_id, reservation_date)
WHERE status IN ('PENDING', 'CONFIRMED');
```

### 트리거 (updated_at 자동 업데이트)

```sql
-- 함수 정의
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성 (모든 테이블에 적용)
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

## MyBatis 매핑 규칙

### 1. 기본 설정 (application.yml)

```yaml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true  # snake_case ↔ camelCase 자동 변환
  type-handlers-package: io.moer.booking.common.mybatis
```

### 2. ResultMap 정의

```xml
<resultMap id="userResultMap" type="io.moer.booking.domain.user.User">
    <id property="id" column="id"/>
    <result property="email" column="email"/>
    <result property="name" column="name"/>
    <result property="role" column="role"
            typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>
    <result property="status" column="status"
            typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>
    <result property="createdAt" column="created_at"/>
    <result property="updatedAt" column="updated_at"/>
</resultMap>
```

### 3. Enum 타입 처리

#### PostgreSQL Enum → Java Enum
```xml
<select id="findById" resultMap="userResultMap">
    SELECT * FROM users WHERE id = #{id}
</select>
```

#### Java Enum → PostgreSQL Enum
```xml
<insert id="save">
    INSERT INTO users (role, status)
    VALUES (#{role}::user_role, #{status}::user_status)
</insert>
```

**주의**: PostgreSQL Enum 사용 시 `::enum_type` 캐스팅 필수!

### 4. JSONB 타입 처리

#### Java Map/List → JSONB
```xml
<insert id="save">
    INSERT INTO reservations (services)
    VALUES (#{services, typeHandler=io.moer.booking.common.mybatis.JsonTypeHandler}::jsonb)
</insert>
```

#### JSONB → Java Map/List
```xml
<resultMap id="reservationResultMap" type="io.moer.booking.domain.reservation.Reservation">
    <result property="services" column="services"
            typeHandler="io.moer.booking.common.mybatis.JsonTypeHandler"/>
</resultMap>
```

### 5. 동적 쿼리

```xml
<select id="findByCondition" resultMap="userResultMap">
    SELECT * FROM users
    <where>
        <if test="condition.businessId != null">
            AND business_id = #{condition.businessId}
        </if>
        <if test="condition.role != null">
            AND role = #{condition.role}::user_role
        </if>
        <if test="condition.keyword != null and condition.keyword != ''">
            AND (
                name ILIKE '%' || #{condition.keyword} || '%'
                OR email ILIKE '%' || #{condition.keyword} || '%'
            )
        </if>
    </where>
    ORDER BY created_at DESC
    LIMIT #{limit} OFFSET #{offset}
</select>
```

## JSONB 타입 핸들러

### JsonTypeHandler.java

**위치**: `src/main/java/io/moer/booking/common/mybatis/JsonTypeHandler.java`

```java
@MappedTypes({Map.class, List.class})
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonTypeHandler extends BaseTypeHandler<Object> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                     Object parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            String json = objectMapper.writeValueAsString(parameter);
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(json);
            ps.setObject(i, jsonObject);
        } catch (Exception e) {
            throw new SQLException("Error converting object to JSON", e);
        }
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    private Object parseJson(String json) throws SQLException {
        if (json == null) {
            return null;
        }
        try {
            // JSON 시작 문자로 Map/List 구분
            if (json.trim().startsWith("[")) {
                return objectMapper.readValue(json, List.class);
            } else {
                return objectMapper.readValue(json, Map.class);
            }
        } catch (Exception e) {
            throw new SQLException("Error parsing JSON", e);
        }
    }
}
```

## 쿼리 패턴

### 1. 시간 충돌 검증

```xml
<select id="existsConflictingReservation" resultType="boolean">
    SELECT EXISTS (
        SELECT 1
        FROM reservations
        WHERE staff_id = #{staffId}
          AND reservation_date = #{date}
          AND status IN ('PENDING', 'CONFIRMED')
          AND (
              (#{startTime} >= start_time AND #{startTime} < end_time)
              OR (#{endTime} > start_time AND #{endTime} <= end_time)
              OR (#{startTime} <= start_time AND #{endTime} >= end_time)
          )
          <if test="excludeId != null">
              AND id != #{excludeId}
          </if>
    )
</select>
```

### 2. 집계 쿼리 (고객 방문 횟수)

```xml
<update id="incrementVisitCount">
    UPDATE customers
    SET visit_count = visit_count + 1,
        total_spent = total_spent + #{amount}
    WHERE id = #{customerId}
</update>
```

### 3. 조인 쿼리

```xml
<select id="findDetailById" resultType="ReservationResponse">
    SELECT
        r.*,
        c.name as customer_name,
        c.phone as customer_phone,
        s.name as staff_name,
        s.specialty as staff_specialty
    FROM reservations r
    INNER JOIN customers c ON r.customer_id = c.id
    LEFT JOIN staffs s ON r.staff_id = s.id
    WHERE r.id = #{id}
</select>
```

### 4. 날짜 범위 검색

```xml
<select id="findByDateRange" resultMap="reservationResultMap">
    SELECT * FROM reservations
    WHERE business_id = #{businessId}
      AND reservation_date BETWEEN #{startDate} AND #{endDate}
    ORDER BY reservation_date, start_time
</select>
```

## Best Practices

### 1. PreparedStatement 사용
✅ MyBatis는 기본적으로 PreparedStatement 사용 (SQL Injection 방지)

### 2. 트랜잭션 관리
✅ Service 계층에서 `@Transactional` 사용

### 3. N+1 문제 방지
✅ JOIN 쿼리 사용 또는 Batch 조회

### 4. 인덱스 활용
✅ 검색 조건에 맞는 인덱스 생성

### 5. JSONB 활용
✅ 유연한 데이터 구조가 필요할 때 JSONB 사용
❌ 자주 검색/정렬하는 필드는 컬럼으로 분리

## 관련 문서

- [도메인 개발 패턴](../02_domain/development-pattern.md)
- [아키텍처](../01_architecture/README.md)
