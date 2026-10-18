package control.services;

import baseLib.BaseModel;
import business.facade.AcaoFacade;
import business.facade.LocalFacade;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import model.Cidade;
import model.Local;
import model.Nacao;
import model.Personagem;
import model.PersonagemOrdem;

/** Read-only projection of currently loaded orders. No history or review state. */
public final class OrdersList {
    private OrdersList() { }

    /** Ownership, rather than membership in a merged world's nation collection. */
    public static boolean ownedBy(Nacao nation, model.Jogador player) {
        return nation != null && nation.getOwner() != null && player != null
                && nation.getOwner().getId() == player.getId();
    }

    /** Derived from all loaded PC orders, independent of the browser's current filters. */
    public static Instant latestOwnCharacterTime(List<Row> rows, model.Jogador player) {
        return rows.stream().filter(row -> row.actor() instanceof Personagem && ownedBy(row.nation(), player))
                .map(Row::time).filter(java.util.Objects::nonNull).max(Instant::compareTo).orElse(null);
    }

    public record Row(BaseModel actor, int slot, PersonagemOrdem order, Local location,
                      String coordinates, String searchText) {
        public Instant time() { return order.getRecordedUpdateTime(); }
        public String key() { return actor.getTpActor() + ":" + actor.getCodigo() + ":" + slot; }
        public Nacao nation() { return actor.getNacao(); }
        public String player() {
            return nation().getOwner() == null ? "" : nation().getOwner().getNome();
        }
        public String parameters() { return String.join(", ", order.getParametrosDisplay()); }
    }

    public static List<Row> collect(Collection<? extends BaseModel> actors, SortedMap<String, Local> locais) {
        List<Row> rows = new ArrayList<>();
        for (BaseModel actor : actors) {
            if (actor.getNacao() == null) continue;
            Local location = location(actor);
            for (Map.Entry<Integer, PersonagemOrdem> entry : actor.getAcoes().entrySet()) {
                PersonagemOrdem po = entry.getValue();
                if (po == null || po.getOrdem() == null) continue;
                String coords = location == null ? "" : LocalFacade.getCoordenadas(location);
                // Search resolved army-path hexes as well as the raw directions and selected targets.
                if (actor instanceof Personagem pc && new AcaoFacade().isMovimentoDirection(po)
                        && !po.getParametrosId().isEmpty()) {
                    try {
                        for (Local step : new AcaoFacade().getLocalDestinationPath(pc, po, locais).values()) {
                            if (step != null) coords += " " + LocalFacade.getCoordenadas(step);
                        }
                    } catch (RuntimeException ex) {
                        // An incomplete path still has searchable raw parameters and its origin.
                    }
                }
                String player = actor.getNacao().getOwner() == null ? "" : actor.getNacao().getOwner().getNome();
                String search = String.join(" ", player, actor.getNacao().getNome(), actor.getNome(),
                        String.valueOf(actor.getCodigo()), po.getOrdem().getNome(), po.getOrdem().getDescricao(),
                        po.getParametrosId().toString(), po.getParametrosDisplay().toString(), coords);
                rows.add(new Row(actor, entry.getKey(), po, location, coords, search.toLowerCase(Locale.ROOT)));
            }
        }
        return rows;
    }

    public static boolean matches(Row row, String text, LocalDate since, ZoneId zone) {
        if (since != null && (row.time() == null || row.time().isBefore(since.atStartOfDay(zone).toInstant()))) {
            return false;
        }
        for (String token : text.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!row.searchText().contains(token)) return false;
        }
        return true;
    }

    public static Local location(BaseModel actor) {
        if (actor instanceof Personagem pc) return pc.getLocal();
        if (actor instanceof Cidade city) return city.getLocal();
        if (actor instanceof Nacao nation && nation.getCapital() != null) return nation.getCapital().getLocal();
        return null;
    }
}
