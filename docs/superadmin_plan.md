슈퍼 관리자(SUPER_ADMIN) 기능 구현 계획

Context

문제점

현재 moer-booking 시스템은 ADMIN, OWNER, STAFF 3개 역할만 존재하며, 전체 시스템을 관리할 수 있는 슈퍼 관리자 기능이 없습니다. 또한 다음과 같은 문제점이 발견되었습니다:

1. 슈퍼 관리자 부재: 전체 업체를 조회/관리할 수 없음
2. 보안 취약점: BusinessService에 권한 체크가 없어 OWNER가 다른 OWNER의 매장도 접근 가능
3. 감사 로그 부재: 중요 액션(삭제, 역할 변경 등)에 대한 추적 불가
4. 시스템 통계 부재: 전체 시스템 통계를 볼 수 없음 (개별 매장 통계만 존재)

목표

완전한 슈퍼 관리자 시스템을 구축하여:
- 전체 업체 및 사용자 관리
- 시스템 통계 및 감사 로그 제공
- 사용자 지원 및 보고서 생성
- 데이터 백업/복원 및 시스템 모니터링

 ---
구현 계획

Phase 1: 핵심 기능 (우선순위 최상, 12일)

1.1 UserRole 확장 및 권한 시스템 강화

변경 파일:
- domain/user/UserRole.java - SUPER_ADMIN enum 추가
- domain/user/User.java - isSuperAdmin(), canAccessBusiness() 로직 수정
- common/security/CustomUserDetails.java - isSuperAdmin() 추가
- common/exception/ErrorCode.java - SUPER_ADMIN 관련 에러 코드 추가

UserRole 확장:
public enum UserRole {
SUPER_ADMIN("슈퍼 관리자"),  // 추가                                                                                                                                                                                                                                           
ADMIN("시스템 관리자"),
OWNER("매장 사장님"),
STAFF("직원");
}

권한 계층:
- SUPER_ADMIN: 모든 매장 + 사용자 관리 + 시스템 설정
- ADMIN: 모든 매장 접근 (기존)
- OWNER: 자신의 businessId만
- STAFF: 자신의 businessId만

User.canAccessBusiness() 수정:
public boolean canAccessBusiness(Long businessId) {
if (isSuperAdmin() || isAdmin()) {
return true;  // SUPER_ADMIN, ADMIN 모두 전체 접근                                                                                                                                                                                                                         
}
return this.businessId != null && this.businessId.equals(businessId);
}

ErrorCode 추가:
// Super Admin 관련 (SA001~SA010)                                                                                                                                                                                                                                                  
SUPER_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "SA001", "슈퍼 관리자 권한이 필요합니다"),
SUPER_ADMIN_CANNOT_BE_DELETED(HttpStatus.BAD_REQUEST, "SA002", "슈퍼 관리자는 삭제할 수 없습니다"),
SUPER_ADMIN_ONLY_ACTION(HttpStatus.FORBIDDEN, "SA003", "슈퍼 관리자만 수행할 수 있는 작업입니다"),

// Audit Log 관련 (AL001~AL010)                                                                                                                                                                                                                                                    
AUDIT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "AL001", "감사 로그를 찾을 수 없습니다"),

1.2 기존 Business 도메인 권한 보안 강화

변경 파일:
- domain/business/controller/BusinessController.java - @AuthenticationPrincipal 추가
- domain/business/service/BusinessService.java - 모든 메서드에 권한 체크 추가

BusinessService 보안 강화:
// 모든 public 메서드에 currentUser 파라미터 추가 및 권한 체크                                                                                                                                                                                                                     
public BusinessResponse getBusiness(Long id, User currentUser) {
Business business = businessRepository.findById(id)
.orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

     // 권한 체크                                                                                                                                                                                                                                                                   
     if (!currentUser.canAccessBusiness(id)) {
         throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
     }

     return getBusinessWithSettings(id);
}

// update, delete 등 모든 메서드에 동일하게 적용

1.3 감사 로그 시스템 구현

새 도메인: domain/auditlog/

파일 구조:
domain/auditlog/
├── AuditLog.java                      # Entity
├── AuditAction.java                   # Enum (액션 타입)
├── controller/
│   └── AuditLogController.java        # 조회 API
├── dto/
│   ├── AuditLogResponse.java
│   └── AuditLogSearchCondition.java
├── repository/
│   └── AuditLogRepository.java
└── service/
└── AuditLogService.java

AuditLog Entity:
@Getter @Builder                                                                                                                                                                                                                                                                   
public class AuditLog {
private Long id;
private Long userId;              // 액션 수행자                                                                                                                                                                                                                               
private String userEmail;
private UserRole userRole;

     private String action;            // BUSINESS_CREATED, USER_ROLE_CHANGED 등                                                                                                                                                                                                    
     private String entityType;        // Business, User, Reservation 등                                                                                                                                                                                                            
     private Long entityId;

     private String description;
     private Map<String, Object> metadata;  // JSONB - 변경 전/후 값                                                                                                                                                                                                                

     private String ipAddress;
     private String userAgent;

     private LocalDateTime createdAt;
}

AuditAction Enum:
public enum AuditAction {
// Business                                                                                                                                                                                                                                                                    
BUSINESS_CREATED, BUSINESS_UPDATED, BUSINESS_DELETED, BUSINESS_STATUS_CHANGED,

     // User                                                                                                                                                                                                                                                                        
     USER_CREATED, USER_ROLE_CHANGED, USER_STATUS_CHANGED, USER_DELETED,

     // System                                                                                                                                                                                                                                                                      
     SYSTEM_BACKUP, SYSTEM_RESTORE, SYSTEM_CONFIG_CHANGED
}

