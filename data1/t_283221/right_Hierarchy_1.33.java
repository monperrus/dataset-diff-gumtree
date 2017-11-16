/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 1.1, a copy of which has been included with this
 * distribution in the LICENSE.txt file.  */

// WARNING This class MUST not have references to the Category or
// WARNING RootCategory classes in its static initiliazation neither 
// WARNING directly nor indirectly.

// Contributors:
//                Luke Blanshard <luke@quiq.com>
//                Mario Schomburg - IBM Global Services/Germany
//                Anders Kristensen
//                Igor Poteryaev
 
package org.apache.log4j;


import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.spi.RootCategory;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RendererSupport;
import org.apache.log4j.Appender;
import org.apache.log4j.or.RendererMap;
import org.apache.log4j.or.ObjectRenderer;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;

/**
   This class is specialized in retrieving loggers by name and also
   maintaining the logger hierarchy.

   <p><em>The casual user does not have to deal with this class
   directly.</em>

   <p>The structure of the logger hierarchy is maintained by the
   {@link #getLogger} method. The hierarchy is such that children link
   to their parent but parents do not have any pointers to their
   children. Moreover, loggers can be instantiated in any order, in
   particular descendant before ancestor.

   <p>In case a descendant is created before a particular ancestor,
   then it creates a provision node for the ancestor and adds itself
   to the provision node. Other descendants of the same ancestor add
   themselves to the previously created provision node.

   @author Ceki G&uuml;lc&uuml; 

*/
public class Hierarchy implements LoggerRepository, RendererSupport {

  private LoggerFactory defaultFactory;
  private Vector listeners;

  Hashtable ht;
  Logger root;
  RendererMap rendererMap;

  int enableInt;
  Level enable;

  boolean emittedNoAppenderWarning = false;
  boolean emittedNoResourceBundleWarning = false;  

  /**
     Create a new logger hierarchy.

     @param root The root of the new hierarchy.

   */
  public
  Hierarchy(Logger root) {
    ht = new Hashtable();
    listeners = new Vector(1);
    this.root = root;
    // Enable all level levels by default.
    enable(Level.ALL);
    this.root.setHierarchy(this);
    rendererMap = new RendererMap();
    defaultFactory = new DefaultCategoryFactory();
  }

  /**
     Add an object renderer for a specific class.       
   */
  public
  void addRenderer(Class classToRender, ObjectRenderer or) {
    rendererMap.put(classToRender, or);
  }
  
  public 
  void addHierarchyEventListener(HierarchyEventListener listener) {
    if(listeners.contains(listener)) {
      LogLog.warn("Ignoring attempt to add an existent listener.");
    } else {
      listeners.add(listener);
    }
  }

  /**
     This call will clear all logger definitions from the internal
     hashtable. Invoking this method will irrevocably mess up the
     logger hierarchy.
     
     <p>You should <em>really</em> know what you are doing before
     invoking this method.

     @since 0.9.0 */
  public
  void clear() {
    //System.out.println("\n\nAbout to clear internal hash table.");
    ht.clear();
  }

  public 
  void emitNoAppenderWarning(Category cat) {
    // No appenders in hierarchy, warn user only once.
    if(!this.emittedNoAppenderWarning) {
      LogLog.error("No appenders could be found for logger (" +
		   cat.getName() + ").");
      LogLog.error("Please initialize the log4j system properly.");
      this.emittedNoAppenderWarning = true;
    }
  }

  /**
     Check if the named logger exists in the hierarchy. If so return
     its reference, otherwise returns <code>null</code>.
     
     @param name The name of the logger to search for.
     
  */
  public
  Logger exists(String name) {    
    Object o = ht.get(new CategoryKey(name));
    if(o instanceof Logger) {
      return (Logger) o;
    } else {
      return null;
    }
  }


  /**
     Similar to {@link #disable(Level)} except that the level
     argument is given as a String.  

     @deprecated Replaced with the {@link #enable(Level)} familiy
     of methods
  */
  public
  void disable(String levelStr) {
    Level p = Level.toLevel(levelStr, null);
    if(p != null) {
      disable(p);
    } else {
      LogLog.warn("Could not convert ["+levelStr+"] to Level.");
    }
  }


