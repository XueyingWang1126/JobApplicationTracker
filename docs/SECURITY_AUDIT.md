# Security Audit (2026-03-03)

## Audit Scope

Repository root: `job-application-tracker`  
Goal: verify the project can be safely published to GitHub without private keys, tokens, or real credentials.

## 1) Residual Name / Internal Trace Scan

Command:

```powershell
rg -n --hidden -S --glob '!target/**' --glob '!.git/**' "(r[o]bot|R[o]botInfo|XSwi[t]ch|com\.xueying\.r[o]bot)"
```

Result:
- `0 matches`

## 2) Sensitive Keyword Scan (Required Coverage)

Reproducible command:

```powershell
$patterns=@(
  ('BEGIN' + ' PRIVATE' + ' KEY'),
  ('PRIVATE' + ' KEY'),
  ('AK' + 'IA'),
  'SECRET',
  'PASSWORD',
  ('TO' + 'KEN'),
  ('Bea' + 'rer'),
  'Authorization',
  ('ssh-' + 'rsa'),
  '\.env',
  'minio',
  'postgres',
  'jdbc:'
)
foreach($p in $patterns){
  rg -n --hidden -S --glob '!target/**' --glob '!.git/**' $p .
}
```

Result summary:
- Counts from current scan (`--glob '!docs/**'`):
  - key-material markers (private key / aws key prefix / token / bearer / ssh key): `0`
  - `SECRET`: `9` (env variable names/placeholders only)
  - `PASSWORD`: `7` (env variable names/placeholders only)
  - `Authorization`: `3` (framework naming only)
  - `\.env`: `18` (policy/documentation references)
  - `minio`: `107` (expected infra references)
  - `postgres`: `20` (expected infra references)
  - `jdbc:`: `3` (datasource/test references)
- private-key markers / AWS-style key prefix / token markers: no leak matches
- `SECRET` / `PASSWORD`: only environment-variable keys and placeholder values (`change_me_*`), no real secret
- `Authorization`: framework class name (`AuthorizationAttributeSourceAdvisor`) only
- `.env`: docs and ignore policy references only, `.env` file itself not retained in repo content
- `minio` / `postgres` / `jdbc:`: expected infrastructure and datasource configuration references

## 3) .env and Ignore Policy Verification

Command:

```powershell
git check-ignore -v .env .env.example run.log run.err target .vscode/settings.json
```

Observed:
- `.env` ignored by `.gitignore`
- `.env.example` explicitly allowed (`!.env.example`)
- `run.log` / `run.err` / `target` / `.vscode` ignored

Repository policy:
- Commit: `.env.example`
- Do not commit: `.env`, logs, dumps, IDE state, build outputs

Additional check:

```powershell
Get-ChildItem -Force .env* | Select-Object Name
```

Observed:
- only `.env.example` exists in working tree

## 4) Hardcoded Password Review

Checked files:
- `docker-compose.yml`
- `src/main/resources/application.yml`
- `run.ps1`
- `README.md`

Finding:
- No hardcoded real password
- Runtime credentials are required through environment variables
- Placeholder values are blocked by `run.ps1` startup validation (`change_me*` cannot pass)

## 5) Publish Gate Conclusion

Current status: **PASS (ready for public GitHub publication)**, with the following guardrails:
- Keep using `.env` locally only
- Replace all placeholder secrets before local startup
- Re-run the above scans before each public release