DB 스키마:
CREATE TABLE audit_logs (
id BIGSERIAL PRIMARY KEY,
user_id BIGINT,
user_email VARCHAR(100),
user_role VARCHAR(20),

     action VARCHAR(50) NOT NULL,
     entity_type VARCHAR(50),
     entity_id BIGINT,

     description TEXT,
     metadata JSONB,

     ip_address VARCHAR(50),
     user_agent TEXT,

     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                                                                                                                                                                                                                                 
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

AuditLogService:
@Service                                                                                                                                                                                                                                                                           
public class AuditLogService {
// 로그 기록                                                                                                                                                                                                                                                                   
public void log(AuditLogCreateRequest request) {
AuditLog log = AuditLog.builder()
.userId(request.getUserId())
.action(request.getAction())
.entityType(request.getEntityType())
.entityId(request.getEntityId())
.description(request.getDescription())
.metadata(request.getMetadata())
.build();

         auditLogRepository.save(log);
     }

     // 조회 (페이징, 필터링)                                                                                                                                                                                                                                                       
     public PageResponse<AuditLogResponse> getLogs(AuditLogSearchCondition condition) {
         // action, entityType, userId, dateRange 필터링                                                                                                                                                                                                                            
     }
}

MyBatis XML (mapper/auditlog/AuditLogMapper.xml):
<mapper namespace="io.moer.booking.domain.auditlog.repository.AuditLogRepository">                                                                                                                                                                                                 
<resultMap id="auditLogResultMap" type="AuditLog">                                                                                                                                                                                                                             
<id property="id" column="id"/>                                                                                                                                                                                                                                            
<result property="userRole" column="user_role"                                                                                                                                                                                                                             
typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>                                                                                                                                                                                                             
<result property="metadata" column="metadata"                                                                                                                                                                                                                              
typeHandler="io.moer.booking.common.mybatis.JsonTypeHandler"/>                                                                                                                                                                                                     
</resultMap>

     <insert id="save">                                                                                                                                                                                                                                                             
         INSERT INTO audit_logs (user_id, user_email, user_role, action, entity_type, entity_id,
                                 description, metadata, ip_address, user_agent)
         VALUES (#{userId}, #{userEmail}, #{userRole}::varchar, #{action}, #{entityType}, #{entityId},
                 #{description}, #{metadata, typeHandler=io.moer.booking.common.mybatis.JsonTypeHandler}::jsonb,
                 #{ipAddress}, #{userAgent})
     </insert>                                                                                                                                                                                                                                                                      

     <select id="findByCondition" resultMap="auditLogResultMap">                                                                                                                                                                                                                    
         SELECT * FROM audit_logs
         <where>                                                                                                                                                                                                                                                                    
             <if test="userId != null">                                                                                                                                                                                                                                             
                 AND user_id = #{userId}
             </if>                                                                                                                                                                                                                                                                  
             <if test="action != null">                                                                                                                                                                                                                                             
                 AND action = #{action}
             </if>                                                                                                                                                                                                                                                                  
             <if test="entityType != null">                                                                                                                                                                                                                                         
                 AND entity_type = #{entityType}
             </if>                                                                                                                                                                                                                                                                  
             <if test="startDate != null">                                                                                                                                                                                                                                          
                 AND created_at >= #{startDate}
             </if>                                                                                                                                                                                                                                                                  
             <if test="endDate != null">                                                                                                                                                                                                                                            
                 AND created_at &lt;= #{endDate}
             </if>                                                                                                                                                                                                                                                                  
         </where>                                                                                                                                                                                                                                                                   
         ORDER BY created_at DESC
         LIMIT #{size} OFFSET #{offset}
     </select>                                                                                                                                                                                                                                                                      
 </mapper>                                                                                                                                                                                                                                                                          

1.4 슈퍼 관리자 전용 도메인 구현

새 도메인: domain/superadmin/

파일 구조:
domain/superadmin/
├── controller/
│   ├── SuperAdminBusinessController.java     # 전체 매장 관리
│   ├── SuperAdminUserController.java         # 전체 사용자 관리
│   └── SuperAdminDashboardController.java    # 시스템 통계
├── dto/
│   ├── SystemStats.java                      # 시스템 전체 통계
│   ├── BusinessListResponse.java
│   ├── UserManagementRequest.java
│   ├── ChangeRoleRequest.java
│   └── BusinessRevenueRank.java
└── service/
├── SuperAdminBusinessService.java
├── SuperAdminUserService.java
└── SuperAdminDashboardService.java

SuperAdminBusinessController (전체 매장 관리):
@RestController                                                                                                                                                                                                                                                                    
@RequestMapping("/api/superadmin/businesses")                                                                                                                                                                                                                                      
@RequiredArgsConstructor                                                                                                                                                                                                                                                           
public class SuperAdminBusinessController {

     private final SuperAdminBusinessService superAdminBusinessService;

     // 전체 매장 조회 (페이징, 필터링)                                                                                                                                                                                                                                             
     @GetMapping                                                                                                                                                                                                                                                                    
     public ResponseEntity<ApiResponse<PageResponse<BusinessResponse>>> getAllBusinesses(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @ModelAttribute BusinessSearchCondition condition,
             @RequestParam(defaultValue = "1") int page,
             @RequestParam(defaultValue = "20") int size) {

         // 권한 체크                                                                                                                                                                                                                                                               
         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         PageResponse<BusinessResponse> response =
             superAdminBusinessService.getAllBusinesses(condition, page, size);

         return ResponseEntity.ok(ApiResponse.success(response));
     }

     // 매장 강제 삭제 (소프트 or 하드)                                                                                                                                                                                                                                             
     @DeleteMapping("/{id}")                                                                                                                                                                                                                                                        
     public ResponseEntity<ApiResponse<Void>> deleteBusiness(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @PathVariable Long id,
             @RequestParam(defaultValue = "false") boolean hard) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         superAdminBusinessService.forceDeleteBusiness(id, hard, currentUser.getUser());

         return ResponseEntity.ok(ApiResponse.success());
     }

     // 매장 상태 일괄 변경                                                                                                                                                                                                                                                         
     @PatchMapping("/bulk-status")                                                                                                                                                                                                                                                  
     public ResponseEntity<ApiResponse<Void>> bulkUpdateStatus(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @RequestBody BulkStatusUpdateRequest request) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         superAdminBusinessService.bulkUpdateStatus(request, currentUser.getUser());

         return ResponseEntity.ok(ApiResponse.success());
     }
}

SuperAdminUserController (전체 사용자 관리):
@RestController                                                                                                                                                                                                                                                                    
@RequestMapping("/api/superadmin/users")                                                                                                                                                                                                                                           
@RequiredArgsConstructor                                                                                                                                                                                                                                                           
public class SuperAdminUserController {

     private final SuperAdminUserService superAdminUserService;

     // 전체 사용자 조회                                                                                                                                                                                                                                                            
     @GetMapping                                                                                                                                                                                                                                                                    
     public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @ModelAttribute UserSearchCondition condition,
             @RequestParam(defaultValue = "1") int page,
             @RequestParam(defaultValue = "20") int size) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         PageResponse<UserResponse> response =
             superAdminUserService.getAllUsers(condition, page, size);

         return ResponseEntity.ok(ApiResponse.success(response));
     }

     // 사용자 역할 변경                                                                                                                                                                                                                                                            
     @PatchMapping("/{id}/role")                                                                                                                                                                                                                                                    
     public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @PathVariable Long id,
             @RequestBody ChangeRoleRequest request) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         UserResponse response = superAdminUserService.changeUserRole(
             id, request.getRole(), currentUser.getUser());

         return ResponseEntity.ok(ApiResponse.success(response));
     }

     // 사용자 강제 정지                                                                                                                                                                                                                                                            
     @PatchMapping("/{id}/suspend")                                                                                                                                                                                                                                                 
     public ResponseEntity<ApiResponse<UserResponse>> suspendUser(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @PathVariable Long id) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         UserResponse response = superAdminUserService.suspendUser(id, currentUser.getUser());

         return ResponseEntity.ok(ApiResponse.success(response));
     }

     // 사용자 강제 삭제                                                                                                                                                                                                                                                            
     @DeleteMapping("/{id}")                                                                                                                                                                                                                                                        
     public ResponseEntity<ApiResponse<Void>> deleteUser(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @PathVariable Long id) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         superAdminUserService.forceDeleteUser(id, currentUser.getUser());

         return ResponseEntity.ok(ApiResponse.success());
     }
}

SuperAdminDashboardController (시스템 통계):
@RestController                                                                                                                                                                                                                                                                    
@RequestMapping("/api/superadmin/dashboard")                                                                                                                                                                                                                                       
@RequiredArgsConstructor                                                                                                                                                                                                                                                           
public class SuperAdminDashboardController {

     private final SuperAdminDashboardService dashboardService;

     // 시스템 전체 통계                                                                                                                                                                                                                                                            
     @GetMapping("/stats")                                                                                                                                                                                                                                                          
     public ResponseEntity<ApiResponse<SystemStats>> getSystemStats(
             @AuthenticationPrincipal CustomUserDetails currentUser) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         SystemStats stats = dashboardService.getSystemStats();

         return ResponseEntity.ok(ApiResponse.success(stats));
     }

     // 매장별 매출 랭킹                                                                                                                                                                                                                                                            
     @GetMapping("/business-ranking")                                                                                                                                                                                                                                               
     public ResponseEntity<ApiResponse<List<BusinessRevenueRank>>> getBusinessRanking(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         List<BusinessRevenueRank> ranking =
             dashboardService.getTopBusinesses(startDate, endDate);

         return ResponseEntity.ok(ApiResponse.success(ranking));
     }

     // 업종별 통계                                                                                                                                                                                                                                                                 
     @GetMapping("/stats-by-type")                                                                                                                                                                                                                                                  
     public ResponseEntity<ApiResponse<List<BusinessTypeStats>>> getStatsByType(
             @AuthenticationPrincipal CustomUserDetails currentUser) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         List<BusinessTypeStats> stats = dashboardService.getStatsByBusinessType();

         return ResponseEntity.ok(ApiResponse.success(stats));
     }
}

