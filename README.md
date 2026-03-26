# Burp Clean Copy Extension

A Burp Suite extension that copies HTTP requests and responses to your clipboard with **clean line endings** — so they paste neatly into Google Docs, Notion, Confluence, or any other web-based editor without producing double blank lines.

## The Problem

Burp Suite stores HTTP messages using standard CRLF (`\r\n`) line endings. Native text editors (VS Code, Sublime, etc.) silently discard the `\r`, so pasting looks fine. Web-based rich-text editors like **Google Docs** treat the `\r` as a visible character, which causes every line to be followed by an extra blank line — making pasted requests messy and hard to read.

## The Fix

This extension adds a **"Clean Copy"** sub-menu to Burp's right-click context menu. It copies the selected request/response to your clipboard after converting all `\r\n` endings to plain `\n`, so the text pastes cleanly everywhere.

## Menu Options

Right-click any request or response in Proxy History, Repeater, Target Site Map, etc.:

- **Clean Copy → Copy Request** – copy just the HTTP request
- **Clean Copy → Copy Response** – copy just the HTTP response
- **Clean Copy → Copy Request + Response** – copy both, separated by a clear divider

## Build

Requires Java 11+ and Maven.

```bash
mvn clean package
```

The ready-to-import JAR will be at:

```
target/burp-clean-copy.jar
```

## Install in Burp Suite

1. Open Burp Suite → **Extensions** tab → **Add**
2. Extension type: **Java**
3. Select `target/burp-clean-copy.jar`
4. Click **Next** — you should see `Clean Copy loaded` in the Output panel

## Usage

1. In **Proxy History**, **Repeater**, **Target**, etc., select one or more requests
2. Right-click → **Clean Copy** → choose what you want to copy
3. Paste into Google Docs (or anywhere) — no extra blank lines
