# Task 1 Report: 菜品起售/停售功能

## Implementation

### Modified Files
1. **`src/main/java/com/reggie/service/DishService.java`** - Added `updateStatus(Integer status, List<Long> ids)` interface method and `import java.util.List`
2. **`src/main/java/com/reggie/service/impl/DishServiceImpl.java`** - Added `@Transactional` implementation using `LambdaUpdateWrapper` to batch update dish status; added `import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper`
3. **`src/main/java/com/reggie/controller/DishController.java`** - Added `POST /status/{status}` endpoint accepting `@PathVariable Integer status` and `@RequestParam List<Long> ids`
4. **`pom.xml`** - Added H2 database dependency for test scope

### Created Files
5. **`src/test/resources/schema.sql`** - H2-compatible DDL for `dish` and `category` tables (stripped MySQL-specific syntax: COLLATE, COMMENT, USING BTREE, ENGINE)
6. **`src/test/resources/application-test.yml`** - Test profile config with H2 in-memory datasource in MySQL mode
7. **`src/test/java/com/reggie/controller/DishControllerTest.java`** - Integration tests with `@DirtiesContext` for test isolation

## Test Results

- **Test run**: 2 tests, 0 failures, 0 errors
- `testUpdateStatus` - Posts to `/dish/status/0` with ids=1, verifies status code 200, JSON code=1, data="操作成功", and asserts dish status changed to 0
- `testUpdateStatusBatch` - Posts to `/dish/status/0` with ids=1,2, verifies both dishes have status=0

## Files Changed
| File | Action |
|------|--------|
| `src/main/java/com/reggie/service/DishService.java` | Modified |
| `src/main/java/com/reggie/service/impl/DishServiceImpl.java` | Modified |
| `src/main/java/com/reggie/controller/DishController.java` | Modified |
| `pom.xml` | Modified |
| `src/test/resources/schema.sql` | Created |
| `src/test/resources/application-test.yml` | Created |
| `src/test/java/com/reggie/controller/DishControllerTest.java` | Created |

## Self-Review Findings

1. **Missing `import java.util.List` in DishService.java** - The original task brief didn't include this import, causing compilation failure. Fixed.
2. **MySQL-specific SQL syntax in schema.sql** - H2 MySQL mode doesn't support `COLLATE`, `COMMENT`, `USING BTREE`, `ENGINE=InnoDB`. Stripped all MySQL-specific syntax for H2 compatibility.
3. **`R.success()` uses `data` field, not `msg`** - The test originally asserted `$.msg` but the `R.success()` helper stores the value in `data`. Changed test assertion to `$.data`.
4. **Test isolation** - `@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)` is required because H2 in-memory DB persists across tests; each test needs a fresh context/schema.

## Concerns
None. All tests pass.