SuperAdminDashboardService (시스템 통계 계산):
@Service                                                                                                                                                                                                                                                                           
@Transactional(readOnly = true)                                                                                                                                                                                                                                                    
@RequiredArgsConstructor                                                                                                                                                                                                                                                           
public class SuperAdminDashboardService {

     private final BusinessRepository businessRepository;
     private final UserRepository userRepository;
     private final ReservationRepository reservationRepository;

     public SystemStats getSystemStats() {
         LocalDate today = LocalDate.now();

         return SystemStats.builder()
             .totalBusinesses(businessRepository.countAll())
             .activeBusinesses(businessRepository.countByStatus(BusinessStatus.ACTIVE))
             .inactiveBusinesses(businessRepository.countByStatus(BusinessStatus.INACTIVE))
             .suspendedBusinesses(businessRepository.countByStatus(BusinessStatus.SUSPENDED))

             .totalUsers(userRepository.countAll())
             .superAdminCount(userRepository.countByRole(UserRole.SUPER_ADMIN))
             .adminCount(userRepository.countByRole(UserRole.ADMIN))
             .ownerCount(userRepository.countByRole(UserRole.OWNER))
             .staffCount(userRepository.countByRole(UserRole.STAFF))

             .totalReservationsToday(reservationRepository.countByDate(today))
             .totalRevenueToday(reservationRepository.sumTotalPriceByDate(today))
             .totalRevenueThisMonth(reservationRepository.sumTotalPriceByMonth(today))

             .newBusinessesThisMonth(businessRepository.countCreatedInMonth(today))
             .newUsersThisMonth(userRepository.countCreatedInMonth(today))

             .build();
     }

     public List<BusinessRevenueRank> getTopBusinesses(LocalDate startDate, LocalDate endDate) {
         // Repository에서 매장별 매출 합계를 계산하고 정렬                                                                                                                                                                                                                         
         return businessRepository.getRevenueRankingByDateRange(startDate, endDate);
     }

     public List<BusinessTypeStats> getStatsByBusinessType() {
         List<BusinessTypeStats> result = new ArrayList<>();

         for (BusinessType type : BusinessType.values()) {
             long count = businessRepository.countByType(type);
             BigDecimal revenue = reservationRepository.sumRevenueByBusinessType(type);

             result.add(BusinessTypeStats.builder()
                 .businessType(type)
                 .count(count)
                 .totalRevenue(revenue)
                 .build());
         }

         return result;
     }
}

DTOs:
// SystemStats.java                                                                                                                                                                                                                                                                
@Getter @Builder                                                                                                                                                                                                                                                                   
public class SystemStats {
// Business 통계                                                                                                                                                                                                                                                               
private Long totalBusinesses;
private Long activeBusinesses;
private Long inactiveBusinesses;
private Long suspendedBusinesses;

     // User 통계                                                                                                                                                                                                                                                                   
     private Long totalUsers;
     private Long superAdminCount;
     private Long adminCount;
     private Long ownerCount;
     private Long staffCount;

     // Reservation 통계                                                                                                                                                                                                                                                            
     private Long totalReservationsToday;
     private BigDecimal totalRevenueToday;
     private BigDecimal totalRevenueThisMonth;

     // 성장 지표                                                                                                                                                                                                                                                                   
     private Long newBusinessesThisMonth;
     private Long newUsersThisMonth;
}

// BusinessRevenueRank.java                                                                                                                                                                                                                                                        
@Getter @Builder                                                                                                                                                                                                                                                                   
public class BusinessRevenueRank {
private Long businessId;
private String businessName;
private String ownerName;
private BigDecimal totalRevenue;
private Long reservationCount;
private Integer rank;
}

// BusinessTypeStats.java                                                                                                                                                                                                                                                          
@Getter @Builder                                                                                                                                                                                                                                                                   
public class BusinessTypeStats {
private BusinessType businessType;
private Long count;
private BigDecimal totalRevenue;
}

// ChangeRoleRequest.java                                                                                                                                                                                                                                                          
@Getter @Setter                                                                                                                                                                                                                                                                    
public class ChangeRoleRequest {
@NotNull(message = "역할을 선택해주세요")                                                                                                                                                                                                                                      
private UserRole role;
}

// BulkStatusUpdateRequest.java                                                                                                                                                                                                                                                    
@Getter @Setter                                                                                                                                                                                                                                                                    
public class BulkStatusUpdateRequest {
@NotNull                                                                                                                                                                                                                                                                       
private List<Long> businessIds;

     @NotNull                                                                                                                                                                                                                                                                       
     private BusinessStatus status;
}

Repository 확장 (필요한 쿼리 메서드 추가):

BusinessRepository.java:
long countAll();
long countByStatus(BusinessStatus status);
long countByType(BusinessType type);
long countCreatedInMonth(LocalDate date);
List<BusinessRevenueRank> getRevenueRankingByDateRange(LocalDate start, LocalDate end);

UserRepository.java:
long countAll();
long countByRole(UserRole role);
long countCreatedInMonth(LocalDate date);

ReservationRepository.java:
long countByDate(LocalDate date);
BigDecimal sumTotalPriceByDate(LocalDate date);
BigDecimal sumTotalPriceByMonth(LocalDate date);
BigDecimal sumRevenueByBusinessType(BusinessType type);

 ---
Phase 2: 고급 관리 기능 (9일)

2.1 사용자 지원 시스템 (Support Ticket)

새 도메인: domain/support/

파일 구조:
domain/support/
├── SupportTicket.java                # Entity
├── SupportTicketStatus.java          # Enum
├── SupportTicketPriority.java        # Enum
├── controller/
│   ├── SupportTicketController.java           # 일반 사용자용
│   └── SuperAdminSupportController.java       # 슈퍼 관리자용
├── dto/
│   ├── SupportTicketResponse.java
│   ├── SupportTicketCreateRequest.java
│   └── SupportTicketSearchCondition.java
├── repository/
│   └── SupportTicketRepository.java
└── service/
└── SupportTicketService.java

SupportTicket Entity:
@Getter @Builder                                                                                                                                                                                                                                                                   
public class SupportTicket {
private Long id;
private Long businessId;
private Long userId;
private String title;
private String content;
private SupportTicketStatus status;        // OPEN, IN_PROGRESS, RESOLVED, CLOSED                                                                                                                                                                                              
private SupportTicketPriority priority;    // LOW, MEDIUM, HIGH, URGENT                                                                                                                                                                                                        
private Long assignedToUserId;             // 담당 SUPER_ADMIN                                                                                                                                                                                                                 
private String response;
private LocalDateTime resolvedAt;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
}

Enums:
public enum SupportTicketStatus {
OPEN("접수"),
IN_PROGRESS("처리중"),
RESOLVED("해결"),
CLOSED("종료");
}

public enum SupportTicketPriority {
LOW("낮음"),
MEDIUM("보통"),
HIGH("높음"),
URGENT("긴급");
}

DB 스키마:
CREATE TABLE support_tickets (
id BIGSERIAL PRIMARY KEY,
business_id BIGINT NOT NULL,
user_id BIGINT NOT NULL,
title VARCHAR(200) NOT NULL,
content TEXT NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
assigned_to_user_id BIGINT,
response TEXT,
resolved_at TIMESTAMP,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

     FOREIGN KEY (business_id) REFERENCES businesses(id),
     FOREIGN KEY (user_id) REFERENCES users(id),
     FOREIGN KEY (assigned_to_user_id) REFERENCES users(id)
);

CREATE INDEX idx_support_tickets_business_id ON support_tickets(business_id);
CREATE INDEX idx_support_tickets_status ON support_tickets(status);
CREATE INDEX idx_support_tickets_assigned_to ON support_tickets(assigned_to_user_id);

