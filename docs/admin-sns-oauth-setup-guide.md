# SNS OAuth 설정 가이드 (관리자 수동 작업)

> **작성일:** 2026-02-18
> **대상:** 프로젝트 관리자 (직접 해야 하는 작업)
> **백엔드 구현:** 완료 (카카오/네이버/구글 3개 모두)

---

## 현재 상태

| 제공자 | 백엔드 구현 | 개발자 콘솔 등록 | 키 발급 | 운영 배포 심사 |
|--------|-----------|----------------|--------|--------------|
| 카카오 | ✅ 완료 | ✅ 완료 (개발용 키 설정됨) | ✅ 개발용 | ❌ 미진행 |
| 네이버 | ✅ 완료 | ✅ 완료 (개발용 키 설정됨) | ✅ 개발용 | ❌ 미진행 |
| 구글   | ✅ 완료 | ✅ 완료 (개발용 키 설정됨) | ✅ 개발용 | ❌ 미진행 (기본 scope는 심사 불필요) |

> 개발 환경에서는 현재 키로 테스트 가능합니다.
> 운영 배포 전에 아래 작업이 필요합니다.

---

## 1. 카카오 (developers.kakao.com)

### 1-1. 이미 완료된 것
- 애플리케이션 등록
- 카카오 로그인 활성화
- 개발용 Redirect URI 등록: `http://localhost:8080/login/oauth2/code/kakao`
- 동의항목 설정: `profile_nickname`, `account_email`

### 1-2. 운영 배포 전 해야 할 것

