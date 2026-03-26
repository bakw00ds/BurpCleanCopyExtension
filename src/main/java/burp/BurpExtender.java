package burp;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * BurpExtender – Clean Copy for Burp Suite
 *
 * Adds a "Clean Copy" right-click sub-menu in every request/response panel.
 * Copies HTTP messages with CRLF (\r\n) stripped to plain LF (\n) and also
 * provides an HTML clipboard flavor so Google Docs renders line breaks correctly
 * instead of doubling them.
 *
 * No text selection required — just right-click anywhere in the panel.
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
        callbacks.printOutput("Clean Copy v1.1.0 loaded — right-click any request/response panel.");
    }

    // -------------------------------------------------------------------------
    // IContextMenuFactory
    // -------------------------------------------------------------------------

    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        List<JMenuItem> items = new ArrayList<>();
        byte ctx = invocation.getInvocationContext();

        // Show the sub-menu in any context where there is a request or response
        boolean relevant =
                ctx == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST   ||
                ctx == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST   ||
                ctx == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_RESPONSE  ||
                ctx == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_RESPONSE  ||
                ctx == IContextMenuInvocation.CONTEXT_PROXY_HISTORY            ||
                ctx == IContextMenuInvocation.CONTEXT_TARGET_SITE_MAP_TABLE    ||
                ctx == IContextMenuInvocation.CONTEXT_SCANNER_RESULTS;

        if (!relevant) return items;

        JMenu menu = new JMenu("Clean Copy");

        // Always show all three options regardless of which pane was right-clicked
        JMenuItem copyReq = new JMenuItem("Copy Request");
        copyReq.addActionListener(e -> copyRequest(invocation));
        menu.add(copyReq);

        JMenuItem copyResp = new JMenuItem("Copy Response");
        copyResp.addActionListener(e -> copyResponse(invocation));
        menu.add(copyResp);

        menu.addSeparator();

        JMenuItem copyBoth = new JMenuItem("Copy Request + Response");
        copyBoth.addActionListener(e -> copyBoth(invocation));
        menu.add(copyBoth);

        items.add(menu);
        return items;
    }

    // -------------------------------------------------------------------------
    // Copy actions
    // -------------------------------------------------------------------------

    private void copyRequest(IContextMenuInvocation invocation) {
        IHttpRequestResponse[] msgs = invocation.getSelectedMessages();
        if (msgs == null || msgs.length == 0) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msgs.length; i++) {
            if (i > 0) sb.append("\n\n───────────────────────────────────\n\n");
            byte[] req = msgs[i].getRequest();
            if (req != null) sb.append(clean(helpers.bytesToString(req)));
        }
        toClipboard(sb.toString());
    }

    private void copyResponse(IContextMenuInvocation invocation) {
        IHttpRequestResponse[] msgs = invocation.getSelectedMessages();
        if (msgs == null || msgs.length == 0) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msgs.length; i++) {
            if (i > 0) sb.append("\n\n───────────────────────────────────\n\n");
            byte[] resp = msgs[i].getResponse();
            if (resp != null) sb.append(clean(helpers.bytesToString(resp)));
            else              sb.append("[No response captured]");
        }
        toClipboard(sb.toString());
    }

    private void copyBoth(IContextMenuInvocation invocation) {
        IHttpRequestResponse[] msgs = invocation.getSelectedMessages();
        if (msgs == null || msgs.length == 0) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msgs.length; i++) {
            if (i > 0) sb.append("\n\n═══════════════════════════════════\n\n");

            byte[] req  = msgs[i].getRequest();
            byte[] resp = msgs[i].getResponse();

            if (req != null) {
                sb.append("──── REQUEST ────\n\n");
                sb.append(clean(helpers.bytesToString(req)));
            }
            if (resp != null) {
                sb.append("\n\n──── RESPONSE ────\n\n");
                sb.append(clean(helpers.bytesToString(resp)));
            } else {
                sb.append("\n\n──── RESPONSE ────\n\n[No response captured]");
            }
        }
        toClipboard(sb.toString());
    }

    // -------------------------------------------------------------------------
    // Clipboard helpers
    // -------------------------------------------------------------------------

    /**
     * Strip carriage returns so every line ending is a plain LF.
     * This prevents Google Docs (and similar web editors) from rendering each
     * \r\n pair as two line breaks.
     */
    private static String clean(String raw) {
        return raw.replace("\r\n", "\n").replace("\r", "\n");
    }

    /**
     * Place text on the system clipboard in both plain-text and HTML flavors.
     * Google Docs and most web editors prefer HTML when available, which means
     * our explicit &lt;br&gt; tags control line breaks rather than leaving it to
     * the editor to interpret raw newline characters.
     */
    private void toClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit()
                   .getSystemClipboard()
                   .setContents(new CleanTransferable(text), null);
        } catch (Exception ex) {
            callbacks.printError("Clean Copy – clipboard error: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Custom Transferable: provides both text/plain and text/html
    // -------------------------------------------------------------------------

    private static class CleanTransferable implements Transferable {

        private static final DataFlavor HTML_FLAVOR;

        static {
            DataFlavor f = null;
            try {
                f = new DataFlavor("text/html; charset=utf-8; class=java.lang.String");
            } catch (ClassNotFoundException ignored) { }
            HTML_FLAVOR = f;
        }

        private final String plain;
        private final String html;

        CleanTransferable(String text) {
            this.plain = text;
            this.html  = buildHtml(text);
        }

        /** Wrap the plain text in a <pre> block so spacing and indentation are preserved. */
        private static String buildHtml(String text) {
            String escaped = text
                    .replace("&",  "&amp;")
                    .replace("<",  "&lt;")
                    .replace(">",  "&gt;")
                    .replace("\"", "&quot;");
            // Each \n becomes a <br> — Google Docs treats these as single line breaks
            String lined = escaped.replace("\n", "<br>\n");
            return "<html><body><pre style=\"font-family:monospace;font-size:12px;\">"
                    + lined + "</pre></body></html>";
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            if (HTML_FLAVOR != null) {
                return new DataFlavor[]{ HTML_FLAVOR, DataFlavor.stringFlavor };
            }
            return new DataFlavor[]{ DataFlavor.stringFlavor };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if (flavor.equals(DataFlavor.stringFlavor)) return true;
            return HTML_FLAVOR != null && flavor.equals(HTML_FLAVOR);
        }

        @Override
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            if (flavor.equals(DataFlavor.stringFlavor)) return plain;
            if (HTML_FLAVOR != null && flavor.equals(HTML_FLAVOR)) return html;
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