SupportTicketController (일반 사용자):
@RestController                                                                                                                                                                                                                                                                    
@RequestMapping("/api/businesses/{businessId}/support/tickets")                                                                                                                                                                                                                    
public class SupportTicketController {

     // 티켓 생성                                                                                                                                                                                                                                                                   
     @PostMapping                                                                                                                                                                                                                                                                   
     public ResponseEntity<ApiResponse<SupportTicketResponse>> createTicket(
             @PathVariable Long businessId,
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @Valid @RequestBody SupportTicketCreateRequest request) {

         // 권한 체크 (해당 매장 소속 사용자만)                                                                                                                                                                                                                                     
         if (!currentUser.canAccessBusiness(businessId)) {
             throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
         }

         SupportTicketResponse response = supportTicketService.createTicket(
             businessId, currentUser.getUserId(), request);

         return ResponseEntity.status(HttpStatus.CREATED)
             .body(ApiResponse.success(response));
     }

     // 내 티켓 목록 조회                                                                                                                                                                                                                                                           
     @GetMapping                                                                                                                                                                                                                                                                    
     public ResponseEntity<ApiResponse<PageResponse<SupportTicketResponse>>> getMyTickets(
             @PathVariable Long businessId,
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @RequestParam(defaultValue = "1") int page,
             @RequestParam(defaultValue = "20") int size) {

         if (!currentUser.canAccessBusiness(businessId)) {
             throw new BusinessException(ErrorCode.BUSINESS_ACCESS_DENIED);
         }

         PageResponse<SupportTicketResponse> response =
             supportTicketService.getTicketsByBusiness(businessId, page, size);

         return ResponseEntity.ok(ApiResponse.success(response));
     }
}

SuperAdminSupportController (슈퍼 관리자):
@RestController                                                                                                                                                                                                                                                                    
@RequestMapping("/api/superadmin/support/tickets")                                                                                                                                                                                                                                 
public class SuperAdminSupportController {

     // 전체 티켓 조회                                                                                                                                                                                                                                                              
     @GetMapping                                                                                                                                                                                                                                                                    
     public ResponseEntity<ApiResponse<PageResponse<SupportTicketResponse>>> getAllTickets(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @ModelAttribute SupportTicketSearchCondition condition,
             @RequestParam(defaultValue = "1") int page,
             @RequestParam(defaultValue = "20") int size) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         PageResponse<SupportTicketResponse> response =
             supportTicketService.getAllTickets(condition, page, size);

         return ResponseEntity.ok(ApiResponse.success(response));
     }

     // 티켓 할당                                                                                                                                                                                                                                                                   
     @PatchMapping("/{id}/assign")                                                                                                                                                                                                                                                  
     public ResponseEntity<ApiResponse<SupportTicketResponse>> assignTicket(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @PathVariable Long id,
             @RequestBody AssignTicketRequest request) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         SupportTicketResponse response = supportTicketService.assignTicket(
             id, request.getAssignedToUserId(), currentUser.getUserId());

         return ResponseEntity.ok(ApiResponse.success(response));
     }

     // 티켓 답변 및 해결                                                                                                                                                                                                                                                           
     @PatchMapping("/{id}/resolve")                                                                                                                                                                                                                                                 
     public ResponseEntity<ApiResponse<SupportTicketResponse>> resolveTicket(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @PathVariable Long id,
             @RequestBody ResolveTicketRequest request) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         SupportTicketResponse response = supportTicketService.resolveTicket(
             id, request.getResponse(), currentUser.getUserId());

         return ResponseEntity.ok(ApiResponse.success(response));
     }
}

2.2 보고서 생성 (Report)

새 도메인: domain/report/

파일 구조:
domain/report/
├── Report.java                       # Entity
├── ReportType.java                   # Enum
├── ReportStatus.java                 # Enum
├── controller/
│   └── SuperAdminReportController.java
├── dto/
│   ├── ReportResponse.java
│   ├── ReportGenerateRequest.java
│   └── ReportSearchCondition.java
├── repository/
│   └── ReportRepository.java
└── service/
├── ReportService.java
└── ReportGeneratorService.java   # 보고서 생성 로직

Report Entity:
@Getter @Builder                                                                                                                                                                                                                                                                   
public class Report {
private Long id;
private String title;
private ReportType reportType;
private ReportStatus status;           // PENDING, GENERATING, COMPLETED, FAILED                                                                                                                                                                                               
private Long generatedByUserId;
private LocalDate startDate;
private LocalDate endDate;
private String fileUrl;                // S3 or 로컬 경로                                                                                                                                                                                                                      
private String fileFormat;             // PDF, EXCEL, CSV                                                                                                                                                                                                                      
private Long fileSizeBytes;
private Map<String, Object> metadata;  // JSONB - 보고서 파라미터                                                                                                                                                                                                              
private String errorMessage;           // 생성 실패 시                                                                                                                                                                                                                         
private LocalDateTime generatedAt;
private LocalDateTime createdAt;
}

Enums:
public enum ReportType {
MONTHLY_SUMMARY("월간 요약 보고서"),
BUSINESS_PERFORMANCE("매장별 성과 보고서"),
USER_ACTIVITY("사용자 활동 보고서"),
REVENUE_ANALYSIS("매출 분석 보고서"),
SYSTEM_HEALTH("시스템 상태 보고서");
}

public enum ReportStatus {
PENDING("대기중"),
GENERATING("생성중"),
COMPLETED("완료"),
FAILED("실패");
}

DB 스키마:
CREATE TABLE reports (
id BIGSERIAL PRIMARY KEY,
title VARCHAR(200) NOT NULL,
report_type VARCHAR(50) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
generated_by_user_id BIGINT NOT NULL,
start_date DATE,
end_date DATE,
file_url TEXT,
file_format VARCHAR(10),
file_size_bytes BIGINT,
metadata JSONB,
error_message TEXT,
generated_at TIMESTAMP,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

     FOREIGN KEY (generated_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_reports_type ON reports(report_type);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_user_id ON reports(generated_by_user_id);

SuperAdminReportController:
@RestController                                                                                                                                                                                                                                                                    
@RequestMapping("/api/superadmin/reports")                                                                                                                                                                                                                                         
public class SuperAdminReportController {

     private final ReportService reportService;

     // 보고서 생성 요청 (비동기)                                                                                                                                                                                                                                                   
     @PostMapping("/generate")                                                                                                                                                                                                                                                      
     public ResponseEntity<ApiResponse<ReportResponse>> generateReport(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @Valid @RequestBody ReportGenerateRequest request) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         ReportResponse response = reportService.requestReportGeneration(
             request, currentUser.getUserId());

         return ResponseEntity.status(HttpStatus.CREATED)
             .body(ApiResponse.success(response));
     }

     // 보고서 목록 조회                                                                                                                                                                                                                                                            
     @GetMapping                                                                                                                                                                                                                                                                    
     public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> getReports(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @ModelAttribute ReportSearchCondition condition,
             @RequestParam(defaultValue = "1") int page,
             @RequestParam(defaultValue = "20") int size) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         PageResponse<ReportResponse> response =
             reportService.getReports(condition, page, size);

         return ResponseEntity.ok(ApiResponse.success(response));
     }

     // 보고서 다운로드                                                                                                                                                                                                                                                             
     @GetMapping("/{id}/download")                                                                                                                                                                                                                                                  
     public ResponseEntity<byte[]> downloadReport(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @PathVariable Long id) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         ReportDownloadInfo info = reportService.getReportFile(id);

         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
         headers.setContentDispositionFormData("attachment", info.getFileName());

         return ResponseEntity.ok()
             .headers(headers)
             .body(info.getFileData());
     }
}

ReportService (비동기 생성):
@Service                                                                                                                                                                                                                                                                           
public class ReportService {