#### (A) Redirect URI 추가 등록
1. [카카오 개발자 콘솔](https://developers.kakao.com) 접속
2. 내 애플리케이션 → 해당 앱 선택
3. **카카오 로그인 > Redirect URI** 메뉴
4. 운영 URL 추가:
   ```
   https://yemo.kr/login/oauth2/code/kakao
   ```
5. (스테이징이 있다면)
   ```
   https://staging.yemo.kr/login/oauth2/code/kakao
   ```

#### (B) 플랫폼 등록
1. **앱 설정 > 플랫폼** 메뉴
2. Web 플랫폼 추가:
   - 사이트 도메인: `https://yemo.kr`
   - (개발용은 이미 등록되어 있을 것: `http://localhost:3000`)

#### (C) 서비스 배포 (심사)
1. **앱 설정 > 앱 키** 메뉴에서 서비스 상태 확인
2. **"배포"** 상태로 변경 신청
3. 카카오 검수 양식 작성:
   - 서비스 설명 (예약 관리 시스템)
   - 동의항목 사용 목적
   - 스크린샷 첨부
4. 심사 소요: **약 3~5 영업일**

#### (D) 운영 환경변수 설정
```env
KAKAO_CLIENT_ID=37ec5f253dda4a9f0c543ee0249557f5    # 현재 개발용 키 (운영용으로 교체 필요 시)
KAKAO_CLIENT_SECRET=8zOJXees3y4v1Zz2vckIfmhjCVYRNVcU  # 현재 개발용 키
```

---

## 2. 네이버 (developers.naver.com)

### 2-1. 이미 완료된 것
- 애플리케이션 등록
- 네이버 로그인 API 추가
- 개발용 Callback URL 등록
- 동의항목 설정: `name`, `email`

### 2-2. 운영 배포 전 해야 할 것

#### (A) Callback URL 추가 등록
1. [네이버 개발자 센터](https://developers.naver.com) 접속
2. Application > 내 애플리케이션 > 해당 앱
3. **API 설정** 탭
4. **로그인 오픈 API 서비스 환경** 섹션
5. PC웹/모바일웹 Callback URL 추가:
   ```
   http://localhost:8080/login/oauth2/code/naver
   https://yemo.kr/login/oauth2/code/naver
   ```

#### (B) 테스터 등록 (개발 중 상태에서)
1. **멤버관리** 탭
2. 테스터 네이버 아이디 추가 (앱 상태가 "개발 중"일 때 테스트 가능한 계정)
3. **주의:** "개발 중" 상태에서는 등록된 테스터만 로그인 가능

#### (C) 서비스 적용 신청
1. **API 설정** 탭 하단
2. **"서비스 적용"** 버튼 클릭
3. 필요 정보:
   - 서비스 URL: `https://yemo.kr`
   - 서비스 명: YEMO
   - 서비스 설명
   - 서비스 대표 이미지 (로고)
   - 이용약관/개인정보처리방침 URL
4. 심사 소요: **약 2~3 영업일**

#### (D) 운영 환경변수 설정
```env
NAVER_CLIENT_ID=m5JkQiqh_8UyKO365EAh       # 현재 개발용 키
NAVER_CLIENT_SECRET=JcUZkcUPhe              # 현재 개발용 키
```

---

## 3. 구글 (console.cloud.google.com)

### 3-1. 이미 완료된 것
- 프로젝트 생성
- OAuth 2.0 클라이언트 ID 생성
- 개발용 리디렉션 URI 등록
- scope 설정: `email`, `profile`

### 3-2. 운영 배포 전 해야 할 것

#### (A) 승인된 리디렉션 URI 추가
1. [Google Cloud Console](https://console.cloud.google.com) 접속
2. **API 및 서비스 > 사용자 인증 정보** 메뉴
3. OAuth 2.0 클라이언트 ID 클릭
4. **승인된 리디렉션 URI** 추가:
   ```
   https://yemo.kr/login/oauth2/code/google
   ```

#### (B) 승인된 JavaScript 원본 추가
1. 같은 설정 화면에서
2. **승인된 JavaScript 원본** 추가:
   ```
   https://yemo.kr
   ```

#### (C) OAuth 동의 화면 설정
1. **API 및 서비스 > OAuth 동의 화면** 메뉴
2. 필수 설정:
   - 앱 이름: `YEMO`
   - 사용자 지원 이메일: `kkm@moer.io`
   - 앱 도메인: `https://yemo.kr`
   - 개발자 연락처 이메일: `kkm@moer.io`
3. **게시 상태 변경:**
   - "테스트" → "프로덕션"으로 변경
   - 기본 scope(`email`, `profile`, `openid`)만 사용하므로 **Google 심사 불필요**

#### (D) 운영 환경변수 설정
```env
GOOGLE_CLIENT_ID=325667166855-...apps.googleusercontent.com  # 현재 개발용 키
GOOGLE_CLIENT_SECRET=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    # 현재 개발용 키
```

---

## 4. 공통 운영 배포 환경변수

```env
# OAuth2 리다이렉트 (프론트엔드 URL)
OAUTH2_REDIRECT_URI=https://yemo.kr/oauth2-redirect
OAUTH2_CUSTOMER_REDIRECT_URI=https://yemo.kr/oauth2-redirect

# 각 제공자 키 (필요 시 운영용으로 교체)
KAKAO_CLIENT_ID=...
KAKAO_CLIENT_SECRET=...
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...

# JWT
JWT_SECRET=운영용_시크릿키
```

---

## 5. 작업 체크리스트

### 즉시 (개발 테스트)
- [ ] 카카오 관리자 로그인 테스트 (`/oauth2/authorize/kakao?loginType=admin`)
- [ ] 네이버 관리자 로그인 테스트 (`/oauth2/authorize/naver?loginType=admin`)
- [ ] 구글 관리자 로그인 테스트 (`/oauth2/authorize/google?loginType=admin`)
- [ ] 네이버 테스터 계정 등록 (개발 중 상태에서 필요)

### 운영 배포 전
- [ ] 카카오: 운영 Redirect URI 추가 + 플랫폼 등록 + 배포 심사 신청
- [ ] 네이버: 운영 Callback URL 추가 + 서비스 적용 신청
- [ ] 구글: 운영 리디렉션 URI 추가 + OAuth 동의 화면 "프로덕션" 전환
- [ ] 운영 환경변수 설정 (.env 또는 CI/CD 시크릿)
- [ ] HTTPS 인증서 적용 확인 (카카오/네이버/구글 모두 운영 시 HTTPS 필수)

### 심사 소요 시간
| 제공자 | 심사 기간 | 비고 |
|--------|----------|------|
| 카카오 | 3~5 영업일 | 검수 양식 + 스크린샷 필요 |
| 네이버 | 2~3 영업일 | 서비스 URL + 이용약관 URL 필요 |
| 구글 | 없음 | 기본 scope만 사용 시 심사 불필요 |

---

## 6. 주의사항

1. **이용약관/개인정보처리방침 URL이 필요합니다**
   - 카카오/네이버 심사 시 제출해야 함
   - 프론트엔드 Phase A-1에서 정식 버전 작성 필요
   - 임시로라도 URL이 있어야 심사 진행 가능

2. **네이버는 "개발 중" 상태에서 테스터만 로그인 가능**
   - 반드시 테스터 계정을 등록해야 개발 테스트 가능
   - 멤버관리 탭에서 네이버 아이디 추가

3. **보안 키 관리**
   - client-secret은 절대 공개 저장소에 올리지 않을 것
   - 현재 application.yml에 기본값으로 들어있는 키는 개발용
   - 운영 시 환경변수로 덮어씌움

4. **동일 이메일 계정 자동 연동**
   - SNS 로그인 시 동일 이메일의 기존 계정이 있으면 자동으로 SNS 연결
   - 별도 "계정 연동" 과정 불필요
