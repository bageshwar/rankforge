# Security Documentation

## JWT Token Storage and XSS Mitigation

### Current Implementation

The RankForge UI stores JWT authentication tokens in `localStorage` for session persistence. This is a standard approach for Single Page Applications (SPAs), but requires careful attention to XSS (Cross-Site Scripting) mitigation.

### Token Storage Location

- **Storage**: `localStorage.getItem('authToken')`
- **Usage**: Tokens are automatically attached to API requests via Axios interceptors
- **Lifetime**: Tokens expire after 7 days (configurable on backend)

### XSS Mitigation Strategies

#### 1. **Content Security Policy (CSP)**

The application should implement a strict Content Security Policy to prevent XSS attacks. Recommended CSP headers:

```
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' https://api.steamcommunity.com;
```

**Implementation**: Configure CSP headers in your web server (nginx, Apache, or CDN) or via meta tags in `index.html`.

#### 2. **Input Sanitization**

All user-generated content displayed in the UI must be sanitized:

- **React's built-in escaping**: React automatically escapes content in JSX expressions (`{userInput}`)
- **Dangerous HTML**: Never use `dangerouslySetInnerHTML` with unsanitized user input
- **Third-party libraries**: Use libraries like `DOMPurify` if HTML rendering is required

**Example**:
```tsx
// ✅ Safe - React escapes automatically
<div>{userInput}</div>

// ❌ Dangerous - Never do this with user input
<div dangerouslySetInnerHTML={{ __html: userInput }} />
```

#### 3. **HTTPS Only**

- **Production**: Always use HTTPS to prevent man-in-the-middle attacks
- **Cookie Security**: If using cookies in the future, set `Secure` and `HttpOnly` flags
- **HSTS**: Enable HTTP Strict Transport Security headers

#### 4. **Token Validation**

The application validates tokens on every API request:

- **Backend validation**: All protected endpoints validate JWT tokens
- **Automatic cleanup**: Invalid/expired tokens are automatically removed from localStorage
- **401 handling**: Unauthorized responses trigger automatic logout

**Implementation** (`src/services/api.ts`):
```typescript
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      localStorage.removeItem('authToken');
      localStorage.removeItem('authUser');
      window.dispatchEvent(new Event('auth:logout'));
    }
    return Promise.reject(error);
  }
);
```

#### 5. **Dependencies Security**

Regularly audit and update dependencies:

```bash
# Check for vulnerabilities
npm audit

# Fix automatically
npm audit fix

# Update dependencies
npm update
```

#### 6. **Build-time Security**

- **Source maps**: Disabled in production builds (`vite.config.ts`)
- **Minification**: Enabled to obfuscate code
- **Environment variables**: Never commit secrets to version control

### Known Limitations

1. **localStorage vs httpOnly Cookies**:
   - **Current**: Tokens stored in `localStorage` (accessible to JavaScript)
   - **Trade-off**: More vulnerable to XSS, but works seamlessly with SPAs
   - **Mitigation**: Strict CSP, input sanitization, and HTTPS

2. **Token in URL** (OAuth callback):
   - **Current**: JWT token passed in URL query parameter during OAuth callback
   - **Risk**: Tokens may appear in server logs, browser history, referrer headers
   - **Mitigation**: Short-lived tokens (7 days), HTTPS required, tokens cleared immediately after use
   - **Future**: Consider implementing code exchange pattern

### Best Practices for Developers

1. **Never log tokens**: Avoid `console.log(token)` in production code
2. **Sanitize all inputs**: Always validate and sanitize user inputs before rendering
3. **Use TypeScript**: Leverage TypeScript's type system to prevent common errors
4. **Regular audits**: Run `npm audit` regularly and update dependencies
5. **Security headers**: Configure proper security headers in production
6. **Error handling**: Don't expose sensitive information in error messages

### Production Checklist

- [ ] HTTPS enabled and enforced
- [ ] Content Security Policy configured
- [ ] Environment variables secured (not in version control)
- [ ] Dependencies up to date (`npm audit` passes)
- [ ] Source maps disabled in production
- [ ] Error messages don't leak sensitive information
- [ ] CORS properly configured (not `*` in production)
- [ ] JWT secret is at least 32 characters
- [ ] Token expiration set appropriately (7 days default)

### Future Enhancements

1. **Token Refresh**: Implement refresh token mechanism for better security
2. **Code Exchange**: Replace URL token with code exchange pattern for OAuth
3. **httpOnly Cookies**: Consider moving to httpOnly cookies with CSRF protection
4. **Rate Limiting**: Implement rate limiting on authentication endpoints
5. **2FA**: Add two-factor authentication for enhanced security

### References

- [OWASP XSS Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
- [OWASP JWT Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [React Security Best Practices](https://reactjs.org/docs/dom-elements.html#dangerouslysetinnerhtml)