     private final ReportGeneratorService generatorService;
     private final TaskExecutor taskExecutor;  // Spring Async                                                                                                                                                                                                                      

     @Transactional                                                                                                                                                                                                                                                                 
     public ReportResponse requestReportGeneration(ReportGenerateRequest request, Long userId) {
         // Report 엔티티 생성 (status = PENDING)                                                                                                                                                                                                                                   
         Report report = Report.builder()
             .title(request.getTitle())
             .reportType(request.getReportType())
             .status(ReportStatus.PENDING)
             .generatedByUserId(userId)
             .startDate(request.getStartDate())
             .endDate(request.getEndDate())
             .fileFormat(request.getFileFormat())
             .metadata(request.getMetadata())
             .build();

         reportRepository.save(report);

         // 비동기 생성 시작                                                                                                                                                                                                                                                        
         taskExecutor.execute(() -> generatorService.generateReport(report.getId()));

         return ReportResponse.from(report);
     }
}

@Service                                                                                                                                                                                                                                                                           
public class ReportGeneratorService {

     public void generateReport(Long reportId) {
         try {
             // 상태를 GENERATING으로 변경                                                                                                                                                                                                                                          
             reportRepository.updateStatus(reportId, ReportStatus.GENERATING);

             Report report = reportRepository.findById(reportId).orElseThrow();

             // 보고서 타입에 따라 데이터 수집 및 생성                                                                                                                                                                                                                              
             byte[] fileData = switch (report.getReportType()) {
                 case MONTHLY_SUMMARY -> generateMonthlySummary(report);
                 case BUSINESS_PERFORMANCE -> generateBusinessPerformance(report);
                 case REVENUE_ANALYSIS -> generateRevenueAnalysis(report);
                 // ...                                                                                                                                                                                                                                                             
             };

             // 파일 저장 (로컬 or S3)                                                                                                                                                                                                                                              
             String fileUrl = saveReportFile(report, fileData);

             // 상태 업데이트                                                                                                                                                                                                                                                       
             reportRepository.updateCompleted(reportId, fileUrl, fileData.length);

             // 이메일 발송 (선택사항)                                                                                                                                                                                                                                              
             // emailService.sendReportCompletedEmail(report);                                                                                                                                                                                                                      

         } catch (Exception e) {
             log.error("보고서 생성 실패: reportId={}", reportId, e);
             reportRepository.updateFailed(reportId, e.getMessage());
         }
     }

     private byte[] generateMonthlySummary(Report report) {
         // Apache POI (Excel) or iText (PDF) 사용하여 생성                                                                                                                                                                                                                         
     }
}

2.3 시스템 설정 관리 (System Config)

새 도메인: domain/systemconfig/

파일 구조:
domain/systemconfig/
├── SystemConfig.java                 # Entity
├── ConfigCategory.java               # Enum
├── controller/
│   └── SuperAdminSystemConfigController.java
├── dto/
│   ├── SystemConfigResponse.java
│   └── SystemConfigUpdateRequest.java
├── repository/
│   └── SystemConfigRepository.java
└── service/
└── SystemConfigService.java

SystemConfig Entity:
@Getter @Builder                                                                                                                                                                                                                                                                   
public class SystemConfig {
private Long id;
private String configKey;          // UNIQUE (예: TRIAL_PERIOD_DAYS)                                                                                                                                                                                                           
private String configValue;
private String description;
private ConfigCategory category;   // SYSTEM, SECURITY, NOTIFICATION, BUSINESS                                                                                                                                                                                                 
private String dataType;           // STRING, INTEGER, BOOLEAN, JSON                                                                                                                                                                                                           
private LocalDateTime updatedAt;
private Long updatedBy;            // 수정한 SUPER_ADMIN                                                                                                                                                                                                                       
}

ConfigCategory Enum:
public enum ConfigCategory {
SYSTEM("시스템"),
SECURITY("보안"),
NOTIFICATION("알림"),
BUSINESS("비즈니스");
}

DB 스키마:
CREATE TABLE system_configs (
id BIGSERIAL PRIMARY KEY,
config_key VARCHAR(100) NOT NULL UNIQUE,
config_value TEXT NOT NULL,
description TEXT,
category VARCHAR(50) NOT NULL,
data_type VARCHAR(20) NOT NULL,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_by BIGINT,

     FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- 초기 설정 데이터                                                                                                                                                                                                                                                                
INSERT INTO system_configs (config_key, config_value, description, category, data_type) VALUES                                                                                                                                                                                     
('TRIAL_PERIOD_DAYS', '30', '체험판 기간 (일)', 'BUSINESS', 'INTEGER'),
('MAX_STAFF_PER_BUSINESS', '50', '매장당 최대 직원 수', 'BUSINESS', 'INTEGER'),
('MAINTENANCE_MODE', 'N', '점검 모드 활성화', 'SYSTEM', 'BOOLEAN'),
('EMAIL_NOTIFICATION_ENABLED', 'Y', '이메일 알림 활성화', 'NOTIFICATION', 'BOOLEAN'),
('MAX_LOGIN_ATTEMPTS', '5', '최대 로그인 시도 횟수', 'SECURITY', 'INTEGER'),
('SESSION_TIMEOUT_MINUTES', '60', '세션 타임아웃 (분)', 'SECURITY', 'INTEGER');

SuperAdminSystemConfigController:
@RestController                                                                                                                                                                                                                                                                    
@RequestMapping("/api/superadmin/system-configs")                                                                                                                                                                                                                                  
public class SuperAdminSystemConfigController {

     // 모든 설정 조회                                                                                                                                                                                                                                                              
     @GetMapping                                                                                                                                                                                                                                                                    
     public ResponseEntity<ApiResponse<List<SystemConfigResponse>>> getAllConfigs(
             @AuthenticationPrincipal CustomUserDetails currentUser) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         List<SystemConfigResponse> configs = systemConfigService.getAllConfigs();

         return ResponseEntity.ok(ApiResponse.success(configs));
     }

     // 설정 수정                                                                                                                                                                                                                                                                   
     @PatchMapping("/{id}")                                                                                                                                                                                                                                                         
     public ResponseEntity<ApiResponse<SystemConfigResponse>> updateConfig(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @PathVariable Long id,
             @Valid @RequestBody SystemConfigUpdateRequest request) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         SystemConfigResponse response = systemConfigService.updateConfig(
             id, request.getConfigValue(), currentUser.getUserId());

         return ResponseEntity.ok(ApiResponse.success(response));
     }
}

 ---
Phase 3: 데이터 관리 기능 (8일)

3.1 데이터 백업 및 복원

새 도메인: domain/backup/

파일 구조:
domain/backup/
├── Backup.java                       # Entity
├── BackupStatus.java                 # Enum
├── controller/
│   └── SuperAdminBackupController.java
├── dto/
│   ├── BackupResponse.java
│   ├── RestoreConfirmation.java
│   └── BackupInfo.java
├── repository/
│   └── BackupRepository.java
└── service/
└── BackupService.java

Backup Entity:
@Getter @Builder                                                                                                                                                                                                                                                                   
public class Backup {
private Long id;
private String backupName;
private BackupStatus status;       // PENDING, IN_PROGRESS, COMPLETED, FAILED                                                                                                                                                                                                  
private Long createdByUserId;
private String fileUrl;
private Long fileSizeBytes;
private String backupType;         // FULL, INCREMENTAL                                                                                                                                                                                                                        
private LocalDateTime startedAt;
private LocalDateTime completedAt;
private String errorMessage;
private LocalDateTime createdAt;
}

BackupStatus Enum:
public enum BackupStatus {
PENDING("대기중"),
IN_PROGRESS("진행중"),
COMPLETED("완료"),
FAILED("실패");
}

