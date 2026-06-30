package control.services;

import baseLib.BaseModel;
import control.facade.WorldFacadeCounselor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import model.Habilidade;
import model.Jogador;
import model.PersonagemOrdem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistenceCommons.SettingsManager;
import persistenceCommons.SysApoio;

/**
 * Tracks whether the loaded order set matches what the server holds, for the status-bar order-sync
 * indicator. The hash is computed entirely client-side (the site only stores the string verbatim),
 * over a canonical serialization of the active player's actions that is:
 * <ul>
 *   <li>display/locale-independent - uses the raw {@code getParametrosId()} + the order number, not
 *       the localized description/display params;</li>
 *   <li>order-of-entry independent - the action lines are sorted, so a Counselor reload that
 *       reshuffles the in-memory list never moves the hash.</li>
 * </ul>
 * States resolved against the server's stored hash for the current turn:
 * <ul>
 *   <li>{@code NO_TOKEN} - no playerToken saved (can neither upload nor query);</li>
 *   <li>{@code SENT} - local hash equals the server's stored hash;</li>
 *   <li>{@code PENDING} - local hash differs (unsent changes);</li>
 *   <li>{@code UNKNOWN} - server hash not known for this turn (offline / nothing sent yet).</li>
 * </ul>
 */
public final class OrdersHashService {

    public enum OrderSyncState {
        NO_TOKEN, SENT, PENDING, UNKNOWN
    }

    private static final Log log = LogFactory.getLog(OrdersHashService.class);
    private static final OrdersHashService INSTANCE = new OrdersHashService();

    private String localHash = null;
    private String serverHash = null;
    private int serverTurn = -1;

    private OrdersHashService() {
    }

    public static OrdersHashService getInstance() {
        return INSTANCE;
    }

    /** Canonical hash of the active player's current order set (see class doc). */
    public static String computeHash(WorldFacadeCounselor wfc) {
        Jogador jogadorAtivo = wfc.getJogadorAtivo();
        List<String> lines = new ArrayList<>();
        for (BaseModel actor : wfc.getActors()) {
            if (jogadorAtivo == null || !jogadorAtivo.isNacao(actor.getNacao())) {
                continue;
            }
            String actorKey = enc(actor.getCodigo()); // cross-type unique key
            for (PersonagemOrdem po : actor.getAcoes().values()) {
                if (po == null || po.getOrdem() == null) {
                    continue;
                }
                StringBuilder params = new StringBuilder();
                if (po.getParametrosId() != null) {
                    for (String p : po.getParametrosId()) {
                        params.append(enc(p)).append(','); // each param encoded -> no delimiter collisions
                    }
                }
                lines.add("A|" + actorKey + "|" + po.getOrdem().getNumero() + "|" + params);
            }
            // Startup packages (turn 0) are part of the submitted order set but live on the nation's
            // habilidades, disjoint from getAcoes() - include them or a package-only change is a false SENT.
            for (Map.Entry<String, Habilidade> e : actor.getHabilidades().entrySet()) {
                try {
                    if (e.getValue() != null && e.getValue().isPackage()) {
                        lines.add("P|" + actorKey + "|" + enc(e.getKey()));
                    }
                } catch (RuntimeException ignore) {
                    // isPackage() can NPE on partially-built abilities (mirrors getPackages); skip
                }
            }
        }
        Collections.sort(lines); // order-of-entry independent
        return SysApoio.sha256(String.join("\n", lines));
    }

    /** URL-encode a component so it can never contain the '|' / ',' / '\n' delimiters used above. */
    private static String enc(String s) {
        if (s == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            return s; // UTF-8 always present; unreachable
        }
    }

    /** Recompute and cache the local hash from the current world. */
    public void refreshLocal(WorldFacadeCounselor wfc) {
        try {
            this.localHash = computeHash(wfc);
        } catch (RuntimeException ex) {
            log.warn("Could not compute orders hash: " + ex);
            this.localHash = null;
        }
    }

    /** Record the server's stored hash from a "&lt;turno&gt;|&lt;hash&gt;" / "NONE" fetch response. */
    public void setServerResponse(String response) {
        this.serverHash = null;
        this.serverTurn = -1;
        if (response == null) {
            return;
        }
        String r = response.trim();
        if (r.isEmpty() || "NONE".equalsIgnoreCase(r)) {
            return;
        }
        int bar = r.indexOf('|');
        if (bar > 0) {
            try {
                this.serverTurn = Integer.parseInt(r.substring(0, bar).trim());
            } catch (NumberFormatException ignore) {
                // leave serverTurn = -1 -> resolves to UNKNOWN
            }
            this.serverHash = r.substring(bar + 1).trim();
        }
    }

    /** After a successful upload the loaded orders ARE what the server holds (no round-trip needed). */
    public void markSent(int turn) {
        this.serverHash = this.localHash;
        this.serverTurn = turn;
    }

    /** Resolve the indicator state for the given current turn. */
    public OrderSyncState resolve(int currentTurn) {
        if (SettingsManager.getInstance().getConfig("playerToken", "").isEmpty()) {
            return OrderSyncState.NO_TOKEN;
        }
        if (serverHash == null || localHash == null || serverTurn != currentTurn) {
            return OrderSyncState.UNKNOWN;
        }
        return serverHash.equals(localHash) ? OrderSyncState.SENT : OrderSyncState.PENDING;
    }

    public String getLocalHash() {
        return localHash;
    }
}
