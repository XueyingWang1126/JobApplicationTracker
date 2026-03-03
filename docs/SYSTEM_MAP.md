# System Map

## Module Map

### Base Package

- `com.xueying.jobapplicationtracker`

### Controllers

- `RegisterAndLoginController`
- `PageController`
- `ApplicationController`
- `DocumentController`

### Services

- `UserService` / `UserServiceImpl`
- `ApplicationService` / `ApplicationServiceImpl`
- `DocumentService` / `DocumentServiceImpl`
- `CompanyAutoFillService` / `CompanyAutoFillServiceImpl`
- `MinIOService` / `MinIOServiceImpl`

### Persistence

- `UserMapper`
- `ApplicationMapper`
- `DocumentMapper`

### Models

- Entities: `User`, `ApplicationEntity`, `DocumentEntity`
- DTOs: `ApplicationDTO`, `ApplicationEditDTO`, `ApplicationQueryDTO`, `CompanyAutoFillResponse`
- VOs: `ApplicationVO`, `DocumentVO`

### Templates

- `login.html`
- `register.html`
- `dashboard.html`
- `applications.html`
- `application-detail.html`
- `documents.html`
- `error.html`

## Route Map

| URL | Method | Controller | Purpose |
| --- | --- | --- | --- |
| `/` | GET | `PageController` | Dashboard entry |
| `/dashboard` | GET | `PageController` | Summary page |
| `/login` | GET/POST | `RegisterAndLoginController` | Login |
| `/register` | GET/POST | `RegisterAndLoginController` | Register |
| `/logout` | GET | Shiro filter | Logout |
| `/applications` | GET | `ApplicationController` | List/manage applications |
| `/applications/create` | POST | `ApplicationController` | Create application |
| `/applications/{id}` | GET | `ApplicationController` | Detail page |
| `/applications/{id}/edit` | GET/POST | `ApplicationController` | Edit/update application |
| `/applications/{id}/delete` | POST | `ApplicationController` | Delete application |
| `/applications/autofill` | GET | `ApplicationController` | Company info auto-fill API |
| `/documents` | GET | `DocumentController` | Attachment overview |
| `/documents/{id}/download` | GET | `DocumentController` | Download attachment |
| `/documents/{id}/delete` | POST | `DocumentController` | Delete attachment |
| `/applications/{applicationId}/documents/{id}/download` | GET | `DocumentController` | Download in app context |
| `/applications/{applicationId}/documents/{id}/delete` | POST | `DocumentController` | Delete in app context |

