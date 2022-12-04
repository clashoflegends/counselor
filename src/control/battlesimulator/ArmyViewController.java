/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control.battlesimulator;


import business.ImageManager;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.converter.NumberStringConverter;
import gui.accessories.battlesimulator.model.ArmySim;
import gui.accessories.battlesimulator.model.PlatoonSim;
import gui.accessories.battlesimulator.model.TroopTypeSim;
import java.net.URISyntaxException;
import javafx.scene.image.Image;

/**
 * FXML Controller class
 *
 * @author serguei
 */
public class ArmyViewController implements Initializable {
    
    private final ArmySim armySim;

    @FXML
    private Label commanderName;
    
    @FXML
    private Spinner<Integer> moral;
    
    @FXML
    private Spinner<Integer> commanderRank;
    
    @FXML
    private GridPane platoonTable;
    
    @FXML
    private ComboBox addPlatoon;
    
    @FXML
    private ImageView nationFlag;
    
    public ArmyViewController() {
        armySim = new ArmySim();
    }
    
    public ArmyViewController(ArmySim army) {
        this.armySim = army;
    }
    
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        //TODO
        nationFlag.setImage(armySim.getNationFlag());
        commanderName.setText(armySim.getCommanderName());
        commanderRank.getValueFactory().valueProperty().set(armySim.getCommanderRankProperty().getValue());     
        armySim.getCommanderRankProperty().bind(commanderRank.getValueFactory().valueProperty());
        moral.getValueFactory().valueProperty().set(armySim.getMoral().getValue());
        armySim.getMoral().bind(moral.getValueFactory().valueProperty());
        try {
            initializePlatoons(armySim.getPlatoonsSim());
            initializeAddPlatoonCombo(armySim.getAvalaibleTroopTypes());
        } catch (IOException ex) {
            Logger.getLogger(ArmyViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
   
       
    }    

    private void initializePlatoons(Set<PlatoonSim> platoons) throws IOException {
        
        Consumer<PlatoonSim> convertPlatoon = new Consumer<PlatoonSim>() {
            @Override
            public void accept(PlatoonSim p) {
                addPlatoon(p);
            }
        };
        

        
        platoons.stream().forEach(convertPlatoon);
           
    }
    
    
   
    
    private Node[] getNewPlatoon() throws IOException {
        GridPane gridPane = (GridPane)FXMLLoader.load(getClass().getResource("/gui/accessories/battlesimulator/views/PlatoonView.fxml"));
        return gridPane.getChildren().toArray(new Node[6]);
    }
    
    private Node[] getNewPlatoon(PlatoonSim platoon) {
        Node[] platoonsFx = null;
        try {
            platoonsFx = getNewPlatoon();
        } catch (IOException ex) {
            Logger.getLogger(ArmyViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return platoonsFx;
    }

    private void initializeAddPlatoonCombo(ObservableList<TroopTypeSim> troopTypes) {        
        addPlatoon.setItems(troopTypes);        
        addPlatoon.getSelectionModel().selectedItemProperty().addListener((option, oldValue, newValue) -> {
            Optional.ofNullable(newValue).ifPresent(v -> {
  //            System.out.println(option);
                PlatoonSim newPlatoon = new PlatoonSim();
                newPlatoon.getTroopTypeProperty().setValue((TroopTypeSim)v);
            
                armySim.addPlatoon(newPlatoon);
         
                Platform.runLater(() -> {           
                    addPlatoon.getSelectionModel().clearSelection();                
                    addPlatoon(newPlatoon);                
                });
            });           
        });     
        addPlatoon.setButtonCell(new ListCell<TroopTypeSim>() {
            @Override
            protected void updateItem(TroopTypeSim item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(addPlatoon.getPromptText());                        
                } else {
                    setText(item.getName());
                }
            }            
        });                    
    }
    
    private void addPlatoon(PlatoonSim p) {
        Node[] platoonFx = getNewPlatoon(p);
        
        Label troopTypeLabel = (Label)platoonFx[0];
        troopTypeLabel.setText(p.getTroopTypeProperty().getValue().getName());
    /*    ComboBox troopCombo = (ComboBox) platoonFx[0];
        ObservableList<TroopTypeSim> troopTypeList = FXCollections.observableArrayList(armySim.getNation().getTroopTypes());
        troopCombo.setItems(troopTypeList);
        troopCombo.valueProperty().bindBidirectional(p.getTroopType());
    */    Spinner trainningSp = (Spinner) platoonFx[1];
        trainningSp.getValueFactory().valueProperty().set(p.getTrainningProperty().getValue());
        p.getTrainningProperty().bind(trainningSp.getValueFactory().valueProperty());
        Spinner weaponSp = (Spinner) platoonFx[2];
        weaponSp.getValueFactory().valueProperty().set(p.getWeaponProperty().getValue());
        p.getWeaponProperty().bind(weaponSp.getValueFactory().valueProperty());
        Spinner armorSp = (Spinner) platoonFx[3];
        armorSp.getValueFactory().valueProperty().set(p.getArmorProperty().getValue());
        p.getArmorProperty().bind(armorSp.getValueFactory().valueProperty());

        TextField troopsQt = (TextField) platoonFx[4];

        troopsQt.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                troopsQt.selectAll();
            }
        });
        NumberStringConverter converter = new NumberStringConverter();
        troopsQt.setTextFormatter(new TextFormatter<>(converter));

