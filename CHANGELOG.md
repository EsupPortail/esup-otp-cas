# Changelog
## v2.3.1 (2026-11-18) ([8.0.x](https://github.com/EsupPortail/esup-otp-cas/releases/tag/v2.3.1-cas_v8.0.x))
- Add configurable connection and read timeouts for requests to esup-otp-api, both defaulting to 10 seconds.
- Omit authentication codes from logs.

## v2.3.0 (2026-08-27) ([8.0.x](https://github.com/EsupPortail/esup-otp-cas/releases/tag/v2.3.0-cas_v8.0.x))
- Update compatibility to Apereo CAS 8.0.1.2 
- Fix bean conflicts with other CAS MFA providers and fix trusted-device webflow configuration.
- Add translations for the CAS MFA provider selection screen.
- Add a Docker Compose development environment and run Playwright tests in CI

## v2.2.0 (2025-11-07) ([7.3.x](https://github.com/EsupPortail/esup-otp-cas/releases/tag/v2.2.0-cas_v7.3.x))
- load esup-otp-api/public/login.js as ESM module
- **BREAKING CHANGES**
    - require esup-otp-api => [v2.2.0](https://github.com/EsupPortail/esup-otp-api/releases/tag/v2.2.0)

## v2.1.0 (2025-10-17) ([7.3.x](https://github.com/EsupPortail/esup-otp-cas/releases/tag/v2.1.0-cas_v7.3.x))
- Centralize the common code from [esup-otp-cas](https://github.com/EsupPortail/esup-otp-cas), [esup-otp-cas-server](https://github.com/EsupPortail/esup-otp-cas-server), and [esup-otp-shibboleth](https://github.com/Renater/esup-otp-shibboleth/) in [esup-otp-api](https://github.com/EsupPortail/esup-otp-api/tree/master/public).
- **BREAKING CHANGES**
    - require esup-otp-api => [v2.1.0](https://github.com/EsupPortail/esup-otp-api/releases/tag/v2.1.0)
    - add `esupotp.otpManagerUrl=https://esup-otp-manager.univ.fr/` configuration in *esupotp.properties*
    - To modify the display or CSS, this is now done [here](https://github.com/EsupPortail/esup-otp-api/tree/master/public).

## v1.2.7 (2025-09-26) ([7.1.x](https://github.com/EsupPortail/esup-otp-cas/releases/tag/v1.2.7-cas_v7.1.x), [7.2.x](https://github.com/EsupPortail/esup-otp-cas/releases/tag/v1.2.7-cas_v7.2.x))
- feat: esupnfc with Esup Auth (V2) app [98bfaf7](https://github.com/EsupPortail/esup-otp-cas/commit/98bfaf7b485c6e31a6e6435b215e1219a3d089d1)

## v1.2.4 (2025-09-26) ([7.0.x](https://github.com/EsupPortail/esup-otp-cas/releases/tag/v1.2.4-cas_v7.0.x), [7.1.x](https://github.com/EsupPortail/esup-otp-cas/releases/tag/v1.2.4-cas_v7.1.x))
- support new method : Passcode Grids [de09444](https://github.com/EsupPortail/esup-otp-cas/commit/de0944468b3a7e565fb29fac53b6c68c7e8224a2)
- add `esupotp.failureMode=CLOSED` configuration in *esupotp.properties* (used to define the behavior if esup-otp-api is down)
    - `CLOSED` (default) => block MFA if esup-otp-api is unreachable by CAS
    - `OPEN` => bypass MFA if esup-otp-api is unreachable by CAS
- cleanup `esupotp.trustedDeviceEnabled` configuration in *esupotp.properties* [df69a3c](https://github.com/EsupPortail/esup-otp-cas/commit/df69a3c3e45c1391e93ea54f80dc49ceb205182d)