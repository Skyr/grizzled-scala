/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009, Brian M. Clapper
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

   * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

   * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

   * Neither the names "clapper.org", "Grizzled Scala Library", nor the
    names of its contributors may be used to endorse or promote products
    derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
*/

package grizzled.parsing

/** `IteratorStream` places a simple stream on top of an iterator,
  * returning `Option`-wrapped instances from the underlying iterator.
  * When the stream is exhausted, the `Iterator` stream returns
  * `None`. Differences from a plain `Iterator` include:
  *
  * - An `IteratorStream` will not throw an exception if you try to read
  *   past the end of it. Instead, it will just keep returning `None`.
  *
  * Example of use with a string:
  *
  * {{{
  * import grizzled.parsing.IteratorStream
  *
  * val s = ...
  * val istream = new IteratorStream[Char](s.elements)
  * }}}
  *
  * @param iterator  the iterator to wrap
  */
class IteratorStream[T](private val iterator: Iterator[T]) {
  private var count = 0

  /** Alternate constructor that takes an `Iterable`.
    *
    * @param iterable the `Iterable`
    */
  def this(iterable: Iterable[T]) = this(iterable.iterator)

  /** Get the next item from the stream, advancing the cursor.
    *
    * @return an `Option` containing the next item, or `None`
    *         if the iterator is exhausted.
    */
  def next: Option[T] = {
    if (! iterator.hasNext)
      None

    else {
      count += 1
      Some(iterator.next)
    }
  }

  /** Get the count of the number of items consumed so far.
    *
    * @return the count
    */
  def totalRead: Int = count
}

/** The `Pushback` trait can be mixed into an `IteratorStream`
  * to permit arbitrary pushback.
  */
trait Pushback[T] extends IteratorStream[T] {
  import scala.collection.mutable.Stack

  private val pushbackStack = new Stack[T]

  /** Get the next item from the stream, advancing the cursor, while
    * honoring previous calls to `pushback()`.
    *
    * @return an `Option` containing the next item, or `None`
    *         if the iterator is exhausted.
    */
  override def next: Option[T] = {
    if (pushbackStack.isEmpty)
      super.next
    else
      Some(pushbackStack.pop)
  }

  /** Get the count of the number of items consumed so far.
    *
    * @return the count
    */
  override def totalRead: Int = (super.totalRead - pushbackStack.length)

  /** Push a single item back onto the stream.
    *
    * @param item  the item
    */
  def pushback(item: T) = pushbackStack push item

  /** Push a list of items back onto the stream. The items are pushed
    * back in reverse order, so the items in the list should be in the order
    * they were retrieved from the stream. For example:
    *
    * {{{
    * val stream = new IteratorStream[Char]("foobar") with Pushback[Char]
    * val list = List(stream.next.get, stream.next.get)
    *
    * // At this point, the list contains ('f', 'o'), and the stream 
    * // contains "obar".
    *
    * stream.pushback(list) // Stream now contains "foobar"
    * }}}
    *
    * @param items  the items to push back.
    */
  def pushback(items: List[T]) = pushbackStack.pushAll(items.reverse)
}
