# TrustVault Admin and Document Sharing Add-on

## What was added
- User roles (`USER` and `ADMIN`) with server-side role checks.
- Admin dashboard at `/admin.html` with user/document metadata and counts. It does not expose file contents or password hashes.
- Share an owned document with a registered username using the **Share** button. Recipients get Viewer access to preview/download only.
- Shared documents appear at `/shared-documents.html`.
- Owners can revoke sharing through `DELETE /api/shares/{documentId}/{recipientUsername}` (CSRF token required).
- A recipient cannot delete the owner's document.

## Configure an administrator safely
1. Register a normal account using the existing registration page.
2. In your local environment, set `TRUSTVAULT_ADMIN_USERNAME` to that exact registered username.
3. Restart the application. `AdminBootstrapService` promotes only that configured account to ADMIN. Public registration cannot set roles.
4. For Render, add `TRUSTVAULT_ADMIN_USERNAME` in the web service environment variables and redeploy/restart. The account must already exist in the database.
5. Open `/admin.html` while logged in as that account. Other users should receive 403.

Keep this environment variable limited to an account you control. Clear it after the intended account has been promoted if you do not want automatic reconciliation on future starts.

## Sharing behavior
- Owner enters a registered recipient username in the Share button prompt.
- Share access is VIEWER only: preview/download are allowed. Uploading, deleting, and re-sharing remain owner-only.
- The backend checks permission for every preview/download request; hiding UI buttons is not used as the security boundary.
- The owner can revoke access using the revoke endpoint. A future UI for managing/revoking recipients can be added separately.

## Important validation before deployment
- Run `mvnw.cmd test` on Windows (or `./mvnw test` on Linux/macOS).
- Test as two separate accounts: recipient can preview/download a shared file but cannot delete it or access unshared files.
- Test `/admin.html` and `/api/admin/**` as both ADMIN and USER.
- Test CSRF behavior for share/revoke state-changing requests.
- Back up the database before deploying schema changes. JPA `ddl-auto=update` should create the new role/share schema, but confirm in a non-production environment first.
- Existing deployment file storage may be ephemeral; sharing does not make files durable across restarts.
