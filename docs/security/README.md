# 보안 문서 (docs/security)

moer-booking 백엔드 보안 점검, 개선 계획, 배포 체크리스트 모음.

## 문서 목록

| 문서 | 용도 | 갱신 주기 |
|------|------|----------|
| [audit-2026-05-12.md](./audit-2026-05-12.md) | 보안 감사 보고서 (발견 사항 + 위험도) | 분기 또는 메이저 릴리즈마다 |
| [improvement-plan.md](./improvement-plan.md) | 우선순위별 개선 계획 (P0~P3) | 항목 완료/추가 시 |
| [launch-checklist.md](./launch-checklist.md) | 프로덕션 배포 전 필수 체크리스트 | 배포마다 사본 생성하여 사용 |

## 사용 흐름

```
1. audit-*.md 로 현재 위험 식별
2. improvement-plan.md 로 우선순위 작업
3. launch-checklist.md 로 배포 직전 검증
```

## 다음 감사 일정

- **현재 감사**: 2026-05-12
- **다음 권장 감사**: 2026-08-12 (3개월 후) 또는 메이저 기능 추가 직후