  /**
     Disable all logging requests of level <em>equal to or below</em>
     the level parameter <code>p</code>, for <em>all</em> loggers in
     this hierarchy. Logging requests of higher level then
     <code>p</code> remain unaffected.

     <p>The "disable" family of methods are there for speed. They
     allow printing methods such as debug, info, etc. to return
     immediately after an integer comparison without walking the
     logger hierarchy. In most modern computers an integer comparison
     is measured in nanoseconds where as a logger walk is measured in
     units of microseconds.

     <p>Other configurators define alternate ways of overriding the
     disable override flag. See {@link PropertyConfigurator} and
     {@link org.apache.log4j.xml.DOMConfigurator}.

     @deprecated Please use the {@link #enable} family of methods instead.

     @since 0.8.5 */
  public
  void disable(Level p) {
    if(p != null) {    
      switch(p.level) {
      case Level.ALL_INT: enable(Level.ALL); break;      
      case Level.DEBUG_INT: enable(Level.INFO); break;      
      case Level.INFO_INT: enable(Level.WARN); break;
      case Level.WARN_INT: enable(Level.ERROR); break;      
      case Level.ERROR_INT: enable(Level.FATAL); break;      
      case Level.FATAL_INT: enable(Level.OFF); break;      
      case Level.OFF_INT: enable(Level.OFF); break;      
      default: 
      }
    }
  }
  
  /**
     Disable all logging requests regardless of logger and level.
     This method is equivalent to calling {@link #disable} with the
     argument {@link Level#FATAL}, the highest possible level.

     @deprecated Please use the {@link #enable} family of methods instead.

     @since 0.8.5 */
  public
  void disableAll() {
    disable(Level.FATAL);
  }


  /**
     Disable all logging requests of level DEBUG regardless of the
     logger.  Invoking this method is equivalent to calling {@link
     #disable} with the argument {@link Level#DEBUG}.

     @deprecated Please use the {@link #enable} family of methods instead.
  */
  public
  void disableDebug() {
    disable(Level.DEBUG);
  }


  /**
     Disable all logging requests of level INFO and below
     regardless of logger. Note that DEBUG messages are also
     disabled.  

     <p>Invoking this method is equivalent to calling {@link
     #disable(Level)} with the argument {@link Level#INFO}.

     @deprecated Please use the {@link #enable} family of methods instead.

     @since 0.8.5 */
  public
  void disableInfo() {
    disable(Level.INFO);
  }  

  /**
     Equivalent to calling <code>enable(Level.ALL)</code>.
     
     By default all levels are enabled.
  */
  public
  void enableAll() {
    enable(Level.ALL);
  }

  /**
     Enable logging for logging requests with level <code>l</code> or
     higher. By default all levels are enabled.

     @param l The minimum level for which logging requests are sent to
     their appenders.  */
  public 
  void enable(Level l) {
    if(l != null) {
      enableInt = l.level;
      enable = l;
    }
  }

  public
  void fireAddAppenderEvent(Category cat, Appender appender) {
    if(listeners != null) {
      int size = listeners.size();
      HierarchyEventListener listener;
      for(int i = 0; i < size; i++) {
	listener = (HierarchyEventListener) listeners.elementAt(i);
	listener.addAppenderEvent(cat, appender);
      }
    }        
  }

  void fireRemoveAppenderEvent(Category cat, Appender appender) {
    if(listeners != null) {
      int size = listeners.size();
      HierarchyEventListener listener;
      for(int i = 0; i < size; i++) {
	listener = (HierarchyEventListener) listeners.elementAt(i);
	listener.removeAppenderEvent(cat, appender);
      }
    }        
  }

  /**
     Returns a {@link Level} representation of the <code>enable</code>
     state.

     @since 1.2 */
  public
  Level getEnable() {
    return enable;
  }

  /**
     Returns an integer representation of the this repository's
     <code>enable</code> state.

     @since 1.2 */
  public
  int getEnableInt() {
    return enableInt;
  }


  /**
     Return a new logger instance named as the first parameter using
     the default factory. 
     
     <p>If a logger of that name already exists, then it will be
     returned.  Otherwise, a new logger will be instantiated and
     then linked with its existing ancestors as well as children.
     
     @param name The name of the logger to retrieve.

 */
  public
  Logger getLogger(String name) {
    return getLogger(name, defaultFactory);
  }