DB 스키마:
CREATE TABLE backups (
id BIGSERIAL PRIMARY KEY,
backup_name VARCHAR(200) NOT NULL,
status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
created_by_user_id BIGINT NOT NULL,
file_url TEXT,
file_size_bytes BIGINT,
backup_type VARCHAR(20) NOT NULL DEFAULT 'FULL',
started_at TIMESTAMP,
completed_at TIMESTAMP,
error_message TEXT,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

     FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

SuperAdminBackupController:
@RestController                                                                                                                                                                                                                                                                    
@RequestMapping("/api/superadmin/backup")                                                                                                                                                                                                                                          
public class SuperAdminBackupController {

     // 백업 생성 (비동기)                                                                                                                                                                                                                                                          
     @PostMapping("/create")                                                                                                                                                                                                                                                        
     public ResponseEntity<ApiResponse<BackupResponse>> createBackup(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @RequestBody CreateBackupRequest request) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         BackupResponse response = backupService.createBackup(
             request, currentUser.getUserId());

         return ResponseEntity.status(HttpStatus.CREATED)
             .body(ApiResponse.success(response));
     }

     // 백업 목록 조회                                                                                                                                                                                                                                                              
     @GetMapping                                                                                                                                                                                                                                                                    
     public ResponseEntity<ApiResponse<List<BackupResponse>>> listBackups(
             @AuthenticationPrincipal CustomUserDetails currentUser) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         List<BackupResponse> backups = backupService.listBackups();

         return ResponseEntity.ok(ApiResponse.success(backups));
     }

     // 백업 복원 (위험 - 이중 확인)                                                                                                                                                                                                                                                
     @PostMapping("/{id}/restore")                                                                                                                                                                                                                                                  
     public ResponseEntity<ApiResponse<Void>> restoreBackup(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @PathVariable Long id,
             @Valid @RequestBody RestoreConfirmation confirmation) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         // 확인 코드 검증                                                                                                                                                                                                                                                          
         if (!"RESTORE_CONFIRMED".equals(confirmation.getConfirmationCode())) {
             throw new BusinessException(
                 ErrorCode.INVALID_INPUT_VALUE,
                 "복원 확인 코드가 일치하지 않습니다");
         }

         backupService.restoreBackup(id, currentUser.getUserId());

         return ResponseEntity.ok(ApiResponse.success());
     }
}

BackupService (PostgreSQL pg_dump 사용):
@Service                                                                                                                                                                                                                                                                           
public class BackupService {

     @Value("${spring.datasource.url}")                                                                                                                                                                                                                                             
     private String dbUrl;

     @Value("${spring.datasource.username}")                                                                                                                                                                                                                                        
     private String dbUsername;

     @Value("${spring.datasource.password}")                                                                                                                                                                                                                                        
     private String dbPassword;

     @Value("${backup.storage.path}")                                                                                                                                                                                                                                               
     private String backupStoragePath;

     private final TaskExecutor taskExecutor;

     @Transactional                                                                                                                                                                                                                                                                 
     public BackupResponse createBackup(CreateBackupRequest request, Long userId) {
         Backup backup = Backup.builder()
             .backupName(request.getBackupName())
             .status(BackupStatus.PENDING)
             .createdByUserId(userId)
             .backupType(request.getBackupType())
             .build();

         backupRepository.save(backup);

         // 비동기 백업 실행                                                                                                                                                                                                                                                        
         taskExecutor.execute(() -> performBackup(backup.getId()));

         return BackupResponse.from(backup);
     }

     private void performBackup(Long backupId) {
         try {
             backupRepository.updateStatus(backupId, BackupStatus.IN_PROGRESS);

             Backup backup = backupRepository.findById(backupId).orElseThrow();

             // pg_dump 명령 실행                                                                                                                                                                                                                                                   
             String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
             String fileName = String.format("backup_%s_%s.sql", timestamp, backupId);
             String filePath = Paths.get(backupStoragePath, fileName).toString();

             ProcessBuilder processBuilder = new ProcessBuilder(
                 "pg_dump",
                 "-h", extractHost(dbUrl),
                 "-U", dbUsername,
                 "-F", "c",  // Custom format                                                                                                                                                                                                                                       
                 "-f", filePath,
                 extractDbName(dbUrl)
             );

             processBuilder.environment().put("PGPASSWORD", dbPassword);
             Process process = processBuilder.start();

             int exitCode = process.waitFor();

             if (exitCode == 0) {
                 File file = new File(filePath);
                 long fileSize = file.length();

                 backupRepository.updateCompleted(backupId, filePath, fileSize);
             } else {
                 String error = new String(process.getErrorStream().readAllBytes());
                 backupRepository.updateFailed(backupId, error);
             }

         } catch (Exception e) {
             log.error("백업 실패: backupId={}", backupId, e);
             backupRepository.updateFailed(backupId, e.getMessage());
         }
     }

     @Transactional                                                                                                                                                                                                                                                                 
     public void restoreBackup(Long backupId, Long userId) {
         Backup backup = backupRepository.findById(backupId)
             .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BACKUP_NOT_FOUND));

         if (backup.getStatus() != BackupStatus.COMPLETED) {
             throw new BusinessException(
                 ErrorCode.INVALID_BACKUP_STATUS,
                 "완료된 백업만 복원할 수 있습니다");
         }

         // 감사 로그 기록 (위험한 작업)                                                                                                                                                                                                                                            
         auditLogService.log(AuditLogCreateRequest.builder()
             .userId(userId)
             .action(AuditAction.SYSTEM_RESTORE.name())
             .entityType("Backup")
             .entityId(backupId)
             .description("데이터베이스 복원 실행")
             .build());

         // pg_restore 명령 실행 (별도 스레드 또는 동기)                                                                                                                                                                                                                            
         taskExecutor.execute(() -> performRestore(backup));
     }

     private void performRestore(Backup backup) {
         try {
             ProcessBuilder processBuilder = new ProcessBuilder(
                 "pg_restore",
                 "-h", extractHost(dbUrl),
                 "-U", dbUsername,
                 "-d", extractDbName(dbUrl),
                 "-c",  // Clean (drop) before restore
                 backup.getFileUrl()
             );

             processBuilder.environment().put("PGPASSWORD", dbPassword);
             Process process = processBuilder.start();

             int exitCode = process.waitFor();

             if (exitCode != 0) {
                 String error = new String(process.getErrorStream().readAllBytes());
                 log.error("복원 실패: {}", error);
             }

         } catch (Exception e) {
             log.error("복원 실패", e);
         }
     }
}

3.2 시스템 모니터링

새 도메인: domain/monitoring/

파일 구조:
domain/monitoring/
├── controller/
│   └── SuperAdminMonitoringController.java
├── dto/
│   ├── SystemMetrics.java
│   ├── ErrorLog.java
│   └── SlowQuery.java
└── service/
└── MonitoringService.java

SuperAdminMonitoringController:
@RestController                                                                                                                                                                                                                                                                    
@RequestMapping("/api/superadmin/monitoring")                                                                                                                                                                                                                                      
public class SuperAdminMonitoringController {

     // 시스템 메트릭                                                                                                                                                                                                                                                               
     @GetMapping("/metrics")                                                                                                                                                                                                                                                        
     public ResponseEntity<ApiResponse<SystemMetrics>> getSystemMetrics(
             @AuthenticationPrincipal CustomUserDetails currentUser) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         SystemMetrics metrics = monitoringService.getSystemMetrics();