        troopsQt.textProperty().bindBidirectional(p.getTroopsProperty(), converter);
              
        Label troopsLeft = (Label) platoonFx[5];
        troopsLeft.setText(p.getTroopsLeft().getValue().toString());
        troopsLeft.textProperty().bindBidirectional(p.getTroopsLeft(), converter);
        
        Button delButton = (Button) platoonFx[6];
        Image deleteIcon;
        try {
            deleteIcon = ImageManager.getInstance().getDeletePlatoonIcon();            
            ImageView view = new ImageView(deleteIcon);
            view.setFitHeight(16);
            view.setFitWidth(16);
            delButton.setGraphic(view);
        } catch (URISyntaxException ex) {
            Logger.getLogger(ArmyViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        delButton.setOnAction((ActionEvent arg0) -> {
            
            Button source = (Button)arg0.getSource();
            int row = GridPane.getRowIndex(source);
            List<Node> removableNodes = platoonTable.getChildren().stream().filter(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) == row).collect(Collectors.toList());
            platoonTable.getChildren().removeAll(removableNodes);
            armySim.getPlatoonsSim().removeIf(platoon -> platoon.getTroopTypeProperty().getValue().getName().equals(((Label)removableNodes.get(0)).getText()));
            Platform.runLater(() -> armySim.getAvalaibleTroopTypes().add(p.getTroopTypeProperty().getValue()));
        });

        int lastRowIndex = getRowCount(platoonTable);
        platoonTable.addRow(lastRowIndex, platoonFx);
        armySim.getAvalaibleTroopTypes().remove(p.getTroopTypeProperty().getValue());
    }
    
    public final int getRowCount(GridPane gridPane) {
        int nRows = gridPane.getRowConstraints().size();
        for (int i = 0; i < gridPane.getChildren().size(); i++) {
            Node child = gridPane.getChildren().get(i);
            if (child.isManaged()) {
                Integer nodeRowIndex = GridPane.getRowIndex(child);
                int rowIndex = nodeRowIndex != null ? nodeRowIndex : 0 ;
                Integer nodeRowSpan = GridPane.getRowSpan(child);
                int rowSpan = nodeRowSpan != null ? nodeRowSpan : 1;
               
                int rowEnd =  rowSpan != GridPane.REMAINING ? rowIndex + rowSpan - 1 : GridPane.REMAINING;
                nRows = Math.max(nRows, (rowEnd != GridPane.REMAINING ? rowEnd : rowIndex) + 1);
            }
        }
        return nRows;
    }
}
