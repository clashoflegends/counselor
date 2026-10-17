package e2e;

import baseLib.BaseModel;
import control.services.TurnReview;
import gui.MainDadosGui;
import gui.services.TurnReviewDialog;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import org.junit.jupiter.api.Test;
import persistenceCommons.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class RecruitmentReviewWindowTest {
    static Object field(Object owner,String name) throws Exception {
        Field f=owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner);
    }
    @Test void selectingSortingAndFilteringKeepsWrappedDetailsOnTheCorrectFinding() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            TurnReviewDialog dialog=null;
            try {
                var f=new RecruitmentReviewTest.Fixture(); f.recruit(200,"no","no"); f.recruit(200,"no","no");
                List<TurnReview.Finding> rows=new ArrayList<>(f.run());
                var findingCtor=TurnReview.Finding.class.getDeclaredConstructor(TurnReview.Level.class,
                        TurnReview.Kind.class,BaseModel.class,int.class,String[].class);
                findingCtor.setAccessible(true);
                rows.add(findingCtor.newInstance(TurnReview.Level.INFO,TurnReview.Kind.OPEN_SLOT,
                        f.actors.get(0),-1,new String[]{"Open actor"}));
                var ctor=TurnReviewDialog.class.getDeclaredConstructor(Window.class,MainDadosGui.class,
                        BundleManager.class,List.class,int.class,int.class,boolean.class);
                ctor.setAccessible(true);
                dialog=ctor.newInstance(null,null,SettingsManager.getInstance().getBundleManager(),rows,2,2,false);
                JTable table=(JTable)field(dialog,"table"); JTextArea detail=(JTextArea)field(dialog,"detail");
                Method filter=TurnReviewDialog.class.getDeclaredMethod("applyFilter",boolean.class); filter.setAccessible(true);
                filter.invoke(dialog,true); ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                table.getRowSorter().toggleSortOrder(3);
                int view=table.convertRowIndexToView(0); table.setRowSelectionInterval(view,view);
                assertEquals(table.getValueAt(view,3),detail.getText());
                assertTrue(detail.getLineWrap()); assertTrue(detail.getWrapStyleWord());
                int openView=table.convertRowIndexToView(rows.size()-1); table.setRowSelectionInterval(openView,openView);
                assertTrue(detail.getText().contains("Open actor"));
                filter.invoke(dialog,false); ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                assertEquals("",detail.getText(),"hidden findings must not leave stale selected details");
                assertNotNull(table.getActionMap().get("review-jump"));
                assertTrue(dialog.getWidth()<1920); assertTrue(dialog.getHeight()<1040);
            } catch(Exception ex) { throw new RuntimeException(ex); }
            finally { if(dialog!=null) dialog.dispose(); }
        });
    }
}
