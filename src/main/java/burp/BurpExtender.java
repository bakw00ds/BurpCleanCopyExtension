package burp;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

/**
 * BurpExtender – Clean Copy for Burp Suite
 *
 * Adds right-click context menu items that copy HTTP requests and/or responses
 * to the clipboard with CRLF (\r\n) line endings replaced by plain LF (\n).
 *
 * Why this matters:
 *   Burp Suite stores HTTP messages with standard CRLF line endings.
 *   Most native text editors silently ignore the \r, so pasting looks fine.
 *   Google Docs (and many other web-based editors) treat \r as a visible
 *   character, producing a doubled blank line after every header / body line.
 *   This extension strips the \r before the text hits the clipboard so that
 *   pasting into any target is clean and readable.
 */
public class BurpExtender implements IBurpExtender, IContextMenuFactory {

    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;

    // -------------------------------------------------------------------------
    // IBurpExtender
    // -------------------------------------------------------------------------

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        this.helpers   = callbacks.getHelpers();

        callbacks.setExtensionName("Clean Copy");
        callbacks.registerContextMenuFactory(this);

        callbacks.printOutput("Clean Copy loaded – right-click any request/response to copy with clean line endings.");
    }

    // -------------------------------------------------------------------------
    // IContextMenuFactory
    // -------------------------------------------------------------------------

    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        List<JMenuItem> items = new ArrayList<>();
        byte ctx = invocation.getInvocationContext();

        boolean hasRequest = ctx == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST
                || ctx == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST
                || ctx == IContextMenuInvocation.CONTEXT_PROXY_HISTORY
                || ctx == IContextMenuInvocation.CONTEXT_TARGET_SITE_MAP_TABLE
                || ctx == IContextMenuInvocation.CONTEXT_SCANNER_RESULTS;

        boolean hasResponse = ctx == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_RESPONSE
                || ctx == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_RESPONSE
                || ctx == IContextMenuInvocation.CONTEXT_PROXY_HISTORY
                || ctx == IContextMenuInvocation.CONTEXT_TARGET_SITE_MAP_TABLE
                || ctx == IContextMenuInvocation.CONTEXT_SCANNER_RESULTS;

        if (!hasRequest && !hasResponse) {
            return items;   // nothing relevant in this context
        }

        // ── Sub-menu parent ──────────────────────────────────────────────────
        JMenu menu = new JMenu("Clean Copy");

        if (hasRequest) {
            JMenuItem copyReq = new JMenuItem("Copy Request");
            copyReq.addActionListener(e -> copyRequest(invocation));
            menu.add(copyReq);
        }

        if (hasResponse) {
            JMenuItem copyResp = new JMenuItem("Copy Response");
            copyResp.addActionListener(e -> copyResponse(invocation));
            menu.add(copyResp);
        }

        if (hasRequest && hasResponse) {
            menu.addSeparator();
            JMenuItem copyBoth = new JMenuItem("Copy Request + Response");
            copyBoth.addActionListener(e -> copyRequestAndResponse(invocation));
            menu.add(copyBoth);
        }

        items.add(menu);
        return items;
    }

    // -------------------------------------------------------------------------
    // Copy helpers
    // -------------------------------------------------------------------------

    private void copyRequest(IContextMenuInvocation invocation) {
        IHttpRequestResponse[] messages = invocation.getSelectedMessages();
        if (messages == null || messages.length == 0) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.length; i++) {
            if (i > 0) sb.append("\n\n───────────────── Request ").append(i + 1).append(" ─────────────────\n\n");
            byte[] req = messages[i].getRequest();
            if (req != null) sb.append(clean(helpers.bytesToString(req)));
        }
        toClipboard(sb.toString());
    }

    private void copyResponse(IContextMenuInvocation invocation) {
        IHttpRequestResponse[] messages = invocation.getSelectedMessages();
        if (messages == null || messages.length == 0) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.length; i++) {
            if (i > 0) sb.append("\n\n───────────────── Response ").append(i + 1).append(" ─────────────────\n\n");
            byte[] resp = messages[i].getResponse();
            if (resp != null) sb.append(clean(helpers.bytesToString(resp)));
            else sb.append("[No response captured]");
        }
        toClipboard(sb.toString());
    }

    private void copyRequestAndResponse(IContextMenuInvocation invocation) {
        IHttpRequestResponse[] messages = invocation.getSelectedMessages();
        if (messages == null || messages.length == 0) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.length; i++) {
            if (i > 0) sb.append("\n\n══════════════════════════════════════════════\n\n");

            byte[] req  = messages[i].getRequest();
            byte[] resp = messages[i].getResponse();

            if (req != null) {
                sb.append("──── REQUEST ────\n\n");
                sb.append(clean(helpers.bytesToString(req)));
            }
            if (resp != null) {
                sb.append("\n\n──── RESPONSE ────\n\n");
                sb.append(clean(helpers.bytesToString(resp)));
            }
        }
        toClipboard(sb.toString());
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Strip carriage-returns so every line ending is a plain LF.
     * This prevents Google Docs and other web editors from rendering double
     * blank lines when pasting text that originated as CRLF HTTP messages.
     */
    private static String clean(String raw) {
        return raw.replace("\r\n", "\n").replace("\r", "\n");
    }

    private void toClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit()
                   .getSystemClipboard()
                   .setContents(new StringSelection(text), null);
        } catch (Exception ex) {
            callbacks.printError("Clean Copy – clipboard error: " + ex.getMessage());
        }
    }
}
