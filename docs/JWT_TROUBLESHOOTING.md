# JWT Authentication Troubleshooting Guide

## Overview

This guide helps contributors diagnose common JWT authentication problems in the Voice-Driven Virtual Customer Success Manager.

JWT authentication issues commonly involve expired tokens, invalid signatures, malformed authorization headers, configuration problems, or refresh token failures.

---

## Authorization Header Format

Authenticated API requests should provide the JWT using the `Authorization` header.

```http
Authorization: Bearer <jwt-token>