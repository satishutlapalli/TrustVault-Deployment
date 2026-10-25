# TrustVault — AI Prompts and Development Notes

## Purpose
This file documents how AI assistance was used while developing TrustVault. It is a summary, not a verbatim transcript of every conversation. Replace or supplement these examples with your actual prompts and experiences.

## Project Context
TrustVault is a document-management web application using Java, Spring Boot, Spring Security, PostgreSQL, HTML, CSS, JavaScript, Maven, and Render.

## Prompt Categories

### 1. Project planning
**Example prompt:** "Help me build a secure document-management application using Java Spring Boot, PostgreSQL, HTML, CSS, and JavaScript. Explain the development process step by step for a beginner."

**How I used the response:** Record the architecture and implementation steps you actually adopted.

### 2. Backend implementation
**Example prompt:** "Explain how to structure Spring Boot controllers, services, repositories, and entity classes for registration, authentication, and document management."

**How I used the response:** Record which classes or concepts you implemented after reviewing the suggestions.

### 3. Database configuration
**Example prompt:** "Help me configure Spring Data JPA and PostgreSQL for my Spring Boot application, including the required environment variables."

**How I used the response:** Describe the configuration you changed and how you verified the connection. Never include passwords or secret values.

### 4. Authentication and security
**Example prompt:** "Help me configure Spring Security for login, logout, password hashing, protected routes, and CSRF protection. Explain how to test each behavior."

**How I used the response:** List only the security controls present in your code and the tests you actually ran.

### 5. Document management
**Example prompt:** "Help me implement document upload, listing, preview, download, and deletion, ensuring that a user can access only their own documents."

**How I used the response:** Describe the implementation retained and the ownership checks verified.

### 6. Frontend development
**Example prompt:** "Help me improve the TrustVault interface with a modern navy-blue and white SaaS design without breaking the existing login, registration, and upload functionality."

**How I used the response:** Note the pages or styling changed and any manual checks performed.

### 7. Debugging and testing
**Example prompt:** "Help me diagnose this Spring Boot error. Explain the likely cause, the smallest safe fix, and how I can verify the result."

**How I used the response:** For each real issue, add the error, AI suggestion, what you checked, the fix you made, and the actual test result.

### 8. Deployment
**Example prompt:** "Guide me through deploying my Spring Boot application with Docker, GitHub, Render, and PostgreSQL. Explain how to configure deployment environment variables safely."

**How I used the response:** Describe deployment steps completed and problems you resolved.

## Specific AI Mistakes or Corrections
Add real examples from your own development experience. Do not invent an AI mistake or claim a fix you did not make.

Use this template for each example:
- **Problem observed:** [Exact error or incorrect behavior.]
- **AI suggestion:** [Suggestion received.]
- **What I checked:** [How you verified it.]
- **Correction made:** [Actual code/configuration change.]
- **Verification:** [Command, test, or manual workflow and actual result.]

If a suggestion worked as intended, say so rather than labeling it an error.

## Verification and Engineering Practices
- Review code and configuration before applying changes.
- Run the Maven build and relevant automated tests.
- Test authentication and document ownership using separate test accounts.
- Test invalid uploads and CSRF behavior.
- Check deployment logs when the application fails to start.
- Keep database passwords, tokens, and other secrets out of source control.

## Tools Used
List only tools actually used during this project. Examples may include ChatGPT, Visual Studio Code, Git/GitHub, Maven, PostgreSQL, and Render. Edit this list to reflect your actual workflow.

## Final Note
This document should accurately represent my development process. Example prompts are templates, not exact historical prompts unless they match what I actually asked. Test results and debugging stories must be truthful and reproducible.
