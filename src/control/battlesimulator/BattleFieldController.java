/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control.battlesimulator;

import business.ImageManager;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import gui.accessories.battlesimulator.model.ArmySim;
import gui.accessories.battlesimulator.model.BattleFieldSim;
import gui.accessories.battlesimulator.model.NationSim;
import gui.accessories.battlesimulator.model.PlatoonSim;
import gui.accessories.battlesimulator.model.TroopTypeSim;
import gui.accessories.battlesimulator.model.engine.BasicCombatEngine;
import gui.accessories.battlesimulator.model.engine.ICombatEngine;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import model.Local;
import model.Nacao;
import model.Personagem;
import model.Terreno;
import model.TipoTropa;



/**
 * FXML Controller class
 *
 * @author serguei
 */
public class BattleFieldController implements Initializable {
    
    
    private BattleFieldSim battleField;

    @FXML
    private Button addArmyButton;
    
    @FXML
    private VBox armyBox;
    
    @FXML
    private MenuItem combatMenuItem;
    
    private Local local;


    public MenuItem getCombatMenuItem() {
        return combatMenuItem;
    }
    

    public VBox getArmyBox() {
        return armyBox;
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        battleField = new BattleFieldSim();
        
        combatMenuItem.setOnAction((e) -> {            
            ICombatEngine engine = new BasicCombatEngine(battleField);
            engine.combat();
        
        });    
      
    }    
    
    public void addArmy(Event event) throws IOException {
        
        if (event != null) {
            String sourceId = ((Button)event.getSource()).getId();
            System.out.println("Button: " + sourceId);
        }
          
        //TODO 
 
      //  addArmySim(newArmy);
        

    }
    
    public void addArmySim(ArmySim army) throws IOException {
        battleField.getArmyList().add(army);
           
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/accessories/battlesimulator/views/ArmyView.fxml"));
        loader.setControllerFactory(new Callback<Class<?>, Object>() {
            @Override
            public Object call(Class<?> aClass) {
                return new ArmyViewController(army);
            }
        });    
                
        VBox armyPane = loader.load();
        ArmyViewController cont = loader.getController();
        cont.getClass();
        armyBox.getChildren().add(armyPane);      
    }
    
    public BattleFieldSim getBattleField() {
        return battleField;
    }

    
    public void setLocal(Local local) {
        this.local = local;
        configUI();
    }
    
    
    // This can by placed in a utility class
    private void configUI() {
        Terreno terreno =  local.getTerreno();
        battleField.setHexType(terreno.getCodigo());
        
        local.getExercitos().values().stream().forEach(army -> {            
            try {
                ArmySim newArmy = new ArmySim();
                Personagem comandante = army.getComandante() != null ? army.getComandante() : new Personagem();
                newArmy.setCommanderName(comandante.getNome());
                newArmy.setCommanderRank(new SimpleIntegerProperty(comandante.getPericiaComandante()));
                newArmy.setMoral(new SimpleIntegerProperty(army.getMoral()));
                
                NationSim armyNation = new NationSim();
                Nacao nacao = army.getNacao();
                
                armyNation.setName(nacao.getNome());
                armyNation.setTroopTypes(
                nacao.getRaca().getTropas().keySet().stream().map(this::convertTroop).collect(Collectors.toSet())             );
                                        
                java.awt.Image image = ImageManager.getInstance().getExercito(army);  
                BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics2D bGr = bufferedImage.createGraphics();
                bGr.drawImage(image, 0, 0, null);
                bGr.dispose();
                               
                Image flag = SwingFXUtils.toFXImage(bufferedImage, null);
                Logger.getLogger(BattleFieldController.class.getName()).log(Level.FINEST, "Loading flag army: {0}.", !flag.isError());
                
                armyNation.setFlag(flag);
                newArmy.setNation(armyNation);
                army.getPelotoes().values().stream().map(pelotao -> {
                    PlatoonSim newPlatoon = new PlatoonSim();
                    newPlatoon.getTroopTypeProperty().setValue(convertTroop(pelotao.getTipoTropa()));
                    ObjectProperty<Integer> objectProp = new SimpleObjectProperty<>(pelotao.getTreino());
                    newPlatoon.setTrainning(IntegerProperty.integerProperty(objectProp));
                    newPlatoon.setWeapon(new SimpleIntegerProperty(pelotao.getModAtaque()));
                    newPlatoon.setArmor(new SimpleIntegerProperty(pelotao.getModDefesa()));
                    newPlatoon.setTroops(pelotao.getQtd());
                    return newPlatoon;
                    
                }).forEach( p -> {
                    newArmy.addPlatoon(p);
                    newArmy.getAvalaibleTroopTypes().remove(p.getTroopTypeProperty().getValue());                    
                });
               
                
                addArmySim(newArmy);
            } catch (IOException ex) {
                Logger.getLogger(BattleFieldController.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        });
        
    }
    private TroopTypeSim convertTroop(TipoTropa tipoTropa) {
        TroopTypeSim newTroop = new TroopTypeSim();
        newTroop.setName(tipoTropa.getNome());
        newTroop.setCode(tipoTropa.getCodigo());
        return newTroop;        
    }
    
}
