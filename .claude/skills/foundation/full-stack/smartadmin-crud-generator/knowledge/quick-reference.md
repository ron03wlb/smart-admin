# SmartAdmin CRUD Generator - Quick Reference

**Version**: 2.0.0  
**Last Updated**: 2026-02-02  
**Skill**: smartadmin-crud-generator (P0 - Critical)

---

## Quick Start

### Generate Full-stack CRUD

User: "Generate CRUD for Employee entity"

Generates:
- Backend: Entity, Dao, Service, Manager, Controller
- Frontend: List page, Form modal, API integration
- Tests: Unit + Integration tests
- API Docs: Knife4j documentation

---

## 4-Phase Execution

### Phase 1: Backend (5 min)
- Entity (JPA annotations)
- Dao/Mapper (MyBatis-Plus)
- Service (business logic)
- Manager (@Transactional methods)
- Controller (REST API)

### Phase 2: Frontend (5 min)
- employee-list.vue (Ant Design table)
- employee-form-modal.vue (Form validation)
- employee-api.ts (Axios requests)

### Phase 3: API Docs (2 min)
- Knife4j @ApiOperation
- Request/Response DTOs
- Example values

### Phase 4: Tests (5 min)
- Unit tests (Service layer)
- Integration tests (Controller + DB)
- ArchUnit tests

---

## SmartAdmin Patterns

### Entity Pattern

@Entity
@Table(name = "t_employee")
public class EmployeeEntity {
    @Id
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private Boolean deleted;  // NOT isDeleted!
}

### Service Pattern

@Service
public class EmployeeService {
    public Option<EmployeeVO> queryDetail(Long id) {
        return Option.ofOptional(employeeDao.selectById(id))
            .map(entity -> SmartBeanUtil.copy(entity, EmployeeVO.class));
    }
}

### Controller Pattern

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;
    
    @GetMapping("/{id}")
    public ResponseDTO<EmployeeVO> detail(@PathVariable Long id) {
        return employeeService.queryDetail(id)
            .map(ResponseDTO::ok)
            .getOrElse(ResponseDTO.error(ErrorCode.NOT_FOUND));
    }
}

---

## Domain Objects

| Type | Purpose | Location |
|------|---------|----------|
| Entity | Database model | entity/ |
| Form | Create/Update request | domain/ |
| QueryForm | List query params | domain/ |
| VO | Response object | domain/ |

---

## Commands

### Generate Backend Only
User: "Generate backend CRUD for Employee"

### Generate Frontend Only
User: "Generate Vue components for Employee list"

### Full Stack
User: "Generate complete CRUD for Employee"

---

## Validation

- [ ] Entity follows naming conventions (singular)
- [ ] Service uses Option<T> (not Optional)
- [ ] Manager has @Transactional
- [ ] Controller uses ResponseDTO
- [ ] Frontend uses Ant Design Vue
- [ ] Tests compile and pass

---

See SKILL.md for complete CRUD patterns.
