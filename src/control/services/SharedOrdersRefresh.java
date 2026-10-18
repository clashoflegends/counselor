package control.services;

import baseLib.BaseModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Comando;
import model.ComandoDetail;
import model.Nacao;
import model.Ordem;
import model.Partida;
import model.PersonagemOrdem;

/** Preflight an entire owner's shared file before replacing any loaded actions. */
public final class SharedOrdersRefresh {
    private SharedOrdersRefresh() { }

    public record Prepared(Map<BaseModel, List<PersonagemOrdem>> orders) { }

    public static Prepared prepare(Comando command, Partida game, Collection<? extends BaseModel> actors,
            Map<String, Ordem> available) {
        if (!command.isSerial() || command.getPartidaId() != game.getId() || command.getTurno() != game.getTurno()
                || command.getJogadorId() == game.getJogadorAtivo().getId()) {
            throw new IllegalArgumentException("Order file scope mismatch");
        }
        Map<String, BaseModel> byCode = new LinkedHashMap<>();
        Map<Integer, Nacao> nations = new LinkedHashMap<>();
        Map<BaseModel, List<PersonagemOrdem>> replacements = new LinkedHashMap<>();
        for (BaseModel actor : actors) {
            Nacao n = actor.getNacao();
            if (n == null) continue;
            nations.put(n.getId(), n);
            if (n.getOwner() != null && n.getOwner().getId() == command.getJogadorId()) {
                replacements.put(actor, new ArrayList<>());
                if (actor.getCodigo() != null && byCode.put(actor.getCodigo(), actor) != null) {
                    throw new IllegalArgumentException("Ambiguous actor code");
                }
            }
        }
        if (replacements.isEmpty()) throw new IllegalArgumentException("Unknown shared owner");
        for (Integer id : command.getPackages().keySet()) {
            Nacao n = nations.get(id);
            if (n == null || n.getOwner() == null || n.getOwner().getId() != command.getJogadorId()) {
                throw new IllegalArgumentException("Package owner mismatch");
            }
        }
        for (ComandoDetail detail : command.getOrdens()) {
            BaseModel actor = byCode.get(detail.getActorCodigo());
            Ordem action = available.get(detail.getOrdemCodigo());
            if (actor == null || action == null || !String.valueOf(actor.getNacao().getId()).equals(detail.getNacaoCodigo())
                    || !sameType(actor, detail)) {
                throw new IllegalArgumentException("Unknown or mismatched shared actor/action");
            }
            PersonagemOrdem order = new PersonagemOrdem();
            order.setNome(actor.getNome()); order.setOrdem(action);
            order.setParametrosId(new ArrayList<>(detail.getParametroId()));
            order.setParametrosDisplay(new ArrayList<>(detail.getParametroDisplay()));
            order.setRecordedUpdateTime(detail.getRecordedUpdateTime());
            replacements.get(actor).add(order);
        }
        return new Prepared(replacements);
    }

    private static boolean sameType(BaseModel actor, ComandoDetail d) {
        return switch (actor.getTpActor()) {
            case "P" -> d.isActorPersonagem(); case "G" -> d.isActorNpc();
            case "C" -> d.isActorCidade(); case "N" -> d.isActorNacao();
            case "E" -> d.isActorExercito(); default -> false;
        };
    }

    /**
     * Compute the entire owner's forecast before mutation. UI observers are notified only AFTER
     * this batch completes, so a rendering/notification failure cannot interrupt an actor replacement.
     */
    public static void apply(Prepared prepared, Map<Nacao, java.util.Set<PersonagemOrdem>> forecasts,
            java.util.function.ToIntBiFunction<PersonagemOrdem, Nacao> cost) {
        Map<Nacao, java.util.Set<PersonagemOrdem>> replacementForecast = new LinkedHashMap<>();
        for (var entry : prepared.orders().entrySet()) {
            Nacao nation = entry.getKey().getNacao();
            var forecast = replacementForecast.computeIfAbsent(nation, n -> new java.util.HashSet<>());
            for (PersonagemOrdem order : entry.getValue()) {
                if (cost.applyAsInt(order, nation) > 0) forecast.add(order);
            }
        }
        for (var entry : prepared.orders().entrySet()) {
            BaseModel actor = entry.getKey();
            actor.remAcoes();
            int slot = 0;
            for (PersonagemOrdem after : entry.getValue()) {
                actor.setAcao(slot++, after);
            }
        }
        forecasts.putAll(replacementForecast);
    }
}
