# HMAC Request Signing Integration Guide

## Overview

The application uses HMAC-related request security components to protect sensitive API operations and reduce replay risks.

HMAC request metadata is transmitted using dedicated HTTP headers.

## Required Headers

Protected requests use the following headers:

| Header | Description |
| --- | --- |
| `X-Timestamp` | Unix timestamp representing when the request was created |
| `X-Nonce` | Unique value used to detect replayed requests |
| `X-Signature` | HMAC-SHA256 signature associated with the request |

## Signature Payload Construction

The HMAC signature utility constructs the signing payload by concatenating:

```text
HTTP_METHOD + REQUEST_PATH + REQUEST_BODY + TIMESTAMP + NONCE