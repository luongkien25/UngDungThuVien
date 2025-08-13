package org.example;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibraryGUI());
    }
}
//update dcm
//search gg
//loi:
/**
 * Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException: Cannot invoke "String.equals(Object)" because the return value of "org.example.LibraryItem.getIsbn()" is null
 * 	at org.example.Library.findItemByIsbn(Library.java:69)
 * 	at org.example.LibraryGUI.updateDocument(LibraryGUI.java:220)
 * 	at org.example.LibraryGUI.handleMenuOption(LibraryGUI.java:106)
 * 	at org.example.LibraryGUI.lambda$new$0(LibraryGUI.java:86)
 * 	at java.desktop/javax.swing.AbstractButton.fireActionPerformed(AbstractButton.java:1972)
 * 	at java.desktop/javax.swing.AbstractButton$Handler.actionPerformed(AbstractButton.java:2314)
 * 	at java.desktop/javax.swing.DefaultButtonModel.fireActionPerformed(DefaultButtonModel.java:407)
 * 	at java.desktop/javax.swing.DefaultButtonModel.setPressed(DefaultButtonModel.java:262)
 * 	at java.desktop/javax.swing.plaf.basic.BasicButtonListener.mouseReleased(BasicButtonListener.java:279)
 * 	at java.desktop/java.awt.Component.processMouseEvent(Component.java:6576)
 * 	at java.desktop/javax.swing.JComponent.processMouseEvent(JComponent.java:3404)
 * 	at java.desktop/java.awt.Component.processEvent(Component.java:6341)
 * 	at java.desktop/java.awt.Container.processEvent(Container.java:2260)
 * 	at java.desktop/java.awt.Component.dispatchEventImpl(Component.java:4958)
 * 	at java.desktop/java.awt.Container.dispatchEventImpl(Container.java:2318)
 * 	at java.desktop/java.awt.Component.dispatchEvent(Component.java:4790)
 * 	at java.desktop/java.awt.LightweightDispatcher.retargetMouseEvent(Container.java:4916)
 * 	at java.desktop/java.awt.LightweightDispatcher.processMouseEvent(Container.java:4559)
 * 	at java.desktop/java.awt.LightweightDispatcher.dispatchEvent(Container.java:4500)
 * 	at java.desktop/java.awt.Container.dispatchEventImpl(Container.java:2304)
 * 	at java.desktop/java.awt.Window.dispatchEventImpl(Window.java:2671)
 * 	at java.desktop/java.awt.Component.dispatchEvent(Component.java:4790)
 * 	at java.desktop/java.awt.EventQueue.dispatchEventImpl(EventQueue.java:725)
 * 	at java.desktop/java.awt.EventQueue.dispatchEvent(EventQueue.java:702)
 * 	at java.desktop/java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:203)
 * 	at java.desktop/java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:124)
 * 	at java.desktop/java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:113)
 * 	at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:109)
 * 	at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:101)
 * 	at java.desktop/java.awt.EventDispatchThread.run(EventDispatchThread.java:90)
 */
//Them database sách
//search hieu suat hoi cham
//display hieu suat hoi cham
//remove???