 /**
     Return a new logger instance named as the first parameter using
     <code>factory</code>.
     
     <p>If a logger of that name already exists, then it will be
     returned.  Otherwise, a new logger will be instantiated by the
     <code>factory</code> parameter and linked with its existing
     ancestors as well as children.
     
     @param name The name of the logger to retrieve.
     @param factory The factory that will make the new logger instance.

 */
  public
  Logger getLogger(String name, LoggerFactory factory) {
    //System.out.println("getInstance("+name+") called.");
    CategoryKey key = new CategoryKey(name);    
    // Synchronize to prevent write conflicts. Read conflicts (in
    // getChainedLevel method) are possible only if variable
    // assignments are non-atomic.
    Logger logger;
    
    synchronized(ht) {
      Object o = ht.get(key);
      if(o == null) {
	logger = factory.makeNewLoggerInstance(name);
	logger.setHierarchy(this);
	ht.put(key, logger);      
	updateParents(logger);
	return logger;
      } else if(o instanceof Logger) {
	return (Logger) o;
      } else if (o instanceof ProvisionNode) {
	//System.out.println("("+name+") ht.get(this) returned ProvisionNode");
	logger = factory.makeNewLoggerInstance(name);
	logger.setHierarchy(this); 
	ht.put(key, logger);
	updateChildren((ProvisionNode) o, logger);
	updateParents(logger);	
	return logger;
      }
      else {
	// It should be impossible to arrive here
	return null;  // but let's keep the compiler happy.
      }
    }
  }

  /**
     Returns all the currently defined categories in this hierarchy as
     an {@link java.util.Enumeration Enumeration}.

     <p>The root logger is <em>not</em> included in the returned
     {@link Enumeration}.  */
  public
  Enumeration getCurrentLoggers() {
    // The accumlation in v is necessary because not all elements in
    // ht are Logger objects as there might be some ProvisionNodes
    // as well.
    Vector v = new Vector(ht.size());
    
    Enumeration elems = ht.elements();
    while(elems.hasMoreElements()) {
      Object o = elems.nextElement();
      if(o instanceof Logger) {
	v.addElement(o);
      }
    }
    return v.elements();
  }

  /**
     @deprecated Please use {@link #getCurrentLoggers} instead.
   */
  public
  Enumeration getCurrentCategories() {
    return getCurrentLoggers();
  }


  /**
     Get the renderer map for this hierarchy.
  */
  public
  RendererMap getRendererMap() {
    return rendererMap;
  }


  /**
     Get the root of this hierarchy.
     
     @since 0.9.0
   */
  public
  Logger getRootLogger() {
    return root;
  }

  /**
     @deprecated See {@link #getEnable} for similar functionality.
   */

  public
  boolean isDisabled(int level) {    
    return enableInt > level;
  }

  /**
     @deprecated Deprecated with no replacement.
  */
  public
  void overrideAsNeeded(String override) {
    LogLog.warn("The Hiearchy.overrideAsNeeded method has been deprecated.");
  }

  /**
     Reset all values contained in this hierarchy instance to their
     default.  This removes all appenders from all categories, sets
     the level of all non-root categories to <code>null</code>,
     sets their additivity flag to <code>true</code> and sets the level
     of the root logger to {@link Level#DEBUG DEBUG}.  Moreover,
     message disabling is set its default "off" value.

     <p>Existing categories are not removed. They are just reset.

     <p>This method should be used sparingly and with care as it will
     block all logging until it is completed.</p>

     @since 0.8.5 */
  public
  void resetConfiguration() {

    getRootLogger().setLevel(Level.DEBUG);
    root.setResourceBundle(null);
    enableAll();
    
    // the synchronization is needed to prevent JDK 1.2.x hashtable
    // surprises
    synchronized(ht) {    
      shutdown(); // nested locks are OK    
    
      Enumeration cats = getCurrentLoggers();
      while(cats.hasMoreElements()) {
	Logger c = (Logger) cats.nextElement();
	c.setLevel(null);
	c.setAdditivity(true);
	c.setResourceBundle(null);
      }
    }
    rendererMap.clear();
  }

  /**
     Does mothing.
 
     @deprecated Deprecated with no replacement.
   */
  public
  void setDisableOverride(String override) {
    LogLog.warn("The Hiearchy.setDisableOverride method has been deprecated.");    
  }



