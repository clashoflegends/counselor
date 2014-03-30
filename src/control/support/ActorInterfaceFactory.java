/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package control.support;

import java.io.Serializable;
import model.Cidade;
import model.Nacao;
import model.Personagem;

/**
 *
 * @author jmoura
 */
public class ActorInterfaceFactory implements Serializable {

    private static ActorInterfaceFactory instance;

    public synchronized static ActorInterfaceFactory getInstance() {
        if (ActorInterfaceFactory.instance == null) {
            ActorInterfaceFactory.instance = new ActorInterfaceFactory();
        }
        return ActorInterfaceFactory.instance;
    }

    public ActorInterface getActorInterface(Personagem personagem) {
        ActorInterfacePersonagem ordPers = new ActorInterfacePersonagem();
        ordPers.setPersonagem(personagem);
        return ordPers;
    }

    public ActorInterface getActorInterface(Cidade cidade) {
        ActorInterfaceCidade ordPers = new ActorInterfaceCidade();
        ordPers.setCidade(cidade);
        return ordPers;
    }

    public ActorInterface getActorInterface(Nacao nacao) {
        ActorInterfaceNacao ordPers = new ActorInterfaceNacao();
        ordPers.setNacao(nacao);
        return ordPers;
    }
}
