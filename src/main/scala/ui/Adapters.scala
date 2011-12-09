package org.positronicnet.ui

import _root_.android.content.Context
import _root_.android.view.LayoutInflater
import _root_.android.view.View
import _root_.android.view.ViewGroup
import _root_.android.util.Log

import org.positronicnet.notifications.Notifier

import org.positronicnet.content.PositronicCursor // for CursorSourceAdapter
import _root_.android.database.Cursor

import scala.collection.mutable.ArrayBuffer

/** Adapter for cursors produced by PositronicDb queries.
  * Automatically handles a fair amount of the usual typecasting
  * gubbish...
  */
abstract class CursorSourceAdapter[T <: AnyRef]( 
  activity: PositronicActivityHelpers,
  converter: PositronicCursor => T,
  source: Notifier[PositronicCursor] = null,
  itemViewResourceId: Int = 0
)
 extends _root_.android.widget.CursorAdapter( activity, null )
{
  var inflater: LayoutInflater = null

  if (source != null) {
    activity.manageListener( this, source ) {
      this.changeCursor( _ )
    }
  }

  def newView( context: Context, 
               cursor: android.database.Cursor,
               parent: ViewGroup ): View =
  {
    if (itemViewResourceId == 0)
      throw new RuntimeException( "QueryAdapter with itemViewResourceId unset"+
                                  " and newView not overridden" )
    if (inflater == null) {
      inflater = 
        parent.getContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
          .asInstanceOf[LayoutInflater]
    }

    return inflater.inflate( itemViewResourceId, parent, false )
  }
                 
  override def bindView( view: View, context: Context, cursor: Cursor ) = {
    val item = converter( cursor.asInstanceOf[ PositronicCursor ] )
    bindItem( view, item )
  }

  def bindItem( view: View, item: T )

  override def getItem( posn: Int ): T = {
    val baseValue = super.getItem( posn )
    if (baseValue == null)
      return null.asInstanceOf[T]
    else
      return converter( baseValue.asInstanceOf[ PositronicCursor ])
  }
}

/**
  * Adapter for Scala `IndexedSeq`s.
  *
  * Supports `newView` and `bindView` methods, analogous to those
  * provided by the base framework's `CursorAdapter` (though
  * `newView` takes only the parent `ViewGroup` as an argument).
  *
  * Note that the `T <: Object` restriction is needed so that
  * our `getItem( _: Int ):T` is compatible with the declared
  * `getItem( _: Int ): java.lang.Object` in the Adapter interface.
  * So, if you really want an adapter for an `IndexedSeq[Long]`,
  * you're on your own.
  *
  * The `itemViewResourceId` and `itemTextResourceId` constructor
  * arguments are optional, but are used by the default implementations
  * of `newView` and `bindView`, q.v., to handle simple cases with a minimum
  * of extra code.
  */

class IndexedSeqAdapter[T <: Object](protected var seq:IndexedSeq[T] = new ArrayBuffer[T],
                                     itemViewResourceId: Int = 0, 
                                     binder: UiBinder = UiBinder
                                    ) 
  extends _root_.android.widget.BaseAdapter 
{
  protected var inflater: LayoutInflater = null

  /** Method to reset the sequence if a new copy was (or might have been)
    * loaded off the UI thread.
    */

  def resetSeq( newSeq: IndexedSeq[T] ) = {
    seq = newSeq
    notifyDataSetChanged
  }

  /** Get a view to use for the given position.  Ordinarily delegates to the
    * `newView` and `bindView` methods, q.v.
    */

  def getView( position: Int, convertView: View, parent: ViewGroup ):View = {

    val view = 
      if (convertView != null) {
        convertView
      }
      else {
        if (inflater == null) {
          inflater = 
            parent.getContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
             .asInstanceOf[LayoutInflater]
        }
        newView( parent )
      }

    bindView( view, getItem( position ))
    return view
  }

  /** Create a new view to display items (if our `AdapterView`'s pool has
    * no spares).
    *
    * If it's not overridden, and if an `itemViewResourceId` was supplied
    * to the constructor, the default implementation will use a layout
    * inflater to inflate that resource, and return the result.
    */

  def newView( parent: ViewGroup ): View = {
    assert( itemViewResourceId != 0 )
    inflater.inflate( itemViewResourceId, parent, false )
  }

  /** Make one of the views resulting from `newView` display a particular
    * `item`.  The default implementation uses the
    * [[org.positronicnet.ui.UiBinder]] object passed in as a constructor
    * argument, for which the default is the [[org.positronicnet.ui.UiBinder]]
    * singleton.
    *
    * If the `view` is a `TextView`, and no other arrangements have been
    * made, this will effectively do `view.setText(item.toString)`.  See
    * the documentation on [[org.positronicnet.ui.UiBinder]] for how to
    * easily make it do something smarter (e.g., loading views with the
    * values of properties named by their resource IDs).
    */

  def bindView( view: View, item: T ) = binder.show( item, view )

  /** Get the n'th item from the current sequence */

  def getItem(position: Int):T = seq(position)

  /** Get the id of the n'th item from the current sequence */

  def getItemId(position: Int) = getItem(position).hashCode()

  /** Get number of items in the current sequence */

  def getCount = seq.size
}

/**
  * Adapter for [[org.positronicnet.notifications.Notifier]]s which
  * manage (and report changes to) Scala `IndexedSeq`s.
  *
  * Like [[org.positronicnet.ui.IndexedSeqAdapter]], except that it wires
  * itself up to automatically be notified of changes within the lifetime
  * of the given `activity`.
  */

class IndexedSeqSourceAdapter[T <: Object](activity: PositronicActivityHelpers,
                                           source: Notifier[IndexedSeq[T]],
                                           itemViewResourceId: Int = 0, 
                                           binder: UiBinder = UiBinder) 
  extends IndexedSeqAdapter[T]( itemViewResourceId = itemViewResourceId,
                                binder = binder )
{
  activity.manageListener( this, source ) { resetSeq( _ ) }
}