  /**
     Used by subclasses to add a renderer to the hierarchy passed as parameter.
   */
  public
  void setRenderer(Class renderedClass, ObjectRenderer renderer) {
    rendererMap.put(renderedClass, renderer);
  }


  /**
     Shutting down a hierarchy will <em>safely</em> close and remove
     all appenders in all categories including the root logger.
     
     <p>Some appenders such as {@link org.apache.log4j.net.SocketAppender}
     and {@link AsyncAppender} need to be closed before the
     application exists. Otherwise, pending logging events might be
     lost.

     <p>The <code>shutdown</code> method is careful to close nested
     appenders before closing regular appenders. This is allows
     configurations where a regular appender is attached to a logger
     and again to a nested appender.
     

     @since 1.0 */
  public 
  void shutdown() {
    Logger root = getRootLogger();    

    // begin by closing nested appenders
    root.closeNestedAppenders();

    synchronized(ht) {
      Enumeration cats = this.getCurrentLoggers();
      while(cats.hasMoreElements()) {
	Logger c = (Logger) cats.nextElement();
	c.closeNestedAppenders();
      }

      // then, remove all appenders
      root.removeAllAppenders();
      cats = this.getCurrentLoggers();
      while(cats.hasMoreElements()) {
	Logger c = (Logger) cats.nextElement();
	c.removeAllAppenders();
      }      
    }
  }


  /**
     This method loops through all the *potential* parents of
     'cat'. There 3 possible cases:

     1) No entry for the potential parent of 'cat' exists

        We create a ProvisionNode for this potential parent and insert
        'cat' in that provision node.

     2) There entry is of type Logger for the potential parent.

        The entry is 'cat's nearest existing parent. We update cat's
        parent field with this entry. We also break from the loop
        because updating our parent's parent is our parent's
        responsibility.
	 
     3) There entry is of type ProvisionNode for this potential parent.

        We add 'cat' to the list of children for this potential parent.
   */
  final
  private
  void updateParents(Logger cat) {
    String name = cat.name;
    int length = name.length();
    boolean parentFound = false;
    
    //System.out.println("UpdateParents called for " + name);
    
    // if name = "w.x.y.z", loop thourgh "w.x.y", "w.x" and "w", but not "w.x.y.z" 
    for(int i = name.lastIndexOf('.', length-1); i >= 0; 
	                                 i = name.lastIndexOf('.', i-1))  {
      String substr = name.substring(0, i);

      //System.out.println("Updating parent : " + substr);
      CategoryKey key = new CategoryKey(substr); // simple constructor
      Object o = ht.get(key);
      // Create a provision node for a future parent.
      if(o == null) {
	//System.out.println("No parent "+substr+" found. Creating ProvisionNode.");
	ProvisionNode pn = new ProvisionNode(cat);
	ht.put(key, pn);
      } else if(o instanceof Category) {
	parentFound = true;
	cat.parent = (Category) o;
	//System.out.println("Linking " + cat.name + " -> " + ((Category) o).name);
	break; // no need to update the ancestors of the closest ancestor
      } else if(o instanceof ProvisionNode) {
	((ProvisionNode) o).addElement(cat);
      } else {
	Exception e = new IllegalStateException("unexpected object type " + 
					o.getClass() + " in ht.");
	e.printStackTrace();			   
      }
    }
    // If we could not find any existing parents, then link with root.
    if(!parentFound) 
      cat.parent = root;
  }

  /** 
      We update the links for all the children that placed themselves
      in the provision node 'pn'. The second argument 'cat' is a
      reference for the newly created Logger, parent of all the
      children in 'pn'

      We loop on all the children 'c' in 'pn':

         If the child 'c' has been already linked to a child of
         'cat' then there is no need to update 'c'.

	 Otherwise, we set cat's parent field to c's parent and set
	 c's parent field to cat.

  */
  final
  private
  void updateChildren(ProvisionNode pn, Logger logger) {
    //System.out.println("updateChildren called for " + logger.name);
    final int last = pn.size();

    for(int i = 0; i < last; i++) {
      Logger l = (Logger) pn.elementAt(i);
      //System.out.println("Updating child " +p.name);

      // Unless this child already points to a correct (lower) parent,
      // make cat.parent point to l.parent and l.parent to cat.
      if(!l.parent.name.startsWith(logger.name)) {
	logger.parent = l.parent;
	l.parent = logger;      
      }
    }
  }    

}