         return ResponseEntity.ok(ApiResponse.success(metrics));
     }

     // 데이터베이스 연결 상태                                                                                                                                                                                                                                                      
     @GetMapping("/database")                                                                                                                                                                                                                                                       
     public ResponseEntity<ApiResponse<DatabaseStatus>> getDatabaseStatus(
             @AuthenticationPrincipal CustomUserDetails currentUser) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         DatabaseStatus status = monitoringService.getDatabaseStatus();

         return ResponseEntity.ok(ApiResponse.success(status));
     }

     // 느린 쿼리 조회                                                                                                                                                                                                                                                              
     @GetMapping("/slow-queries")                                                                                                                                                                                                                                                   
     public ResponseEntity<ApiResponse<List<SlowQuery>>> getSlowQueries(
             @AuthenticationPrincipal CustomUserDetails currentUser,
             @RequestParam(defaultValue = "1000") int thresholdMs) {

         if (!currentUser.isSuperAdmin()) {
             throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
         }

         List<SlowQuery> queries = monitoringService.getSlowQueries(thresholdMs);

         return ResponseEntity.ok(ApiResponse.success(queries));
     }
}

MonitoringService:
@Service
public class MonitoringService {

     private final DataSource dataSource;

     public SystemMetrics getSystemMetrics() {
         Runtime runtime = Runtime.getRuntime();

         return SystemMetrics.builder()
             .totalMemory(runtime.totalMemory())
             .freeMemory(runtime.freeMemory())
             .usedMemory(runtime.totalMemory() - runtime.freeMemory())
             .maxMemory(runtime.maxMemory())
             .availableProcessors(runtime.availableProcessors())
             .uptime(ManagementFactory.getRuntimeMXBean().getUptime())
             .build();
     }

     public DatabaseStatus getDatabaseStatus() {
         try (Connection conn = dataSource.getConnection()) {
             DatabaseMetaData metaData = conn.getMetaData();

             return DatabaseStatus.builder()
                 .connected(true)
                 .databaseProductName(metaData.getDatabaseProductName())
                 .databaseProductVersion(metaData.getDatabaseProductVersion())
                 .url(metaData.getURL())
                 .build();

         } catch (SQLException e) {
             return DatabaseStatus.builder()
                 .connected(false)
                 .errorMessage(e.getMessage())
                 .build();
         }
     }

     public List<SlowQuery> getSlowQueries(int thresholdMs) {
         // PostgreSQL pg_stat_statements 확장 사용
         String sql = """
             SELECT query, calls, mean_exec_time, total_exec_time
             FROM pg_stat_statements
             WHERE mean_exec_time > ?
             ORDER BY mean_exec_time DESC
             LIMIT 50
         """;

         // JDBC로 실행 후 DTO 매핑
     }
}

DTOs:
// SystemMetrics.java                                                                                                                                                                                                                                                              
@Getter @Builder                                                                                                                                                                                                                                                                   
public class SystemMetrics {
private Long totalMemory;
private Long freeMemory;
private Long usedMemory;
private Long maxMemory;
private Integer availableProcessors;
private Long uptime;  // milliseconds                                                                                                                                                                                                                                          
}

// DatabaseStatus.java                                                                                                                                                                                                                                                             
@Getter @Builder                                                                                                                                                                                                                                                                   
public class DatabaseStatus {
private Boolean connected;
private String databaseProductName;
private String databaseProductVersion;
private String url;
private String errorMessage;
}

// SlowQuery.java                                                                                                                                                                                                                                                                  
@Getter @Builder                                                                                                                                                                                                                                                                   
public class SlowQuery {
private String query;
private Long calls;
private Double meanExecTime;  // milliseconds                                                                                                                                                                                                                                  
private Double totalExecTime;
}

 ---
보안 및 제약사항

1. SUPER_ADMIN 계정 생성 제한

AuthService.register() 수정:
@Transactional                                                                                                                                                                                                                                                                     
public RegisterResponse register(RegisterRequest request) {
// SUPER_ADMIN은 일반 회원가입 불가                                                                                                                                                                                                                                            
if (request.getRole() == UserRole.SUPER_ADMIN) {
throw new BusinessException(
ErrorCode.SUPER_ADMIN_ONLY_ACTION,
"슈퍼 관리자는 일반 회원가입으로 생성할 수 없습니다");
}

     // 기존 로직...                                                                                                                                                                                                                                                                
}

최초 SUPER_ADMIN 생성 방법:

schema.sql에 초기 데이터 추가:
-- 최초 슈퍼 관리자 계정 (비밀번호: Admin123!)                                                                                                                                                                                                                                     
INSERT INTO users (email, password, name, role, status, email_verified, created_at)
VALUES (
'superadmin@moer.io',
'$2a$10$YourBCryptHashedPassword',  -- BCrypt 해시                                                                                                                                                                                                                             
'시스템 관리자',
'SUPER_ADMIN',
'ACTIVE',
'Y',
CURRENT_TIMESTAMP                                                                                                                                                                                                                                                              
);

2. Controller 권한 체크 패턴

모든 슈퍼 관리자 엔드포인트는 메서드 시작 시 권한 체크:
public ResponseEntity<ApiResponse<?>> someAction(
@AuthenticationPrincipal CustomUserDetails currentUser) {

     if (!currentUser.isSuperAdmin()) {
         throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
     }

     // 비즈니스 로직...                                                                                                                                                                                                                                                            
}

3. 중요 액션 감사 로그 자동 기록

Service 메서드에서 중요 액션 수행 시 감사 로그 기록:
@Service                                                                                                                                                                                                                                                                           
public class SuperAdminBusinessService {

     @Transactional                                                                                                                                                                                                                                                                 
     public void forceDeleteBusiness(Long id, boolean hard, User admin) {
         Business business = businessRepository.findById(id)
             .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

         // 감사 로그 기록                                                                                                                                                                                                                                                          
         auditLogService.log(AuditLogCreateRequest.builder()
             .userId(admin.getId())
             .userEmail(admin.getEmail())
             .userRole(admin.getRole())
             .action(AuditAction.BUSINESS_DELETED.name())
             .entityType("Business")
             .entityId(id)
             .description(String.format("매장 강제 삭제 (하드 삭제: %s)", hard))
             .metadata(Map.of(
                 "businessName", business.getName(),
                 "ownerId", business.getOwnerId(),
                 "hardDelete", hard
             ))
             .build());

         // 삭제 수행                                                                                                                                                                                                                                                               
         if (hard) {
             // 연관 데이터 모두 삭제                                                                                                                                                                                                                                               
             // ...                                                                                                                                                                                                                                                                 
         }
         businessRepository.delete(id);
     }
}

4. SUPER_ADMIN 삭제 방지

SuperAdminUserService.forceDeleteUser():
public void forceDeleteUser(Long userId, User admin) {
User targetUser = userRepository.findById(userId)
.orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

     // SUPER_ADMIN은 삭제 불가                                                                                                                                                                                                                                                     
     if (targetUser.isSuperAdmin()) {
         throw new BusinessException(
             ErrorCode.SUPER_ADMIN_CANNOT_BE_DELETED,
             "슈퍼 관리자 계정은 삭제할 수 없습니다");
     }

     // 감사 로그 + 삭제 수행                                                                                                                                                                                                                                                       
     // ...                                                                                                                                                                                                                                                                         
}

 ---
주요 파일 목록

Phase 1 - 수정 파일

- src/main/java/io/moer/booking/domain/user/UserRole.java
- src/main/java/io/moer/booking/domain/user/User.java
- src/main/java/io/moer/booking/common/security/CustomUserDetails.java
- src/main/java/io/moer/booking/common/exception/ErrorCode.java
- src/main/java/io/moer/booking/domain/business/service/BusinessService.java
- src/main/java/io/moer/booking/domain/business/controller/BusinessController.java
- src/main/java/io/moer/booking/domain/auth/service/AuthService.java
- src/main/resources/db/schema.sql

Phase 1 - 신규 도메인

- src/main/java/io/moer/booking/domain/auditlog/ (전체 신규)
    - AuditLog.java, AuditAction.java
    - controller/AuditLogController.java
    - dto/ (3개 DTO)
    - repository/AuditLogRepository.java
    - service/AuditLogService.java
