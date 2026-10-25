# TrustVault Threat Model

## 1. Purpose and Scope

TrustVault is a document-management web application built with Java Spring Boot, Spring Security, PostgreSQL, HTML, CSS, and JavaScript. This document identifies relevant threats, describes controls to verify, and lists tests to run before evaluation.

**Status note:** A control is not verified merely because it is described here. Run the listed tests and record the actual result. Update this file when the implementation changes.

## 2. Assets to Protect

- User account details, including usernames and email addresses.
- Password hashes stored in the database.
- Uploaded documents and their metadata.
- User sessions and authentication state.
- Database credentials, deployment environment variables, and configuration.
- Application integrity and availability.

## 3. Trust Boundaries

1. **Browser to application:** Requests, cookies, forms, and uploaded files come from an untrusted client.
2. **Application to database:** The backend reads and writes account and document metadata.
3. **Application to file storage:** Uploaded file bytes are stored on the server filesystem.
4. **Deployment configuration:** Render environment variables and database credentials must remain private.

## 4. Threats, Controls, and Tests

| ID | Threat | Control to verify | Test to run |
|---|---|---|---|
| T1 | Unauthenticated user accesses private pages or APIs | Spring Security authentication rules | Open a protected page and call a protected API in a private/incognito window without logging in. Confirm access is denied or redirected to login. |
| T2 | User A reads, downloads, previews, or deletes User B's document | Backend document-ownership checks | Create two accounts. Upload a document as User B and attempt to access its identifier as User A. Confirm access is denied and the document remains unchanged. |
| T3 | Passwords are exposed if the database is accessed | BCrypt password hashing | Register a test account and inspect its database record. Confirm the stored value is a BCrypt hash, not the original password. |
| T4 | Malicious or unsupported file is uploaded | Extension, size, and content/signature validation | Try an unsupported extension, a file larger than the configured 10 MB limit, and a renamed file whose content does not match its extension. Confirm each is rejected. |
| T5 | Cross-Site Request Forgery (CSRF) causes an unwanted state change | Spring Security CSRF protection for applicable requests | Submit a state-changing request without a valid CSRF token. Confirm it is rejected, while legitimate frontend requests still work. |
| T6 | Repeated password guessing | Login-attempt restriction or temporary lockout | Use a dedicated test account and perform repeated failed logins. Confirm the configured lockout occurs and verify recovery without affecting other users. |
| T7 | A session remains usable after logout | Spring Security logout and session handling | Log in, log out, then revisit a protected page and retry a protected API with the old session. Confirm reauthentication is required. |
| T8 | Secrets leak through source control or logs | Environment variables and careful logging | Review tracked files and recent commits for passwords, API keys, and tokens. Check logs do not print secrets. Rotate any credential that has been exposed. |
| T9 | Uploaded files overwrite other files or expose internal paths | Generated stored filenames and controlled endpoints | Upload two files with the same original name. Confirm neither overwrites the other and users cannot choose arbitrary server paths. |
| T10 | Database or storage failure causes data loss or unsafe errors | Database configuration and error handling | In a safe local test environment, simulate an unavailable database or storage failure. Confirm the application fails safely without exposing stack traces or secrets. |
| T11 | Sessions last longer than intended | Configured session timeout (intended setting: 15 minutes) | Verify the deployed configuration and test session expiry. Confirm protected actions require authentication after expiry. |
| T12 | Uploaded content is interpreted unsafely by the browser | Controlled content types and safe preview/download behavior | Test allowed file types through preview and download. Confirm untrusted content is not executed as application HTML and response headers are appropriate. |

## 5. Security Controls to Verify in Code

- Passwords are hashed with BCrypt before database storage.
- Authentication is required for protected endpoints.
- Every document read, preview, download, and delete operation checks ownership on the server.
- Uploads enforce the configured size limit and an allowlist of extensions and content signatures.
- CSRF protection remains enabled for state-changing browser requests, except for narrowly justified endpoints.
- Login attempt restrictions are applied consistently and tested.
- Logout invalidates the authenticated session.
- Database credentials come from environment variables and are not committed to Git.
- Error responses do not reveal stack traces, credentials, or internal filesystem paths.
- Uploaded files use generated names rather than user-supplied paths.

## 6. Test Evidence Log

Record actual results after running each test. Do not mark a test as passed until it has been executed.

| Test | Result (Pass/Fail/Not run) | Evidence / notes |
|---|---|---|
| Unauthenticated access | Not run | |
| Cross-user document access | Not run | |
| Password hashing | Not run | |
| Invalid and oversized uploads | Not run | |
| CSRF rejection | Not run | |
| Login lockout | Not run | |
| Logout and session invalidation | Not run | |
| Secret scan / repository review | Not run | |
| Duplicate filenames / path handling | Not run | |
| Error handling | Not run | |
| Session timeout | Not run | |
| Preview and download safety | Not run | |

## 7. Known Limitations and Residual Risks

- A threat model is not a substitute for automated tests, a security review, or penetration testing.
- File persistence on an ephemeral deployment filesystem may not survive service restarts or redeployments. Confirm hosting storage behavior before relying on permanent document retention.
- The application has not been independently audited for production use.
- Rate limiting, account recovery, multi-factor authentication, malware scanning, durable object storage, monitoring, and backup/restore procedures should be evaluated before production use if they are not implemented.
- Security behavior may differ between local and deployed environments. Repeat important tests against the deployed service using accounts and files you control.

## 8. Responsible Testing

Only test environments you own or are authorized to assess. Use test accounts and non-sensitive sample documents. Do not attempt to access real users' files, disrupt service, or publish secrets.

## 9. Updating This Document

When a security issue is found:
1. Record the observed behavior and steps to reproduce it.
2. Identify the root cause.
3. Implement and review the fix.
4. Add or update a regression test.
5. Rerun the test and record the actual result here.
