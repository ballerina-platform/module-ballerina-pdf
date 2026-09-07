# Changelog

This file contains all the notable changes done to the Ballerina `pdf` package through the releases.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- [[#9132] Add `Type/Library` and `Area/Storage & File Management` Keywords, Reformat README for Connector Store Discoverability](https://github.com/ballerina-platform/ballerina-library/issues/9132)

## [0.9.1] - 2026-04-23

### Added
- [pdf module layout configuration is not working](https://github.com/ballerina-platform/ballerina-library/issues/8757)

## [0.9.0] - 2026-03-02

### Added
- HTML-to-PDF conversion via `parseHtml()` with configurable page size, margins, fonts, and CSS injection
- PDF text extraction via `extractText()`, `fileExtractText()`, and `urlExtractText()`
- PDF-to-image conversion via `toImages()`, `fileToImages()`, and `urlToImages()` (Base64-encoded PNG)
- Standard page size presets: `A4`, `LETTER`, `LEGAL`
- Custom page dimensions support via `CustomPageSize` record
- Custom font registration via `Font` record and `customFonts` option
- Additional CSS injection via `additionalCss` option
- Max pages limit via `maxPages` option
- Bundled Liberation Sans and Liberation Serif fonts (Apache-licensed)
- Distinct error types: `HtmlParseError`, `RenderError`, `ReadError`