- src/main/resources/mapper/auditlog/AuditLogMapper.xml
- src/main/java/io/moer/booking/domain/superadmin/ (전체 신규)
    - controller/ (3개 Controller)
    - dto/ (8개 DTO)
    - service/ (3개 Service)

Phase 1 - Repository 확장

- src/main/java/io/moer/booking/domain/business/repository/BusinessRepository.java
- src/main/java/io/moer/booking/domain/user/repository/UserRepository.java
- src/main/java/io/moer/booking/domain/reservation/repository/ReservationRepository.java
- src/main/resources/mapper/business/BusinessMapper.xml
- src/main/resources/mapper/user/UserMapper.xml
- src/main/resources/mapper/reservation/ReservationMapper.xml

Phase 2 - 신규 도메인

- src/main/java/io/moer/booking/domain/support/ (전체 신규)
- src/main/resources/mapper/support/SupportTicketMapper.xml
- src/main/java/io/moer/booking/domain/report/ (전체 신규)
- src/main/resources/mapper/report/ReportMapper.xml
- src/main/java/io/moer/booking/domain/systemconfig/ (전체 신규)
- src/main/resources/mapper/systemconfig/SystemConfigMapper.xml

Phase 3 - 신규 도메인

- src/main/java/io/moer/booking/domain/backup/ (전체 신규)
- src/main/resources/mapper/backup/BackupMapper.xml
- src/main/java/io/moer/booking/domain/monitoring/ (전체 신규)

 ---
검증 계획

1. 단위 테스트

- UserRole enum 테스트 (SUPER_ADMIN 추가 확인)
- User.canAccessBusiness() 로직 테스트
- SuperAdminBusinessService 테스트
- SuperAdminUserService 테스트
- AuditLogService 테스트

2. 통합 테스트

- 슈퍼 관리자 로그인 → JWT에 role=SUPER_ADMIN 포함 확인
- 전체 매장 조회 API 테스트
- 매장 강제 삭제 → 감사 로그 생성 확인
- 사용자 역할 변경 → 감사 로그 생성 확인
- OWNER가 슈퍼 관리자 API 호출 시 403 Forbidden 확인

3. E2E 시나리오

1. 최초 설정
- schema.sql 실행 → SUPER_ADMIN 계정 생성 확인
- 슈퍼 관리자로 로그인
2. 전체 업체 관리
- GET /api/superadmin/businesses → 전체 매장 목록 조회
- DELETE /api/superadmin/businesses/{id} → 매장 삭제
- GET /api/superadmin/audit-logs → 삭제 로그 확인
3. 사용자 관리
- GET /api/superadmin/users → 전체 사용자 조회
- PATCH /api/superadmin/users/{id}/role → OWNER를 ADMIN으로 변경
- 감사 로그에서 역할 변경 기록 확인
4. 시스템 통계
- GET /api/superadmin/dashboard/stats → 시스템 전체 통계 확인
- GET /api/superadmin/dashboard/business-ranking → 매출 랭킹 확인
5. 지원 티켓 (Phase 2)
- OWNER가 티켓 생성
- SUPER_ADMIN이 티켓 확인 및 답변
- 티켓 상태 변경 확인
6. 보고서 생성 (Phase 2)
- POST /api/superadmin/reports/generate → 보고서 생성 요청
- 비동기 생성 완료 대기
- GET /api/superadmin/reports/{id}/download → 다운로드
7. 백업 및 복원 (Phase 3)
- POST /api/superadmin/backup/create → 백업 생성
- 백업 파일 생성 확인
- POST /api/superadmin/backup/{id}/restore → 복원 (테스트 DB에서만)

4. 보안 테스트

- OWNER가 /api/superadmin/* 엔드포인트 호출 시 403 확인
- SUPER_ADMIN 회원가입 시도 시 에러 확인
- SUPER_ADMIN 계정 삭제 시도 시 에러 확인
- JWT 토큰 없이 API 호출 시 401 확인

5. 성능 테스트

- 전체 매장 1000개 조회 시 응답 시간 (<2초)
- 감사 로그 10000개 조회 시 응답 시간 (<3초)
- 시스템 통계 계산 시간 (<1초)

6. Swagger UI 테스트

- http://localhost:8080/swagger-ui.html 접속
- "SuperAdmin" 태그로 구분된 엔드포인트 확인
- Authorize 버튼으로 JWT 토큰 설정
- Try it out으로 각 API 테스트

 ---
예상 일정
┌────────────────────────────┬──────────────────────────────────────────────────────────────┬────────────────┐
│           Phase            │                          작업 내용                           │ 예상 소요 시간 │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ Phase 1                    │ UserRole 확장 + 권한 보안 강화 + 감사 로그 + 슈퍼 관리자 API │ 12일           │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ - UserRole 확장            │ SUPER_ADMIN enum 추가, User 로직 수정, ErrorCode             │ 1일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ - Business 권한 보안       │ BusinessService/Controller 권한 체크 추가                    │ 2일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ - 감사 로그 시스템         │ AuditLog 도메인 전체 구현                                    │ 3일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ - SuperAdmin Business API  │ 전체 매장 조회/수정/삭제 API                                 │ 2일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ - SuperAdmin User API      │ 전체 사용자 관리, 역할 변경 API                              │ 2일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ - SuperAdmin Dashboard API │ 시스템 통계, 랭킹, 업종별 통계                               │ 2일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ Phase 2                    │ Support Ticket + Report + System Config                      │ 9일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ - Support Ticket           │ 지원 티켓 시스템 전체                                        │ 3일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ - Report                   │ 보고서 생성 시스템 (비동기)                                  │ 4일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ - System Config            │ 시스템 설정 관리                                             │ 2일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ Phase 3                    │ Backup + Monitoring                                          │ 8일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ - Backup & Restore         │ 백업/복원 시스템 (pg_dump)                                   │ 5일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ - Monitoring               │ 시스템 모니터링, 느린 쿼리                                   │ 3일            │
├────────────────────────────┼──────────────────────────────────────────────────────────────┼────────────────┤
│ 총 계                      │                                                              │ 29일           │
└────────────────────────────┴──────────────────────────────────────────────────────────────┴────────────────┘
 ---
우선순위 정리

⭐ 필수 (Phase 1)

1. UserRole에 SUPER_ADMIN 추가
2. 기존 Business/User 권한 체크 보안 강화 (취약점 해결)
3. 감사 로그 시스템
4. 슈퍼 관리자 전용 API (매장 관리, 사용자 관리, 시스템 통계)

🔸 권장 (Phase 2)

1. 사용자 지원 시스템 (Support Ticket)
2. 보고서 생성 (Report)
3. 시스템 설정 관리 (System Config)

🔹 선택 (Phase 3)

1. 데이터 백업 및 복원
2. 시스템 모니터링

 ---
구현 순서

1. DB 스키마 업데이트 (schema.sql)
- audit_logs 테이블 추가
- 초기 SUPER_ADMIN 계정 INSERT
- (Phase 2/3) support_tickets, reports, system_configs, backups 테이블
2. UserRole 확장 및 보안 강화
- UserRole enum, User entity, CustomUserDetails
- ErrorCode 추가
- BusinessService/AuthService 권한 체크 추가
3. 감사 로그 도메인
- AuditLog entity, enum, repository, service
- MyBatis XML 작성
4. 슈퍼 관리자 도메인
- SuperAdminBusinessController/Service
- SuperAdminUserController/Service
- SuperAdminDashboardController/Service
5. Repository 확장
- BusinessRepository, UserRepository, ReservationRepository에 통계 쿼리 추가
- MyBatis XML 업데이트
6. (Phase 2) Support Ticket 도메인
7. (Phase 2) Report 도메인
8. (Phase 2) System Config 도메인
9. (Phase 3) Backup 도메인
10. (Phase 3) Monitoring 도메인
11. 통합 테스트 및 E2E 테스트
12. Swagger 문서 업데이트