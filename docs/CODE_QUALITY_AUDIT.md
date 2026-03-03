# Code Quality Audit (2026-03-03)

## Audit Method

Commands used:

```powershell
rg -n --hidden --glob '!target/**' --glob '!.git/**' "TODO|FIXME|HACK|XXX" src docs README.md pom.xml run.ps1 docker-compose.yml
```

```powershell
rg -n --hidden --glob '!target/**' --glob '!.git/**' "listRecentUpdated|SwaggerConfig|CaptchaController|CaptchaService|MybatisPlusConfig|jsencrypt.min.js"
```

## Findings and Decisions

1. `src/main/java/com/xueying/jobapplicationtracker/config/MybatisPlusConfig.java`  
Decision: **Delete**  
Reason: empty config class, no runtime behavior.

2. `src/main/java/com/xueying/jobapplicationtracker/config/SwaggerConfig.java` + `springfox` dependency in `pom.xml`  
Decision: **Delete**  
Reason: API docs were not part of the target user journey and increased dependency surface.

3. `src/main/java/com/xueying/jobapplicationtracker/controller/CaptchaController.java` + `src/main/java/com/xueying/jobapplicationtracker/service/CaptchaService.java`  
Decision: **Delete**  
Reason: dead feature path (no page usage, no auth dependency).

4. `ApplicationService#listRecentUpdated` and implementation  
Decision: **Delete**  
Reason: not used by UI or tests.

5. `src/main/resources/application.properties`  
Decision: **Delete**  
Reason: duplicated server-port config; now unified in `application.yml`.

6. `src/main/resources/static/jsencrypt.min.js`  
Decision: **Delete**  
Reason: unused asset and triggered false-positive key-material scan noise.

7. `/documents/upload` flow  
Decision: **Delete page-level entry, keep Documents as overview**  
Reason: upload入口已并入 Application 创建/编辑流程，Documents 页面定位为辅助总览。

8. `MD5Util` utility  
Decision: **Keep**  
Reason: still used by `UserServiceImpl` for legacy password-hash compatibility.

## Comments Added

Added class-level comments for:
- Controllers
- Services and service implementations
- Mappers
- Entities

Added method-level comments for key business methods:
- Application create/update flow
- Document ownership checks in download/delete flow
- Login/register and password verification flow

## TODO / FIXME Sweep

- `0 matches` for `TODO|FIXME|HACK|XXX` in current source/docs/config scope.
