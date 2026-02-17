# [백엔드 요청] 매장 정보 API - slug 필드 누락 이슈

## 현상
- 매장 설정 > 기본정보 페이지에서 "예약 페이지 주소" 영역이 표시되지 않음
- `currentSlug` 값이 빈 문자열이어서 `v-if="currentSlug"` 조건에 의해 UI가 숨겨짐

## 원인 분석
- 프론트엔드는 매장 정보 로드 시 `business.slug` 값을 읽어 예약 URL을 구성
- **`GET /businesses/{businessId}` API 응답에 `slug` 필드가 포함되지 않는 것으로 확인**
- slug 저장(PATCH)은 정상 동작하나, 조회(GET) 시 반환되지 않음

## 프론트엔드 코드 참조
```javascript
// src/pages/shop-admin/business-settings/index.vue
async function loadBusinessInfo() {
  const business = settingsStore.business
  currentSlug.value = business.slug || ''  // ← slug가 응답에 없으면 빈 문자열
  slugForm.value.slug = business.slug || ''
}

const bookingUrl = computed(() => {
  const base = window.location.origin
  return `${base}/booking/${currentSlug.value}`
})
```

## 요청사항
1. **`GET /businesses/{businessId}` 응답에 `slug` 필드 추가** 요청
    - 현재 응답 예시에 `slug` 필드가 누락된 것으로 보임
    - 기대 응답 형식:
   ```json
   {
     "id": 1,
     "name": "모어 네일",
     "slug": "moer-nail",
     "...기타 필드": "..."
   }
   ```

## 관련 API
- `GET /businesses/{businessId}` - slug 필드 추가 필요
- `PATCH /businesses/{businessId}/slug` - 정상 동작 (저장은 됨)

## 우선순위
중간 - 관리자가 예약 URL을 확인/공유할 수 없는 상태
