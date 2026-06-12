# Oraculum SSO Architecture Plan

This document outlines the planned Single Sign-On (SSO) architecture for Oraculum, designed specifically for a Homelab environment where Keycloak serves as the master gatekeeper, while Google SSO is provided for restricted public access.

## Architecture Overview

We will use the **Spring Boot Native Approach** (`spring-boot-starter-oauth2-client`). 
Spring Boot will handle two Identity Providers (IdPs) directly:
1. **Keycloak**: Trusted, private SSO for the Homelab Admin.
2. **Google**: Public-facing SSO for friends and family.

Because Keycloak grants master access to the Homelab (e.g., Proxmox, Portainer), we **cannot** use Keycloak as an Identity Broker for Google users. Friends and family must authenticate directly with Google and never interact with the Keycloak server.

## Configuration & User Whitelisting

To prevent unauthorized Google users from accessing Oraculum, we will implement a Database Whitelist combined with dynamic per-provider auto-registration logic.

### 1. `application.yaml` Configuration
We will define custom security properties to dictate how each provider is handled when a user successfully authenticates:

```yaml
oraculum:
  security:
    providers:
      keycloak:
        auto-register: true
        default-role: ADMIN
      google:
        auto-register: false
        default-role: USER
```

### 2. The `t_user` Database Table
We will create a `t_user` table in the database to store authorized users and their assigned roles (`ADMIN` or `USER`).

### 3. Custom `OAuth2UserService` Interceptor
Spring Security will intercept all successful OIDC logins and execute the following logic based on the provider:

#### Scenario A: Admin logs in via Keycloak
1. User authenticates against Keycloak.
2. Interceptor checks properties: `keycloak` has `auto-register: true`.
3. Interceptor checks `t_user`. If the user does not exist, it inserts them into the database, assigns `ROLE_ADMIN`, and grants access.
4. **Result:** Frictionless access for the Homelab owner.

#### Scenario B: Unauthorized user logs in via Google
1. A random person authenticates via Google.
2. Interceptor checks properties: `google` has `auto-register: false`.
3. Interceptor checks `t_user`. The user's email is not found.
4. Interceptor throws an `AccessDeniedException`.
5. **Result:** The user is blocked and redirected to an error page.

#### Scenario C: Authorized friend logs in via Google
1. The Admin previously logged into the Vaadin UI and manually added the friend's email to `t_user` (assigning `ROLE_USER`).
2. The friend authenticates via Google.
3. Interceptor checks properties: `google` has `auto-register: false`.
4. Interceptor checks `t_user` and finds the friend's email.
5. Interceptor grants access with restricted `ROLE_USER` permissions.
6. **Result:** Secure, restricted access for friends and family.

## Required Dependencies
To implement this in the future, the following dependencies must be added to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

## UI Considerations
A custom Vaadin `LoginView` will be created with two distinct buttons:
- **[ Admin Login ]** -> Redirects to Keycloak
- **[ Sign in with Google ]** -> Redirects to Google
